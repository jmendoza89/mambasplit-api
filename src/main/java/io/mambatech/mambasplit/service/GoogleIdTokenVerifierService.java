package io.mambatech.mambasplit.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleIdTokenVerifierService implements GoogleTokenVerifier {
  private final String clientId;
  private final GoogleIdTokenVerifier verifier;

  public GoogleIdTokenVerifierService(@Value("${app.security.google.client-id:}") String clientId) {
    this.clientId = clientId == null ? "" : clientId.trim();
    this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
      .setAudience(Collections.singletonList(this.clientId))
      .build();
  }

  @Override
  public GoogleUser verify(String idToken) {
    if (clientId.isBlank()) throw new IllegalArgumentException("Google auth is not configured");
    try {
      GoogleIdToken token = verifier.verify(idToken);
      if (token == null) throw new IllegalArgumentException("Invalid Google token");
      GoogleIdToken.Payload payload = token.getPayload();
      String sub = payload.getSubject();
      String email = payload.getEmail();
      if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
        throw new IllegalArgumentException("Invalid Google token");
      }
      Object emailVerified = payload.get("email_verified");
      boolean verified = Boolean.TRUE.equals(emailVerified) || "true".equalsIgnoreCase(String.valueOf(emailVerified));
      return new GoogleUser(sub, email, (String) payload.get("name"), (String) payload.get("picture"), verified);
    } catch (GeneralSecurityException | IOException ex) {
      throw new IllegalArgumentException("Invalid Google token");
    }
  }
}
