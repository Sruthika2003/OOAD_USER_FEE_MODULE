package com.smartcampusmng.campusmanager.service;

import com.smartcampusmng.campusmanager.entity.User;
import com.smartcampusmng.campusmanager.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentFeeService studentFeeService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                      StudentFeeService studentFeeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentFeeService = studentFeeService;
    }

    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // If the user is a student, create initial fees
        if (user.getRole() == User.UserRole.STUDENT) {
            studentFeeService.createInitialFeesForStudent(savedUser);
        }

        return savedUser;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean authenticate(String username, String password, User.UserRole role) {
        try {
            User user = findByUsername(username);
            return passwordEncoder.matches(password, user.getPassword()) && 
                   user.getRole() == role;
        } catch (Exception e) {
            return false;
        }
    }

    public User updateUserProfile(String username, User updatedUser) {
        User existingUser = findByUsername(username);
        
        // Check if the new email is already taken by another user
        if (!existingUser.getEmail().equals(updatedUser.getEmail()) && 
            userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Update only allowed fields
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        
        // Only update password if a new one is provided
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existingUser);
    }

    public List<User> findAllByRole(User.UserRole role) {
        return userRepository.findByRole(role);
    }
} 