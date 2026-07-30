package com.github.kio7po.comic_tracker.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.UserRole;
import com.github.kio7po.comic_tracker.domain.exceptions.EmailAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.UsernameAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.WeakPasswordException;
import com.github.kio7po.comic_tracker.domain.port.persistence.UserRepository;
import com.github.kio7po.comic_tracker.domain.port.security.PasswordHasher;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public User register(String username, String email, String rawPassword, String displayName) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException(MIN_PASSWORD_LENGTH);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException(username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setDisplayName(displayName);
        user.setRole(UserRole.USER);

        return userRepository.save(user);
    }

}
