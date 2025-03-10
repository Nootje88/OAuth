package com.template.OAuth.dto;

import com.template.OAuth.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class NotificationPreferencesDto {
    private Set<NotificationType> enabledNotifications;
}