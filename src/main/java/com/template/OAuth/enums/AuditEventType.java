package com.template.OAuth.enums;

public enum AuditEventType {
    // Authentication events
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_REFRESH,

    // User management events
    USER_CREATED,
    USER_UPDATED,
    USER_ROLE_CHANGED,
    USER_DELETED,

    // Profile events
    PROFILE_UPDATED,
    PROFILE_PICTURE_UPDATED,

    // Security events
    ACCESS_DENIED,
    PERMISSION_CHANGE,

    // Administrative events
    CONFIG_CHANGED,
    SYSTEM_EVENT,

    // Data events
    DATA_ACCESSED,
    DATA_CREATED,
    DATA_UPDATED,
    DATA_DELETED
}