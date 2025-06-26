package com.dealwithpapers.dealwithpapers.service;

import com.dealwithpapers.dealwithpapers.dto.UserLoginDTO;
import com.dealwithpapers.dealwithpapers.dto.UserRegisterDTO;
import com.dealwithpapers.dealwithpapers.dto.UserResponseDTO;
import com.dealwithpapers.dealwithpapers.dto.UserUpdateDTO;
import com.dealwithpapers.dealwithpapers.dto.PasswordUpdateDTO;

public interface UserService {
    UserResponseDTO register(UserRegisterDTO registerDTO);
    UserResponseDTO login(UserLoginDTO loginDTO);
    UserResponseDTO getCurrentUser();
    void logout();
    UserResponseDTO updateUserInfo(UserUpdateDTO updateDTO);
    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);
} 