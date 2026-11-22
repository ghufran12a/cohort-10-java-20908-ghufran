package com.contactapp.contactmanager.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The security rules of the whole application: "who may call which url".
 *
 * Every request passes through a chain of filters BEFORE it reaches a controller:
 *   request -> [CORS] -> [check the JWT in the Authorization header] -> [url rules] -> controller
 * If the token is missing/invalid/expired, the request is stopped with 401 and the controller never runs.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protects cookie-based sessions. We use a token in a header and no session, so turn it off.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // uses the corsConfigurationSource bean below
                // STATELESS = the server remembers nobody; the token must come with every request
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll() // register + login are public
                        .anyRequest().authenticated())                          // everything else needs a valid token
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        // no / bad token -> 401 as JSON (not an HTML page)
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJson(response, 401, "Unauthorized", "Please log in again"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJson(response, 403, "Forbidden", "You are not allowed to do this")));
        return http.build();
    }

    // BCrypt: a one-way hash with a built-in random "salt". Same password -> different hash every time,
    // and nobody (not even us) can turn the hash back into the password.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS: the browser blocks a page on localhost:5173 (React) from calling localhost:8080 (this API)
    // unless the API says "that origin is allowed".
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private static void writeJson(jakarta.servlet.http.HttpServletResponse response, int status,
                                  String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error
                + "\",\"message\":\"" + message + "\"}");
    }
}
