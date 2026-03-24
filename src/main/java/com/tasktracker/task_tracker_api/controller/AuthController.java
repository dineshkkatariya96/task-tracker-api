package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.JwtUtil;
import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.LoginRequest;
import com.tasktracker.task_tracker_api.dto.LoginResponse;
import com.tasktracker.task_tracker_api.dto.RegisterRequest;
import com.tasktracker.task_tracker_api.entity.User;
import com.tasktracker.task_tracker_api.enums.Role;
import com.tasktracker.task_tracker_api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(StringConstants.AuthMessages.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.EMPLOYEE);

        userRepository.save(user);
        return ResponseEntity.ok(StringConstants.AuthMessages.USER_REGISTERED_SUCCESSFULLY);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(request.getEmail());

        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail()).get();

        return ResponseEntity.ok(
                new LoginResponse(token, user.getEmail(), user.getRole().name()));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(
            @RequestBody RegisterRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);

        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.USER_NOT_FOUND));

        if (requester.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(StringConstants.AuthMessages.ONLY_ADMINS_CAN_CREATE);
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(StringConstants.AuthMessages.EMAIL_ALREADY_EXISTS);
        }

        User admin = new User();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);
        return ResponseEntity.ok(StringConstants.AuthMessages.ADMIN_CREATED_SUCCESSFULLY);
    }
}