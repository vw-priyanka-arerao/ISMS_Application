package vwg.cms.c4c.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiDocumentDraftRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 80) String category,
        @Size(max = 80) String reviewerUsername,
        @Size(max = 2000) String prompt
) {
}