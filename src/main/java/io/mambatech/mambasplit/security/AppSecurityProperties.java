package io.mambatech.mambasplit.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record AppSecurityProperties(
  @NotBlank String issuer,
  @NotBlank String secret,
  @Min(1) int accessTokenMinutes,
  @Min(1) int refreshTokenDays
) {}
