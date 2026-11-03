package com.bloodlink.service;

import com.bloodlink.dto.DonorRequest;
import com.bloodlink.model.BloodBank;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.Donor;
import com.bloodlink.model.User;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.DonorRepository;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonorService {

    private final DonorRepository donorRepository;
    private final BloodBankRepository bloodBankRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    private Long getEnforcedBloodBankId(User currentUser) {
        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            return null; // admin has global access
        } else if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            return currentUser.getAssignedBloodBank().getId();
        } else {
            throw new AccessDeniedException("Access denied: Hospital staff cannot access donor records");
        }
    }

    public List<Donor> getAllDonors(Long requestedBloodBankId) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        if (enforcedId != null) {
            return donorRepository.findAllByBloodBankId(enforcedId);
        }

        // Admin
        if (requestedBloodBankId != null) {
            return donorRepository.findAllByBloodBankId(requestedBloodBankId);
        }
        return donorRepository.findAll();
    }

    public Donor getDonorById(Long id) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        Donor donor = donorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Donor not found with id: " + id));

        if (enforcedId != null && !donor.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only access donors belonging to your assigned blood bank");
        }

        return donor;
    }

    public List<Donor> searchDonors(String query, BloodInventory.BloodType bloodType, Long adminBankFilter) {
        User currentUser = getCurrentUser();
        Long targetBankId = getEnforcedBloodBankId(currentUser);
        if (targetBankId == null && adminBankFilter != null) {
            targetBankId = adminBankFilter;
        }

        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasBloodType = bloodType != null;

        if (hasQuery && hasBloodType) {
            return donorRepository.searchDonorsWithBloodType(targetBankId, query.trim(), bloodType);
        } else if (hasQuery) {
            return donorRepository.searchDonors(targetBankId, query.trim());
        } else if (hasBloodType) {
            return donorRepository.findByBloodType(targetBankId, bloodType);
        } else {
            return getAllDonors(targetBankId);
        }
    }

    @Transactional
    public Donor registerDonor(DonorRequest request) {
        User currentUser = getCurrentUser();

        // Blood bank determination
        BloodBank bloodBank;
        if (currentUser.getRole() == User.Role.BLOOD_BANK_STAFF) {
            if (currentUser.getAssignedBloodBank() == null
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.PENDING
                    || currentUser.getApprovalStatus() == User.ApprovalStatus.REJECTED) {
                throw new AccessDeniedException("Staff member is not an approved member of any blood bank");
            }
            bloodBank = currentUser.getAssignedBloodBank();
        } else if (currentUser.getRole() == User.Role.SYSTEM_ADMIN) {
            if (request.getBloodBankId() == null) {
                // If not provided by admin, find the first active blood bank
                bloodBank = bloodBankRepository.findAllByActiveTrue().stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active blood bank found. Please create one first or provide bloodBankId."));
            } else {
                bloodBank = bloodBankRepository.findById(request.getBloodBankId())
                    .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + request.getBloodBankId()));
            }
        } else {
            throw new AccessDeniedException("Access denied: Hospital staff cannot register donors");
        }

        if (!bloodBank.getActive()) {
            throw new IllegalArgumentException("Cannot register a donor to an inactive blood bank");
        }

        // Validate duplicates
        if (donorRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("A donor with phone number '" + request.getPhone() + "' is already registered");
        }
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (donorRepository.existsByEmail(request.getEmail().trim())) {
                throw new IllegalArgumentException("A donor with email '" + request.getEmail().trim() + "' is already registered");
            }
        }

        // Validate age (18 - 65)
        if (request.getDateOfBirth() != null) {
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 18) {
                throw new IllegalArgumentException("Donor must be at least 18 years old (Current age: " + age + ")");
            }
            if (age > 65) {
                throw new IllegalArgumentException("Donor must be 65 years old or younger (Current age: " + age + ")");
            }
        }

        Donor donor = Donor.builder()
            .fullName(request.getFullName())
            .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null)
            .phone(request.getPhone())
            .bloodType(request.getBloodType())
            .gender(request.getGender())
            .dateOfBirth(request.getDateOfBirth())
            .address(request.getAddress())
            .bloodBank(bloodBank)
            .eligible(true)
            .build();

        return donorRepository.save(donor);
    }

    @Transactional
    public Donor updateDonor(Long id, DonorRequest request) {
        Donor donor = getDonorById(id); // automatically enforces access rights

        // Check duplicate phone
        if (donorRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new IllegalArgumentException("Another donor with phone number '" + request.getPhone() + "' already exists");
        }

        // Check duplicate email
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            if (donorRepository.existsByEmailAndIdNot(request.getEmail().trim(), id)) {
                throw new IllegalArgumentException("Another donor with email '" + request.getEmail().trim() + "' already exists");
            }
            donor.setEmail(request.getEmail().trim());
        } else {
            donor.setEmail(null);
        }

        // Validate age
        if (request.getDateOfBirth() != null) {
            int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
            if (age < 18) {
                throw new IllegalArgumentException("Donor must be at least 18 years old (Current age: " + age + ")");
            }
            if (age > 65) {
                throw new IllegalArgumentException("Donor must be 65 years old or younger (Current age: " + age + ")");
            }
            donor.setDateOfBirth(request.getDateOfBirth());
        }

        donor.setFullName(request.getFullName());
        donor.setPhone(request.getPhone());
        donor.setBloodType(request.getBloodType());
        donor.setGender(request.getGender());
        donor.setAddress(request.getAddress());

        return donorRepository.save(donor);
    }
}
