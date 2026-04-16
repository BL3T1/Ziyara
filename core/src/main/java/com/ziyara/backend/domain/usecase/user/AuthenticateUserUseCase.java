package com.ziyara.backend.domain.usecase.user;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.UserRepository;
import java.util.Optional;

/**
 * Use Case: Authenticate User
 * Handles user authentication logic
 * Part of Clean Architecture - Domain Layer
 */
public class AuthenticateUserUseCase {
    
    private final UserRepository userRepository;
    
    public AuthenticateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public Result execute(Input input) {
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(input.email());
        
        if (userOpt.isEmpty()) {
            return Result.failure("Invalid email or password");
        }
        
        User user = userOpt.get();
        
        // Check if user can login
        if (!user.getStatus().canLogin()) {
            return Result.failure("Account is not active. Status: " + user.getStatus());
        }
        
        // Check if account is locked
        if (user.isLocked()) {
            return Result.failure("Account is temporarily locked. Please try again later.");
        }
        
        // Verify password (password verification will be done by security layer)
        return Result.success(user);
    }
    
    public record Input(String email, String password) {}
    
    public record Result(boolean success, User user, String error) {
        public static Result success(User user) {
            return new Result(true, user, null);
        }
        
        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
