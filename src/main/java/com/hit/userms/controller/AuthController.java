package com.hit.userms.controller;

import com.hit.userms.dto.UserDTO;
import com.hit.userms.exception.RefreshTokenExpiredException;
import com.hit.userms.model.RefreshToken;
import com.hit.userms.model.UserEO;
import com.hit.userms.repo.RefreshTokenRepo;
import com.hit.userms.repo.UserRepo;
import com.hit.userms.request.LoginRequest;
import com.hit.userms.request.OtpVerifyRequest;
import com.hit.userms.request.PasswordChangeRequest;
import com.hit.userms.response.ApiResponse;
import com.hit.userms.security.AuthResponse;
import com.hit.userms.security.JwtHelper;
import com.hit.userms.service.ImageService;
import com.hit.userms.service.UserService;
import jakarta.persistence.Enumerated;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenRepo refreshTokenRepo;
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private JwtHelper helper;

    @Autowired
    private ImageService imageService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDTO userDTO) {
        UserDTO response = this.userService.registerUser(userDTO);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {

        this.doAuthenticate(request.getUsername(), request.getPassword());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = this.helper.generateToken(userDetails);

        UserEO userEO = this.userRepo.findByEmail(userDetails.getUsername());

        if (this.refreshTokenRepo.findByUserEO(userEO) != null) {
            this.refreshTokenRepo.delete(this.refreshTokenRepo.findByUserEO(userEO));
        }

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setRToken(UUID.randomUUID().toString());

        refreshToken.setExpiryTime(LocalDateTime.now().plusDays(30));

        refreshToken.setUserEO(userEO);

        RefreshToken saved = this.refreshTokenRepo.save(refreshToken);

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .role(userEO.getRole())
                .refreshToken(saved.getRToken())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> generateToneWithRefreshToken(
            @RequestParam("token") String token) {
        RefreshToken refreshToken = this.refreshTokenRepo.findByRToken(token);

        if (refreshToken != null && refreshToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenExpiredException("Kindly login again");
        }

        UserEO userEO = this.userRepo
                .findById(refreshToken.getUserEO().getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User is not present"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEO.getEmail());
        String bearer_token = this.helper.generateToken(userDetails);

        AuthResponse response = AuthResponse.builder()
                .token(bearer_token)
                .role(userEO.getRole())
                .refreshToken(refreshToken.getRToken())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void doAuthenticate(String email, String password) {

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, password);
        try {
            manager.authenticate(authentication);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException(" Invalid Username or Password !!");
        }

    }

    @GetMapping("/user-details")
    public ResponseEntity<?> getUserDetails(
            @RequestHeader("Authorization") String header) {
        return new ResponseEntity<>(this.userService.getUserFromToken(header), HttpStatus.OK);
    }

    @PostMapping("/send-otp")
    public ApiResponse sendPasswordResetOtp(
            @RequestParam("email") String email) {
        return this.userService.generateOtp(email);

    }

    @PostMapping("/re-send-otp")
    public ApiResponse resendPasswordResetOtp(
            @RequestParam("email") String email) {
        return this.userService.resendOtp(email);
    }

    @PostMapping("/verify-otp")
    public ApiResponse verifyOtp(@RequestBody OtpVerifyRequest otpVerifyRequest) {

        return this.userService.verifyOtp(otpVerifyRequest);

    }

    @PostMapping("/change-password")
    public ApiResponse changePassword(@RequestBody PasswordChangeRequest passwordChangeRequest) {

        return this.userService.passWordChangeRequest(passwordChangeRequest);
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestHeader("Authorization") String header,
            @RequestParam("file") MultipartFile multipartFile

    ) throws IOException {

        String token = header.substring(7);
        String response = this.imageService.upLoadImage(token, multipartFile);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/download-image/{id}")
    public ResponseEntity<?> downloadImage(@PathVariable String id) throws IOException {
        byte[] imageData = imageService.getImage(id);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.valueOf("image/png"))
                .body(imageData);

    }

}
