package com.queueless.security;

import com.queueless.entity.User;
import com.queueless.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsService implementation for loading credentials from UserRepository.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by email (our system's username).
     *
     * @param email user's email address
     * @return UserDetails representing the user principal
     * @throws UsernameNotFoundException if user doesn't exist in the database
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        return UserPrincipal.create(user);
    }

    /**
     * Loads a user by ID. Useful when decoding JWT tokens containing the user's primary key.
     *
     * @param id the user's database ID
     * @return UserDetails representing the user principal
     * @throws UsernameNotFoundException if user doesn't exist
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + id
                ));

        return UserPrincipal.create(user);
    }
}
