package io.mambatech.mambasplit.security;

import java.util.UUID;

public record AuthPrincipal(UUID userId, String email) {}
