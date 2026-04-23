package by.bsuir.springbootproject.config;

import by.bsuir.springbootproject.security.AuthLoginValidationFilter;
import by.bsuir.springbootproject.security.GoogleOAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    private final AuthLoginValidationFilter authLoginValidationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(authLoginValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home",
                                "/error",
                                "/error/**",
                                "/auth/**",
                                "/oauth2/**",
                                "/style/**",
                                "/script/**",
                                "/assets/**",
                                "/catalog/**",
                                "/comics/**",
                                "/read/**",
                                "/reset/**"
                        ).permitAll()
                        .requestMatchers(
                                "/profile/**",
                                "/collections/**",
                                "/notifications/**",
                                "/comics/*/chapters/new",
                                "/comics/*/chapters/options",
                                "/translations/*/preview"
                        ).authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("login")
                        .passwordParameter("password")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/auth/login")
                        .successHandler(googleOAuth2SuccessHandler)
                        .failureUrl("/auth/login?oauthError=true")
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/logout-success")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestedWith = request.getHeader("X-Requested-With");
                            if ("XMLHttpRequest".equals(requestedWith)) {
                                response.setStatus(401);
                                return;
                            }
                            response.sendRedirect(request.getContextPath() + "/auth/login");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestedWith = request.getHeader("X-Requested-With");
                            if ("XMLHttpRequest".equals(requestedWith)) {
                                response.setStatus(403);
                                return;
                            }
                            response.sendRedirect(request.getContextPath() + "/auth/login");
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}