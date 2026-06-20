package com.ziyara.backend.domain.usecase.user;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class DeactivateUserUseCase {

    private final UserRepository userRepository;

    public DeactivateUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        Optional<User> userOpt = userRepository.findById(input.userId());
        if (userOpt.isEmpty()) {
            return Result.failure("User not found");
        }

        User user = userOpt.get();

        if (user.getStatus() == UserStatus.DELETED) {
            return Result.failure("User account is already deactivated");
        }

        // Prevent self-deactivation of a super admin without explicit override
        if (input.userId().equals(input.deactivatedBy())) {
            return Result.failure("Users cannot deactivate their own account");
        }

        user.softDelete();
        userRepository.save(user);
        return Result.ok();
    }

    public record Input(UUID userId, UUID deactivatedBy) {}

    public record Result(boolean success, String error) {
        public static Result ok() {
            return new Result(true, null);
        }

        public static Result failure(String error) {
            return new Result(false, error);
        }
    }
}
