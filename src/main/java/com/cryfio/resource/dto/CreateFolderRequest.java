package com.cryfio.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFolderRequest(
        UUID parentFolderId,
        @NotBlank(message = "Folder name is required")
        @Size(max = 255,message = "Folder name cannot exceed 255 characters")
        String name
) {}
