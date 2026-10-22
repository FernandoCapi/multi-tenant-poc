package com.vesta.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vesta.auth.LoginCredentialsRepository.LoginCredentials;

@Service
public class AuthService {

    private final LoginCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    // Compared against when the e-mail does not exist, so both failures cost the same time
    private final String unknownUserHash;

    public AuthService(
            LoginCredentialsRepository credentialsRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Unknown e-mail and wrong password fail the same way (CA-05.2). */
    public LoginResponse login(String email, String password) {
        Optional<LoginCredentials> credentials = credentialsRepository.findByEmail(email);
        String hash = credentials.map(LoginCredentials::passwordHash).orElse(unknownUserHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);
        if (credentials.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return tokenService.issue(credentials.get().userId(), credentials.get().familyId());
    }
}
