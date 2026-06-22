package com.ziyara.backend.domain.usecase.user;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

public class VerifyEmailUseCase {

    private final UserRepository userRepository;

    public VerifyEmailUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        Optional<User> userOpt = userRepository.findById(input.userId());
        if (userOpt.isEmpty()) {
            return Result.failure("User not found");
        }

        User user = userOpt.get();

        if (user.isEmailVerified()) {
            return Result.success(user);
        }

        if (user.getStatus() == UserStatus.DELETED) {
            return Result.failure("Cannot verify a deleted account");
        }

        user.verifyEmail();

        // Automatically activate the account once email is verified
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.activate();
        }

        User saved = userRepository.save(user);
        return Result.success(saved);
    }

    public record Input(UUID userId) {}

    public record Result(boolean success, User user, String error) {
        public static Result success(User user) {
            return new Result(true, user, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
