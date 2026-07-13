package com.myshop.repository.jpa;

import com.myshop.model.entity.FlashSale;
import com.myshop.model.enums.FlashSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {

    Optional<FlashSale> findFirstByStatusOrderByStartsAtAsc(FlashSaleStatus status);
}
