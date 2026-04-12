package by.bsuir.springbootproject.config;

import by.bsuir.springbootproject.constants.RoutePaths;
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

@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                RoutePaths.ROOT,
                                RoutePaths.HOME,
                                RoutePaths.ERROR,
                                RoutePaths.ERROR + "/**",
                                RoutePaths.AUTH + "/**",
                                "/oauth2/**",
                                "/style/**",
                                "/script/**",
                                "/assets/**",
                                RoutePaths.CATALOG + "/**",
                                RoutePaths.COMICS + "/**",
                                RoutePaths.READ + "/**",
                                RoutePaths.PASSWORD_RESET + "/**"
                        ).permitAll()
                        .requestMatchers(
                                RoutePaths.PROFILE + "/**",
                                RoutePaths.COLLECTIONS + "/**",
                                RoutePaths.NOTIFICATIONS + "/**"
                        ).authenticated()
                        .requestMatchers(RoutePaths.ADMIN + "/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage(RoutePaths.LOGIN)
                        .loginProcessingUrl(RoutePaths.LOGIN)
                        .defaultSuccessUrl(RoutePaths.HOME, true)
                        .failureUrl(RoutePaths.LOGIN + "?error=true")
                        .usernameParameter("login")
                        .passwordParameter("password")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage(RoutePaths.LOGIN)
                        .successHandler(googleOAuth2SuccessHandler)
                        .failureUrl(RoutePaths.LOGIN + "?oauthError=true")
                )
                .logout(logout -> logout
                        .logoutUrl(RoutePaths.LOGOUT)
                        .logoutSuccessUrl(RoutePaths.LOGOUT_SUCCESS)
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
                            response.sendRedirect(request.getContextPath() + RoutePaths.LOGIN);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestedWith = request.getHeader("X-Requested-With");
                            if ("XMLHttpRequest".equals(requestedWith)) {
                                response.setStatus(403);
                                return;
                            }
                            response.sendRedirect(request.getContextPath() + RoutePaths.LOGIN);
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

