package com.medion.hardwarestore.domain.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, UUID> {
    List<Withdrawal> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
