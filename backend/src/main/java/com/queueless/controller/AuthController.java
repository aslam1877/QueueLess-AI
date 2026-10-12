package com.queueless.controller;

import com.queueless.dto.request.LoginRequest;
import com.queueless.dto.request.RegisterRequest;
import com.queueless.dto.response.AuthResponse;
import com.queueless.dto.response.UserResponse;
import com.queueless.security.UserPrincipal;
import com.queueless.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * REST controller for user registration, authentication, and profile retrieval.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructor injection — Spring provides the AuthService bean.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     *
     * POST /api/auth/register
     *
     * Request body:
     * {
     *   "name": "Aslam",
     *   "email": "aslam@example.com",
     *   "password": "securePass123",
     *   "role": "CUSTOMER"        ← optional, defaults to CUSTOMER
     * }
     *
     * Success response (201 Created):
     * {
     *   "id": 1,
     *   "name": "Aslam",
     *   "email": "aslam@example.com",
     *   "role": "CUSTOMER",
     *   "createdAt": "2024-01-15T10:30:00"
     * }
     *
     * Notice: password is NOT in the response.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Authenticate user credentials and return a signed JWT token.
     *
     * POST /api/auth/login
     *
     * Request body:
     * {
     *   "email": "aslam@example.com",
     *   "password": "securePass123"
     * }
     *
     * Response (200 OK):
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1Ni...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 86400000,
     *   "user": {
     *     "id": 1,
     *     "name": "Aslam",
     *     "email": "aslam@example.com",
     *     "role": "CUSTOMER",
     *     "createdAt": "..."
     *   }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the authenticated user's profile.
     * Requires Authorization: Bearer <token>
     *
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a user by ID.
     *
     * GET /api/auth/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = authService.getUserById(id);
        return ResponseEntity.ok(response);
    }
}
