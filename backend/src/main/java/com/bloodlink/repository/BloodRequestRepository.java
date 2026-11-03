package com.bloodlink.repository;

import com.bloodlink.model.BloodRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId);

    List<BloodRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM BloodRequest r WHERE " +
           "(:hospitalId IS NULL OR r.hospital.id = :hospitalId) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:priority IS NULL OR r.priority = :priority) " +
           "ORDER BY r.createdAt DESC")
    List<BloodRequest> findWithFilters(@Param("hospitalId") Long hospitalId,
                                     @Param("status") BloodRequest.RequestStatus status,
                                     @Param("priority") BloodRequest.RequestPriority priority);

    @Query("SELECT r.status, COUNT(r) FROM BloodRequest r WHERE " +
           "(:hospitalId IS NULL OR r.hospital.id = :hospitalId) " +
           "GROUP BY r.status")
    List<Object[]> countRequestsByStatus(@Param("hospitalId") Long hospitalId);

    @Query("SELECT r.priority, COUNT(r) FROM BloodRequest r WHERE " +
           "(:hospitalId IS NULL OR r.hospital.id = :hospitalId) " +
           "GROUP BY r.priority")
    List<Object[]> countRequestsByPriority(@Param("hospitalId") Long hospitalId);

    @Query("SELECT r FROM BloodRequest r WHERE " +
           "(:hospitalId IS NULL OR r.hospital.id = :hospitalId) " +
           "ORDER BY r.createdAt DESC")
    List<BloodRequest> findRecentRequests(@Param("hospitalId") Long hospitalId, Pageable pageable);
}
