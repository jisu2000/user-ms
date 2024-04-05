package com.hit.userms.service;

import com.hit.userms.dto.UserDTO;
import com.hit.userms.request.OtpVerifyRequest;
import com.hit.userms.request.PasswordChangeRequest;
import com.hit.userms.response.ApiResponse;

public interface UserService {

    UserDTO registerUser(UserDTO userDTO);

    UserDTO getUserFromToken(String token);

    ApiResponse generateOtp(String email);

    ApiResponse resendOtp(String email);
    ApiResponse verifyOtp(OtpVerifyRequest otpVerifyRequest);

    ApiResponse passWordChangeRequest(PasswordChangeRequest passwordChangeRequest);
}
