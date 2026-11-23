package com.cryfio.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateFolderResponse(
        UUID folderId,
        UUID parentFolderId,
        String name,
        Instant createdAt
) {}

