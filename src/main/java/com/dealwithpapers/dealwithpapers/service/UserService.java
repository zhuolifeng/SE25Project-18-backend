package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;

public interface UserService {
    UserResponseDTO register(UserRegisterDTO registerDTO);
    UserResponseDTO login(UserLoginDTO loginDTO);
    UserResponseDTO getCurrentUser();
    void logout();
} 