package org.example.homeandgarden.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.security.service.UserDetailsServiceImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomLogoutHandler customLogoutHandler;
    private final AccessDeniedHandler customAccessDeniedHandler;
    private final AuthenticationEntryPoint customAuthenticationEntryPoint;
    private final ObjectMapper objectMapper;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(STATELESS));

        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers(HttpMethod.GET, "/products/status", "/products/top", "/products/pending", "/products/profit").authenticated()
                .requestMatchers(HttpMethod.GET, "/categories", "/categories/*/products", "/products/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register","/auth/login", "/auth/token" , "/auth/forgot-password", "/auth/reset-password").permitAll()
                .requestMatchers("/manage/**", "/swagger-ui.html", "/swagger-ui/**", "/api/v1/auth/**", "/v3/api-docs/**").permitAll()
                .anyRequest()
                .authenticated());

//        http.anonymous(AbstractHttpConfigurer::disable);//To disable anonymous authentication in Spring Security

        http.exceptionHandling(exceptionHandling -> {
            exceptionHandling
                    .authenticationEntryPoint(customAuthenticationEntryPoint);
            exceptionHandling
                    .accessDeniedHandler(customAccessDeniedHandler);
        })
                .formLogin(withDefaults())
                .httpBasic(withDefaults());

        http.logout(logout ->
                logout.logoutUrl("/auth/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            MessageResponse messageResponse = new MessageResponse(
                                    "Logout successful.");
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write(objectMapper.writeValueAsString(messageResponse));
                            SecurityContextHolder.clearContext();
                        }));

        http.userDetailsService(userDetailsService);
        http.addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

}