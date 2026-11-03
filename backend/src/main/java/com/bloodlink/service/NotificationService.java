package com.bloodlink.service;

import com.bloodlink.dto.NotificationResponseDto;
import com.bloodlink.model.*;
import com.bloodlink.repository.NotificationRepository;
import com.bloodlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new AccessDeniedException("User not found: " + auth.getName()));
    }

    /**
     * Get paginated notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getMyNotifications(Boolean unreadOnly, int limit) {
        User currentUser = getCurrentUser();
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, safeLimit);

        List<Notification> list;
        if (Boolean.TRUE.equals(unreadOnly)) {
            list = notificationRepository.findAllByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(currentUser.getId(), pageable);
        } else {
            list = notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        }

        return list.stream().map(NotificationResponseDto::fromEntity).toList();
    }

    /**
     * Get count of unread notifications for current user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        return notificationRepository.countByRecipientIdAndIsReadFalse(currentUser.getId());
    }

    /**
     * Mark a single notification as read, enforcing recipient ownership.
     */
    @Transactional
    public NotificationResponseDto markAsRead(Long id) {
        User currentUser = getCurrentUser();
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You cannot modify notifications belonging to another user");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationResponseDto.fromEntity(saved);
    }

    /**
     * Mark all unread notifications of the current user as read.
     */
    @Transactional
    public int markAllAsRead() {
        User currentUser = getCurrentUser();
        return notificationRepository.markAllAsReadForUser(currentUser.getId());
    }

    // ─── Notification Creation Helpers (Executed within caller's transaction) ───

    public void createNotification(User recipient, String title, String message,
                                   NotificationType type, Long referenceId, String referenceType) {
        if (recipient == null) {
            return;
        }
        Notification notification = Notification.builder()
            .recipient(recipient)
            .title(title)
            .message(message)
            .type(type)
            .referenceId(referenceId)
            .referenceType(referenceType)
            .isRead(false)
            .build();
        notificationRepository.save(notification);
    }

    /**
     * Notify all eligible system administrators.
     */
    public void notifyAdmins(String title, String message, NotificationType type, Long refId, String refType) {
        List<User> admins = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.SYSTEM_ADMIN)
            .toList();
        for (User admin : admins) {
            createNotification(admin, title, message, type, refId, refType);
        }
    }

    /**
     * Notify all active blood bank staff across all blood banks (e.g. for new open requests).
     */
    public void notifyAllBloodBankStaff(String title, String message, NotificationType type, Long refId, String refType) {
        List<User> staffList = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.BLOOD_BANK_STAFF && u.getAssignedBloodBank() != null && Boolean.TRUE.equals(u.getAssignedBloodBank().getActive()))
            .toList();
        for (User staff : staffList) {
            createNotification(staff, title, message, type, refId, refType);
        }
    }

    /**
     * Notify all staff members assigned to a specific blood bank.
     */
    public void notifyBloodBankStaff(BloodBank bloodBank, String title, String message,
                                     NotificationType type, Long refId, String refType) {
        if (bloodBank == null) return;
        List<User> staffList = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.BLOOD_BANK_STAFF &&
                         u.getAssignedBloodBank() != null &&
                         u.getAssignedBloodBank().getId().equals(bloodBank.getId()))
            .toList();
        for (User staff : staffList) {
            createNotification(staff, title, message, type, refId, refType);
        }
    }

    /**
     * Notify all staff members assigned to a specific hospital.
     */
    public void notifyHospitalStaff(Hospital hospital, String title, String message,
                                    NotificationType type, Long refId, String refType) {
        if (hospital == null) return;
        List<User> staffList = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.HOSPITAL_STAFF &&
                         u.getAssignedHospital() != null &&
                         u.getAssignedHospital().getId().equals(hospital.getId()))
            .toList();
        for (User staff : staffList) {
            createNotification(staff, title, message, type, refId, refType);
        }
    }
}
