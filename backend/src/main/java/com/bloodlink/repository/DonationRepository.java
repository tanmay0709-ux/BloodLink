package com.bloodlink.repository;

import com.bloodlink.model.Donation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findAllByBloodBankId(Long bloodBankId);

    List<Donation> findAllByDonorIdOrderByDonationDateDesc(Long donorId);

    List<Donation> findAllByDonorIdAndBloodBankIdOrderByDonationDateDesc(Long donorId, Long bloodBankId);

    Optional<Donation> findTopByDonorIdAndStatusOrderByDonationDateDesc(Long donorId, Donation.DonationStatus status);

    @Query("SELECT COUNT(d) FROM Donation d WHERE (:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId)")
    long countTotalDonations(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT COUNT(d), SUM(d.quantity) FROM Donation d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND d.status = 'COMPLETED'")
    List<Object[]> getCompletedDonationTotals(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT d.bloodType, COUNT(d), SUM(d.quantity) FROM Donation d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND d.status = 'COMPLETED' " +
           "GROUP BY d.bloodType")
    List<Object[]> getCompletedDonationsByBloodType(@Param("bloodBankId") Long bloodBankId);

    @Query("SELECT d FROM Donation d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) " +
           "ORDER BY d.donationDate DESC, d.createdAt DESC")
    List<Donation> findRecentDonations(@Param("bloodBankId") Long bloodBankId, Pageable pageable);

    @Query("SELECT d.donationDate, d.quantity FROM Donation d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND " +
           "d.status = 'COMPLETED' AND d.donationDate >= :startDate")
    List<Object[]> getCompletedDonationsSince(@Param("bloodBankId") Long bloodBankId, @Param("startDate") LocalDate startDate);
}
