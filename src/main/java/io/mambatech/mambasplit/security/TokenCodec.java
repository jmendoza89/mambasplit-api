package io.mambatech.mambasplit.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class TokenCodec {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private TokenCodec() {}

  public static String randomUrlToken(int bytes) {
    if (bytes < 16) {
      throw new IllegalArgumentException("Token size too small");
    }
    byte[] value = new byte[bytes];
    SECURE_RANDOM.nextBytes(value);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  public static String sha256Base64Url(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
