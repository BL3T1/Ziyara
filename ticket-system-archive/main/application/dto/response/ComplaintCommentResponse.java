package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complaint comment details")
public class ComplaintCommentResponse {

    @Schema(description = "Comment ID")
    private UUID id;

    @Schema(description = "Author user ID")
    private UUID userId;

    @Schema(description = "Author name")
    private String userName;

    @Schema(description = "Comment content")
    private String comment;

    @Schema(description = "Internal status")
    private Boolean isInternal;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    public static ComplaintCommentResponseBuilder builder() {
        return new ComplaintCommentResponseBuilder();
    }

    public static class ComplaintCommentResponseBuilder {
        private UUID id;
        private UUID userId;
        private String userName;
        private String comment;
        private Boolean isInternal;
        private LocalDateTime createdAt;

        public ComplaintCommentResponseBuilder id(UUID id) { this.id = id; return this; }
        public ComplaintCommentResponseBuilder userId(UUID userId) { this.userId = userId; return this; }
        public ComplaintCommentResponseBuilder userName(String userName) { this.userName = userName; return this; }
        public ComplaintCommentResponseBuilder comment(String comment) { this.comment = comment; return this; }
        public ComplaintCommentResponseBuilder isInternal(Boolean isInternal) { this.isInternal = isInternal; return this; }
        public ComplaintCommentResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ComplaintCommentResponse build() {
            ComplaintCommentResponse r = new ComplaintCommentResponse();
            r.setId(id); r.setUserId(userId); r.setUserName(userName);
            r.setComment(comment); r.setIsInternal(isInternal); r.setCreatedAt(createdAt);
            return r;
        }
    }
}
