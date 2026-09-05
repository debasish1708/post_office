package com.postoffice.service;

import com.postoffice.dto.AuthRequest;
import com.postoffice.dto.UserDto;
import com.postoffice.model.User;
import com.postoffice.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public UserDto register(AuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new IllegalArgumentException("An account already exists for this email");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBalance(new BigDecimal("100.00"));
        userRepository.save(user);

        login(request.getEmail().trim().toLowerCase(), request.getPassword(), httpRequest, httpResponse);
        return UserDto.from(user);
    }

    public UserDto login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(), password));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        securityContextRepository.saveContext(context, request, response);

        User user = requireCurrentUser();
        return UserDto.from(user);
    }

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Not logged in");
        }
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
