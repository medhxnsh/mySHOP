package com.myshop.repository.jpa;

import com.myshop.model.entity.FlashSaleReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FlashSaleReservationRepository extends JpaRepository<FlashSaleReservation, UUID> {

    long countBySaleIdAndStatus(UUID saleId, String status);

    boolean existsBySaleIdAndUserId(UUID saleId, UUID userId);
}
