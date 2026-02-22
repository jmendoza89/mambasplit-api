package io.mambatech.mambasplit.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
  private final AppSecurityProperties props;
  private final Key key;

  public JwtService(AppSecurityProperties props) {
    this.props = props;
    this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(UUID userId, String email) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.accessTokenMinutes(), ChronoUnit.MINUTES);

    return Jwts.builder()
      .issuer(props.issuer())
      .subject(userId.toString())
      .claim("email", email)
      .issuedAt(Date.from(now))
      .expiration(Date.from(exp))
      .signWith(key)
      .compact();
  }

  public Jws<Claims> parse(String jwt) {
    return Jwts.parser()
      .verifyWith((javax.crypto.SecretKey) key)
      .requireIssuer(props.issuer())
      .build()
      .parseSignedClaims(jwt);
  }
}
