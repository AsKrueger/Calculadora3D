package com.tdcostmanager.backend.application.service;

import com.tdcostmanager.backend.application.dto.AuthResponse;
import com.tdcostmanager.backend.application.dto.LoginRequest;
import com.tdcostmanager.backend.application.dto.RegisterRequest;
import com.tdcostmanager.backend.domain.model.Role;
import com.tdcostmanager.backend.domain.model.User;
import com.tdcostmanager.backend.domain.repository.UserRepository;
import com.tdcostmanager.backend.infrastructure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("El email ya está registrado");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER); // Rol por defecto
        user.setEnabled(true);

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(); // Ya validado por authenticationManager

        String jwtToken = jwtService.generateToken(user);
        
        return new AuthResponse(
                jwtToken,
                "Bearer",
                jwtService.getExpirationTime() / 1000 // Segundos
        );
    }
}
