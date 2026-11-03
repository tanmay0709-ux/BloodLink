package com.bloodlink.repository;

import com.bloodlink.model.BloodDispatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodDispatchRepository extends JpaRepository<BloodDispatch, Long> {

    Optional<BloodDispatch> findByDispatchReference(String dispatchReference);

    Optional<BloodDispatch> findByRequestId(Long requestId);

    List<BloodDispatch> findAllByHospitalIdOrderByDispatchedAtDesc(Long hospitalId);

    List<BloodDispatch> findAllByBloodBankIdOrderByDispatchedAtDesc(Long bloodBankId);

    List<BloodDispatch> findAllByOrderByDispatchedAtDesc();

    @Query("SELECT d.status, COUNT(d) FROM BloodDispatch d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND " +
           "(:hospitalId IS NULL OR d.hospital.id = :hospitalId) " +
           "GROUP BY d.status")
    List<Object[]> countDispatchesByStatus(@Param("bloodBankId") Long bloodBankId, @Param("hospitalId") Long hospitalId);

    @Query("SELECT d FROM BloodDispatch d WHERE " +
           "(:bloodBankId IS NULL OR d.bloodBank.id = :bloodBankId) AND " +
           "(:hospitalId IS NULL OR d.hospital.id = :hospitalId) " +
           "ORDER BY d.dispatchedAt DESC")
    List<BloodDispatch> findRecentDispatches(@Param("bloodBankId") Long bloodBankId,
                                            @Param("hospitalId") Long hospitalId,
                                            Pageable pageable);
}
