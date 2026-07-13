package com.myshop.service;

import com.myshop.config.MetricsConfig;
import com.myshop.constants.CacheKeys;
import com.myshop.model.entity.FlashSale;
import com.myshop.model.enums.FlashSaleStatus;
import com.myshop.repository.jpa.FlashSaleRepository;
import com.myshop.repository.jpa.FlashSaleReservationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FlashSaleReconciliationJob — detects drift between the two sides of the
 * flash-sale design (Phase 9).
 *
 * Redis (buyers set) is the source of truth for WHO bought; Postgres
 * (reservations) is the durable record. They converge through Kafka, so a
 * small in-flight gap is normal. A PERSISTENT gap means reservations were
 * lost in the crash window between Lua-accept and Kafka-publish — that is
 * the documented weak spot of skipping the outbox on the hot path, and this
 * job is the detector for it: alert (ERROR + metric), don't auto-repair.
 * Repair is a human decision: refund the slot (compensate script) or
 * recreate the reservation from the buyers set.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "myshop.flash.reconciliation-enabled", havingValue = "true", matchIfMissing = true)
public class FlashSaleReconciliationJob {

    /** Positive drift must persist unchanged this many ticks before alarming. */
    private static final int STUCK_TICKS_THRESHOLD = 4;

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private final java.util.Map<java.util.UUID, long[]> driftHistory = new java.util.concurrent.ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${myshop.flash.reconciliation-interval-ms:30000}")
    public void reconcile() {
        List<FlashSale> activeSales = flashSaleRepository.findAll().stream()
                .filter(s -> s.getStatus() == FlashSaleStatus.ACTIVE)
                .toList();

        for (FlashSale sale : activeSales) {
            Long buyers = redisTemplate.opsForSet()
                    .size(CacheKeys.format(CacheKeys.FLASH_BUYERS, sale.getId()));
            long confirmed = reservationRepository.countBySaleIdAndStatus(sale.getId(), "CONFIRMED");
            long accepted = buyers == null ? 0 : buyers;
            long inFlight = accepted - confirmed;

            if (inFlight < 0) {
                // More orders than buyers should be impossible — loud alarm.
                meterRegistry.counter(MetricsConfig.FLASH_DRIFT).increment();
                log.error("Flash sale {} OVERSELL SUSPECTED: {} confirmed reservations vs {} accepted buyers",
                        sale.getId(), confirmed, accepted);
                continue;
            }

            // Positive drift is normal while the worker drains — the alarm is
            // the SAME positive value persisting across ticks (reservations
            // lost in the accept→publish crash window).
            long[] history = driftHistory.computeIfAbsent(sale.getId(), k -> new long[] { -1, 0 });
            if (inFlight > 0 && inFlight == history[0]) {
                history[1]++;
                if (history[1] >= STUCK_TICKS_THRESHOLD) {
                    meterRegistry.counter(MetricsConfig.FLASH_DRIFT).increment();
                    log.error("Flash sale {}: {} reservation(s) stuck in flight for {} checks — "
                            + "likely lost between Lua accept and Kafka publish. Repair manually: "
                            + "compensate the slot or recreate the reservation from the buyers set.",
                            sale.getId(), inFlight, history[1]);
                }
            } else {
                history[0] = inFlight;
                history[1] = 0;
            }
        }
    }
}
