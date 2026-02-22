package io.mambatech.mambasplit.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {
  private static final Set<String> PUBLIC_DOCS_PROFILES = Set.of("local", "dev", "test");
  private final JwtAuthFilter jwtAuthFilter;
  private final Environment environment;

  public SecurityConfig(JwtAuthFilter jwtAuthFilter, Environment environment) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.environment = environment;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> {
        auth.requestMatchers("/api/v1/auth/**").permitAll();
        if (isPublicDocsEnabled()) {
          auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
        }
        auth.anyRequest().authenticated();
      });

    http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private boolean isPublicDocsEnabled() {
    return Arrays.stream(environment.getActiveProfiles()).anyMatch(PUBLIC_DOCS_PROFILES::contains);
  }
}
