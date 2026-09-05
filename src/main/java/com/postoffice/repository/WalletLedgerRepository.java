package com.postoffice.repository;

import com.postoffice.model.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {
    List<WalletLedger> findByUserIdOrderByCreatedAtDesc(Long userId);
}
