package com.template.OAuth.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ProfileUpdateHistoryDto {
    private Long id;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Instant updateDate;
}