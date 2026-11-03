package com.bloodlink.service;

import com.bloodlink.dto.DonationRequest;
import com.bloodlink.dto.DonationResponse;
import com.bloodlink.model.BloodBank;
import com.bloodlink.model.BloodInventory;
import com.bloodlink.model.Donation;
import com.bloodlink.model.Donor;
import com.bloodlink.model.User;
import com.bloodlink.repository.BloodBankRepository;
import com.bloodlink.repository.DonationRepository;
import com.bloodlink.repository.DonorRepository;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationService {

    private final DonationRepository donationRepository;
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
            throw new AccessDeniedException("Access denied: Hospital staff cannot access donation records");
        }
    }

    public List<DonationResponse> getAllDonations(Long requestedBloodBankId) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        List<Donation> list;
        if (enforcedId != null) {
            list = donationRepository.findAllByBloodBankId(enforcedId);
        } else if (requestedBloodBankId != null) {
            list = donationRepository.findAllByBloodBankId(requestedBloodBankId);
        } else {
            list = donationRepository.findAll();
        }
        return list.stream().map(DonationResponse::fromEntity).toList();
    }

    public DonationResponse getDonationById(Long id) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Donation not found with id: " + id));

        if (enforcedId != null && !donation.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only access donations belonging to your assigned blood bank");
        }

        return DonationResponse.fromEntity(donation);
    }

    public List<DonationResponse> getDonationsByDonor(Long donorId) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        Donor donor = donorRepository.findById(donorId)
            .orElseThrow(() -> new IllegalArgumentException("Donor not found with id: " + donorId));

        if (enforcedId != null && !donor.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only access donations belonging to your assigned blood bank");
        }

        List<Donation> list = donationRepository.findAllByDonorIdOrderByDonationDateDesc(donorId);
        return list.stream().map(DonationResponse::fromEntity).toList();
    }

    @Transactional
    public DonationResponse recordDonation(DonationRequest request) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        Donor donor = donorRepository.findById(request.getDonorId())
            .orElseThrow(() -> new IllegalArgumentException("Donor not found with id: " + request.getDonorId()));

        if (enforcedId != null && !donor.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only record donations for donors at your assigned blood bank");
        }

        // Determine blood bank
        BloodBank bloodBank = donor.getBloodBank();
        if (currentUser.getRole() == User.Role.SYSTEM_ADMIN && request.getBloodBankId() != null) {
            bloodBank = bloodBankRepository.findById(request.getBloodBankId())
                .orElseThrow(() -> new IllegalArgumentException("Blood bank not found with id: " + request.getBloodBankId()));
        }

        LocalDate donationDate = request.getDonationDate() != null ? request.getDonationDate() : LocalDate.now();

        // Prevent future donation dates
        if (donationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Donation date cannot be in the future");
        }

        Donation.DonationStatus status = request.getStatus() != null ? request.getStatus() : Donation.DonationStatus.COMPLETED;

        // Eligibility interval check (56 days between completed donations)
        if (status == Donation.DonationStatus.COMPLETED && donor.getLastDonationDate() != null) {
            long daysSinceLast = ChronoUnit.DAYS.between(donor.getLastDonationDate(), donationDate);
            if (daysSinceLast < 56 && daysSinceLast >= 0) {
                long remaining = 56 - daysSinceLast;
                throw new IllegalArgumentException("Donor is not eligible yet. Minimum 56 days required between donations. Eligible from "
                    + donor.getLastDonationDate().plusDays(56) + " (" + remaining + " days remaining).");
            }
        }

        BloodInventory.BloodType bloodType = request.getBloodType() != null ? request.getBloodType() : donor.getBloodType();

        Donation donation = Donation.builder()
            .donor(donor)
            .bloodBank(bloodBank)
            .donationDate(donationDate)
            .bloodType(bloodType)
            .quantity(request.getQuantity())
            .location(request.getLocation() != null && !request.getLocation().trim().isEmpty() ? request.getLocation().trim() : bloodBank.getName())
            .status(status)
            .notes(request.getNotes())
            .build();

        Donation saved = donationRepository.save(donation);

        // If completed, update donor's last donation date and eligibility
        if (status == Donation.DonationStatus.COMPLETED) {
            if (donor.getLastDonationDate() == null || donationDate.isAfter(donor.getLastDonationDate())) {
                donor.setLastDonationDate(donationDate);
                donorRepository.save(donor);
            }
        }

        return DonationResponse.fromEntity(saved);
    }

    @Transactional
    public DonationResponse updateStatus(Long id, Donation.DonationStatus newStatus) {
        User currentUser = getCurrentUser();
        Long enforcedId = getEnforcedBloodBankId(currentUser);

        Donation donation = donationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Donation not found with id: " + id));

        if (enforcedId != null && !donation.getBloodBank().getId().equals(enforcedId)) {
            throw new AccessDeniedException("Access denied: You can only update donations belonging to your assigned blood bank");
        }

        donation.setStatus(newStatus);
        Donation saved = donationRepository.save(donation);

        // Update donor if changed to COMPLETED
        if (newStatus == Donation.DonationStatus.COMPLETED) {
            Donor donor = donation.getDonor();
            if (donor.getLastDonationDate() == null || donation.getDonationDate().isAfter(donor.getLastDonationDate())) {
                donor.setLastDonationDate(donation.getDonationDate());
                donorRepository.save(donor);
            }
        }

        return DonationResponse.fromEntity(saved);
    }
}
