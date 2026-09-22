package ru.cloudStorage.CloudStorage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import ru.cloudStorage.CloudStorage.dto.ErrorMessage;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // Вредоносные формы будут игнорироваться (json), вредоносные json отклоняются CORS защитой -> csrf.disable()
                .csrf(csrf -> csrf.disable()
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/",
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/static/**",
                                        "/assets/**",
                                        "/config.js",
                                        "/login",
                                        "/index.html",
                                        "/registration",
                                        "/api/auth/**",
                                        "/error").permitAll()
                                .anyRequest().authenticated())
                        .exceptionHandling(exception -> exception
                                .authenticationEntryPoint((request,
                                                           response,
                                                           authException) -> {
                                    String requestURI = request.getRequestURI();

                                    if (requestURI.startsWith("/api") ||
                                        requestURI.contains("v3/api-docs") ||
                                        requestURI.contains("swagger")) {
                                        response.setContentType("application/json;charset=UTF-8");
                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                        ErrorMessage errorMessage = new ErrorMessage("User is not authorized");
                                        ObjectMapper mapper = new ObjectMapper();
                                        String jsonResponse = mapper.writeValueAsString(errorMessage);
                                        response.getWriter().write(jsonResponse);
                                    } else {
                                        response.sendRedirect(request.getContextPath() + "/login");
                                    }
                                })
                        )
                        .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                                .expiredSessionStrategy(event ->
                                        event.getResponse().setStatus(HttpStatus.UNAUTHORIZED.value())))
                        .logout(logout -> logout
                                .logoutUrl("/api/auth/sign-out")
                                .invalidateHttpSession(true)
                                .deleteCookies("CLOUD_STORAGE_SESSION_ID")
                                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))));
        return http.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
