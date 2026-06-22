package com.ziyara.backend.domain.usecase.user;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.UUID;

public class RegisterUserUseCase {

    private final UserRepository userRepository;

    public RegisterUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        if (input.email() == null || input.email().isBlank()) {
            return Result.failure("Email is required");
        }

        if (userRepository.existsByEmail(input.email())) {
            return Result.failure("An account with this email already exists");
        }

        if (input.phone() != null && !input.phone().isBlank()
                && userRepository.existsByPhone(input.phone())) {
            return Result.failure("An account with this phone number already exists");
        }

        User user = new User(UUID.randomUUID(), input.email(), input.phone(),
                input.passwordHash(), input.role());

        User saved = userRepository.save(user);
        return Result.success(saved);
    }

    public record Input(String email, String phone, String passwordHash, UserRole role) {}

    public record Result(boolean success, User user, String error) {
        public static Result success(User user) {
            return new Result(true, user, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
