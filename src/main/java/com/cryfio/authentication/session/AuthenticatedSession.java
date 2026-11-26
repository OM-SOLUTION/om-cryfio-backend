package com.cryfio.authentication.session;

import java.util.UUID;

public record AuthenticatedSession(
        UUID accountId,
        UUID sessionId
) {}
