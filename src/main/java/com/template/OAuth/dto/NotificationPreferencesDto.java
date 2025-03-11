package com.template.OAuth.dto;

import com.template.OAuth.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class NotificationPreferencesDto {
    @NotNull(message = "Enabled notifications cannot be null")
    private Set<NotificationType> enabledNotifications;
}