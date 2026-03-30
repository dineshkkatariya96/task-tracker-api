package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.JwtUtil;
import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.LoginRequest;
import com.tasktracker.task_tracker_api.dto.LoginResponse;
import com.tasktracker.task_tracker_api.dto.RegisterRequest;
import com.tasktracker.task_tracker_api.entity.User;
import com.tasktracker.task_tracker_api.enums.Role;
import com.tasktracker.task_tracker_api.exception.BadRequestException;
import com.tasktracker.task_tracker_api.exception.ForbiddenException;
import com.tasktracker.task_tracker_api.exception.ResourceNotFoundException;
import com.tasktracker.task_tracker_api.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

//request response logging
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("POST /api/auth/register hit for email={}", maskEmail(request.getEmail()));
        logger.debug("Processing user registration with role={}", Role.EMPLOYEE);

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.info("Registration rejected because email already exists for email={}", maskEmail(request.getEmail()));
            throw new BadRequestException(StringConstants.AuthMessages.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.EMPLOYEE);

        logger.debug("Saving new user record for email={}", maskEmail(request.getEmail()));
        userRepository.save(user);
        logger.info("User registration completed for email={}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(StringConstants.AuthMessages.USER_REGISTERED_SUCCESSFULLY);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        logger.info("POST /api/auth/login hit for email={}", maskEmail(request.getEmail()));
        logger.debug("Authenticating user for email={}", maskEmail(request.getEmail()));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        logger.debug("Loading user details after authentication for email={}", maskEmail(request.getEmail()));
        UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getEmail());

        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.USER_NOT_FOUND));

        logger.info("Login completed successfully for email={}", maskEmail(request.getEmail()));
        return ResponseEntity.ok(
                new LoginResponse(token, user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<String> createAdmin(
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String authHeader) {
        logger.info("POST /api/auth/create-admin hit for targetEmail={} authorizationHeaderPresent={}",
                maskEmail(request.getEmail()), authHeader != null && !authHeader.isBlank());

        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            throw new BadRequestException(StringConstants.AuthMessages.INVALID_AUTHORIZATION_HEADER);
        }

        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);

        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.USER_NOT_FOUND));

        logger.debug("Admin creation requested by requesterEmail={} requesterRole={}",
                maskEmail(email), requester.getRole());
        if (requester.getRole() != Role.ADMIN) {
            logger.info("Admin creation rejected for requesterEmail={} due to insufficient privileges", maskEmail(email));
            throw new ForbiddenException(StringConstants.AuthMessages.ONLY_ADMINS_CAN_CREATE);
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.info("Admin creation rejected because email already exists for email={}", maskEmail(request.getEmail()));
            throw new BadRequestException(StringConstants.AuthMessages.EMAIL_ALREADY_EXISTS);
        }

        User admin = new User();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(Role.ADMIN);

        logger.debug("Saving admin user record for email={}", maskEmail(request.getEmail()));
        userRepository.save(admin);
        logger.info("Admin creation completed for email={} by requesterEmail={}",
                maskEmail(request.getEmail()), maskEmail(email));
        return ResponseEntity.ok(StringConstants.AuthMessages.ADMIN_CREATED_SUCCESSFULLY);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "N/A";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
