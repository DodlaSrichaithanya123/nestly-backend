package com.nestly.server.config;

import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nestly.server.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserRepository userRepository, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userRepository = userRepository;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> {
                }); // optional but helps for testing

        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ✅ Public endpoints
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/test",
                                "/api/auth/forgot-password")
                        .permitAll()
                        .requestMatchers("/api/rooms", "/api/rooms/featured", "/api/rooms/{id}").permitAll()

                        // ✅ Admin-only endpoints
                        .requestMatchers("/api/rooms/upload", "/api/rooms/delete/**").hasRole("ADMIN")
                        .requestMatchers("/images/**").permitAll()

                        // ✅ Booking endpoints
                        // .requestMatchers("/api/bookings/create").hasRole("USER")
                        .requestMatchers("/api/bookings/create").permitAll()
                        .requestMatchers("/api/bookings/**").hasAnyRole("USER", "ADMIN")
                        // .requestMatchers("/api/paypal/**").permitAll() // Allow PayPal endpoints
                        .requestMatchers("/api/paypal/**").permitAll()
                        // .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loggingFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ CORS Configuration
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List
                .of(System.getenv("FRONTEND_URL") != null ? System.getenv("FRONTEND_URL") : "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ✅ Optional Logging Filter
    @Bean
    public OncePerRequestFilter loggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws IOException, ServletException {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                if (auth != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        String authJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(auth);
                        System.out.println("\n===============================");
                        System.out.println("🔎 Request URL: " + request.getRequestURI());
                        System.out.println("🔎 Authenticated User: " + auth.getName());
                        System.out.println("🔎 Authorities: " + auth.getAuthorities());
                        System.out.println("🔎 Full Authentication JSON:\n" + authJson);

                        if ((request.getRequestURI().startsWith("/api/rooms/upload") ||
                                request.getRequestURI().startsWith("/api/rooms/delete")) &&
                                auth.getAuthorities().stream()
                                        .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                            System.out.println("⚠️ ALERT: Non-admin user tried to access ADMIN endpoint!");
                        }

                        System.out.println("===============================\n");
                    } catch (Exception e) {
                        System.out.println("⚠️ Failed to log auth object: " + e.getMessage());
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    // ✅ UserDetailsService for AuthenticationManager
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
