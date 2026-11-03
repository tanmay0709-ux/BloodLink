package com.bloodlink.dto;

import com.bloodlink.model.Notification;
import com.bloodlink.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long id;
    private Long recipientId;
    private String recipientUsername;
    private String title;
    private String message;
    private NotificationType type;
    private Long referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto fromEntity(Notification n) {
        return NotificationResponseDto.builder()
            .id(n.getId())
            .recipientId(n.getRecipient() != null ? n.getRecipient().getId() : null)
            .recipientUsername(n.getRecipient() != null ? n.getRecipient().getUsername() : null)
            .title(n.getTitle())
            .message(n.getMessage())
            .type(n.getType())
            .referenceId(n.getReferenceId())
            .referenceType(n.getReferenceType())
            .isRead(n.getIsRead())
            .createdAt(n.getCreatedAt())
            .build();
    }
}
