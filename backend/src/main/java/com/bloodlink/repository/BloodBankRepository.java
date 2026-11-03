package com.bloodlink.repository;

import com.bloodlink.model.BloodBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, Long> {
    boolean existsByName(String name);
    Optional<BloodBank> findByName(String name);
    List<BloodBank> findAllByActiveTrue();
}
