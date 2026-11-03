package com.bloodlink.repository;

import com.bloodlink.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    boolean existsByName(String name);
    Optional<Hospital> findByName(String name);
    List<Hospital> findAllByActiveTrue();
}
