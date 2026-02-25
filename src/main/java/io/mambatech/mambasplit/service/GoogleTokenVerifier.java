package io.mambatech.mambasplit.service;

public interface GoogleTokenVerifier {
  GoogleUser verify(String idToken);

  record GoogleUser(String sub, String email, String name, String picture, boolean emailVerified) {}
}
