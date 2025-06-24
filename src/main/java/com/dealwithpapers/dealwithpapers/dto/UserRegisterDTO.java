package com.dealwithpapers.dealwithpapers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDTO {
    private String username;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
} 