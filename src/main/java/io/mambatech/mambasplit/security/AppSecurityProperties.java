package io.mambatech.mambasplit.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record AppSecurityProperties(
  String issuer,
  String secret,
  int accessTokenMinutes,
  int refreshTokenDays
) {}
