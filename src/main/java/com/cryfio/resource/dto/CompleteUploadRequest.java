package com.cryfio.resource.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CompleteUploadRequest(
        @NotNull
        List<@Valid CompletedUploadRequest> parts
) {
    public record CompletedUploadRequest(
            @Min(1)
            int partNumber,
            @NotBlank
            String eTag
    ){}
}
