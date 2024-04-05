package com.hit.userms.service.impl;

import com.google.protobuf.Api;
import com.hit.userms.config.AppConstant;
import com.hit.userms.dto.UserDTO;
import com.hit.userms.enums.Role;
import com.hit.userms.model.RefreshToken;
import com.hit.userms.model.UserEO;
import com.hit.userms.repo.RefreshTokenRepo;
import com.hit.userms.repo.UserRepo;
import com.hit.userms.request.EmailRequest;
import com.hit.userms.request.OtpVerifyRequest;
import com.hit.userms.request.PasswordChangeRequest;
import com.hit.userms.response.ApiResponse;
import com.hit.userms.security.JwtHelper;
import com.hit.userms.service.UserService;
import com.hit.userms.utils.OtpHelper;
import com.hit.userms.webclients.MailRestemplate;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private MailRestemplate mailRestemplate;

    private final HashMap<Integer, LocalDateTime> otpTimeStamp = new HashMap<>();

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDTO registerUser(UserDTO userDTO) {

        UserEO userEO = this.modelMapper.map(userDTO, UserEO.class);
        userEO.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        userEO.setRole(Role.USER);
        UserEO savedUser = this.userRepo.save(userEO);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRToken(UUID.randomUUID().toString());
        refreshToken.setExpiryTime(LocalDateTime.now().plusDays(30));
        refreshToken.setUserEO(userEO);
        RefreshToken saved = this.refreshTokenRepo.save(refreshToken);

        return modelMapper.map(userEO, UserDTO.class);
    }

    @Override
    public UserDTO getUserFromToken(String header) {

        String token = header.substring(7);

        if (jwtHelper.isTokenExpired(token)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token is Expired");
        }

        String userName = jwtHelper.getUsernameFromToken(token);

        UserEO userEO = this.userRepo.findByEmail(userName);

        return this.modelMapper.map(userEO, UserDTO.class);

    }

    @Override
    public ApiResponse generateOtp(String email) {

        UserEO userEO = this.userRepo.findByEmail(email);

        if (userEO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found with the provided email");
        }

        if (otpTimeStamp.get(userEO.getUserId()) != null) {

            long differenceInSeconds = Math.abs(ChronoUnit.SECONDS.between(otpTimeStamp.get(userEO.getUserId()),
                    LocalDateTime.now()));

            boolean isLessThanTwoMinutes = differenceInSeconds < 120;

            if (isLessThanTwoMinutes) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wait until 2 minutes");
            }
        }
        String otp = OtpHelper.generateOTP(6);

        String body = AppConstant.generateOtpMsg(userEO.getFullName(), otp);

        EmailRequest request = new EmailRequest();
        request.setSubject("Password Reset OTP");
        request.setBody(body);
        request.setRecipentEmail(email);

        boolean result = mailRestemplate.sendMail(request);

        userEO.setOtp(otp);
        userEO.setOtpExpirationTime(LocalDateTime.now().plusMinutes(2));
        otpTimeStamp.put(userEO.getUserId(), LocalDateTime.now());

        this.userRepo.save(userEO);
        return ApiResponse
                .builder()
                .msg("Otp send Successfully")
                .status(HttpStatus.OK).build();
    }

    @Override
    public ApiResponse resendOtp(String email) {

        UserEO userEO = this.userRepo.findByEmail(email);

        if (userEO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found with the provided email");
        }

        if (otpTimeStamp.get(userEO.getUserId()) != null) {

            long differenceInSeconds = Math.abs(ChronoUnit.SECONDS.between(otpTimeStamp.get(userEO.getUserId()),
                    LocalDateTime.now()));

            boolean isLessThanTwoMinutes = differenceInSeconds < 60;

            if (isLessThanTwoMinutes) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wait until 1 minutes");
            }
        }
        String otp = OtpHelper.generateOTP(6);

        String body = AppConstant.generateOtpMsg(userEO.getFullName(), otp);

        EmailRequest request = new EmailRequest();
        request.setSubject("Password Reset OTP");
        request.setBody(body);
        request.setRecipentEmail(email);

        boolean result = mailRestemplate.sendMail(request);

        userEO.setOtp(otp);
        userEO.setOtpExpirationTime(LocalDateTime.now().plusMinutes(2));
        otpTimeStamp.put(userEO.getUserId(), LocalDateTime.now());

        this.userRepo.save(userEO);
        return ApiResponse.builder().msg("Otp sent successfully").status(HttpStatus.OK).build();
    }

    @Override
    public ApiResponse verifyOtp(OtpVerifyRequest otpVerifyRequest) {

        UserEO userEO = this.userRepo.findByEmail(otpVerifyRequest.getEmail());

        if (userEO == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not present");
        }

        if (otpVerifyRequest.getOtp().equals(userEO.getOtp())
                && userEO.getOtpExpirationTime().isAfter(LocalDateTime.now())) {
            return ApiResponse.builder().msg("OTP verified").status(HttpStatus.OK).build();
        }

        return ApiResponse.builder().msg("OTP not verified").status(HttpStatus.BAD_REQUEST).build();

    }

    @Override
    public ApiResponse passWordChangeRequest(PasswordChangeRequest passwordChangeRequest) {
        UserEO userEO = this.userRepo.findByEmail(passwordChangeRequest.getEmail());

        if (userEO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found with the provided email");
        }

        userEO.setPassword(passwordEncoder.encode(passwordChangeRequest.getPassword()));
        this.userRepo.save(userEO);

        String body = AppConstant.generatePassChangeMsg(userEO.getFullName());

        String sub = "Password changed Message";

        String recipent = passwordChangeRequest.getEmail();

        EmailRequest request = new EmailRequest();
        request.setSubject(sub);
        request.setBody(body);
        request.setRecipentEmail(recipent);

        boolean result = mailRestemplate.sendMail(request);

        return ApiResponse.builder().msg("Password Changed successfully").status(HttpStatus.OK).build();
    }
}
