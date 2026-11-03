package com.bloodlink.repository;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.BloodUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodUnitRepository extends JpaRepository<BloodUnit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BloodUnit b WHERE b.id IN :ids")
    List<BloodUnit> findAllByIdWithLock(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BloodUnit b WHERE b.id = :id")
    Optional<BloodUnit> findByIdWithLock(@Param("id") Long id);

    boolean existsByUnitId(String unitId);

    boolean existsByUnitIdAndIdNot(String unitId, Long id);

    Optional<BloodUnit> findByUnitId(String unitId);

    List<BloodUnit> findAllByBloodBankId(Long bloodBankId);

    List<BloodUnit> findAllByDonationId(Long donationId);

    @Query("SELECT b FROM BloodUnit b WHERE b.expiryDate < :today AND b.status IN ('AVAILABLE', 'RESERVED', 'QUARANTINED')")
    List<BloodUnit> findExpiredUnits(@Param("today") LocalDate today);

    @Query("SELECT b FROM BloodUnit b WHERE " +
           "(:bloodBankId IS NULL OR b.bloodBank.id = :bloodBankId) AND " +
           "(:bloodType IS NULL OR b.bloodType = :bloodType) AND " +
           "(:componentType IS NULL OR b.componentType = :componentType) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:query IS NULL OR LOWER(b.unitId) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<BloodUnit> searchUnits(@Param("bloodBankId") Long bloodBankId,
                                @Param("bloodType") BloodInventory.BloodType bloodType,
                                @Param("componentType") BloodUnit.BloodComponentType componentType,
                                @Param("status") BloodUnit.BloodUnitStatus status,
                                @Param("query") String query);

    @Query("SELECT b.bloodType, b.status, COUNT(b), SUM(b.quantity) FROM BloodUnit b WHERE " +
           "(:bloodBankId IS NULL OR b.bloodBank.id = :bloodBankId) " +
           "GROUP BY b.bloodType, b.status")
    List<Object[]> getStockSummaryByBloodTypeAndStatus(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT b.status, COUNT(b) FROM BloodUnit b WHERE " +
           "(:bloodBankId IS NULL OR b.bloodBank.id = :bloodBankId) " +
           "GROUP BY b.status")
    List<Object[]> countUnitsByStatus(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT b.bloodType, COUNT(b), SUM(b.quantity) FROM BloodUnit b WHERE " +
           "(:bloodBankId IS NULL OR b.bloodBank.id = :bloodBankId) AND b.status = 'AVAILABLE' " +
           "GROUP BY b.bloodType")
    List<Object[]> countAvailableUnitsByBloodType(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT b.componentType, COUNT(b), SUM(b.quantity) FROM BloodUnit b WHERE " +
           "(:bloodBankId IS NULL OR b.bloodBank.id = :bloodBankId) AND b.status = 'AVAILABLE' " +
           "GROUP BY b.componentType")
    List<Object[]> countAvailableUnitsByComponentType(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT b FROM BloodUnit b WHERE " +
           "(:bloodBankId IS NULL OR b.bloodBank.id = :bloodBankId) AND " +
           "b.status = 'AVAILABLE' AND b.expiryDate BETWEEN :today AND :thresholdDate " +
           "ORDER BY b.expiryDate ASC")
    List<BloodUnit> findUnitsNearExpiry(@Param("bloodBankId") Long bloodBankId,
                                        @Param("today") LocalDate today,
                                        @Param("thresholdDate") LocalDate thresholdDate);
}
