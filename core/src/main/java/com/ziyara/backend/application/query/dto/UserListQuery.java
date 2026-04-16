package com.ziyara.backend.application.query.dto;

import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

/**
 * Query parameters for paginated user list (CQRS query side).
 */
@Data
@Builder
public class UserListQuery {
    private int page;
    private int size;
    private UserStatus status;
    private UserRole role;

    public int getOffset() {
        return page * size;
    }
}
