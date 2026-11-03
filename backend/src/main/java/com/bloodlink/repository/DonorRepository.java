package com.bloodlink.repository;

import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Donor> findByPhone(String phone);

    List<Donor> findAllByBloodBankId(Long bloodBankId);

    @Query("SELECT d FROM Donor d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND (" +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "d.phone LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Donor> searchDonors(@Param("bloodBankId") Long bloodBankId, @Param("query") String query);

    @Query("SELECT d FROM Donor d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND " +
           "d.bloodType = :bloodType")
    List<Donor> findByBloodType(@Param("bloodBankId") Long bloodBankId, @Param("bloodType") BloodInventory.BloodType bloodType);

    @Query("SELECT d FROM Donor d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND " +
           "d.bloodType = :bloodType AND (" +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "d.phone LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Donor> searchDonorsWithBloodType(@Param("bloodBankId") Long bloodBankId,
                                         @Param("query") String query,
                                         @Param("bloodType") BloodInventory.BloodType bloodType);

    @Query("SELECT COUNT(d) FROM Donor d WHERE (:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId)")
    long countDonors(@Param("bloodBankId") Long bloodBankId);
}
