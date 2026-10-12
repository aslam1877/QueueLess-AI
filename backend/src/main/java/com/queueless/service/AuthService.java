package com.queueless.service;

import com.queueless.dto.request.LoginRequest;
import com.queueless.dto.request.RegisterRequest;
import com.queueless.dto.response.AuthResponse;
import com.queueless.dto.response.UserResponse;
import com.queueless.entity.User;
import com.queueless.enums.UserRole;
import com.queueless.exception.DuplicateResourceException;
import com.queueless.exception.ResourceNotFoundException;
import com.queueless.repository.UserRepository;
import com.queueless.security.JwtTokenProvider;
import com.queueless.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration, authentication, and JWT issuance.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                "A user with email '" + request.getEmail() + "' already exists"
            );
        }

        UserRole role = parseRole(request.getRole());
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
            request.getName(),
            request.getEmail(),
            hashedPassword,
            role
        );

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + principal.getEmail()
                ));

        return new AuthResponse(
                jwt,
                "Bearer",
                jwtTokenProvider.getExpirationMs(),
                UserResponse.from(user)
        );
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param principal authenticated user principal
     * @return current user profile
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + principal.getId()
                ));

        return UserResponse.from(user);
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user's ID
     * @return the user's public information
     * @throws ResourceNotFoundException if no user exists with that ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + id
            ));

        return UserResponse.from(user);
    }

    /**
     * Parses the role string from the request.
     * Defaults to CUSTOMER if not specified.
     *
     * @param roleString the role from the request (may be null)
     * @return the corresponding UserRole enum value
     * @throws IllegalArgumentException if the role string is invalid
     */
    private UserRole parseRole(String roleString) {
        if (roleString == null || roleString.isBlank()) {
            return UserRole.CUSTOMER;
        }

        try {
            return UserRole.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid role: '" + roleString + "'. Valid roles are: CUSTOMER, BUSINESS_OWNER, STAFF"
            );
        }
    }
}
