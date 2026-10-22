package com.school.counseling.config;

import com.school.counseling.module.auth.service.JwtAuthenticationFilter;
import com.school.counseling.module.auth.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // thuant toan băm 1 chieu = chuoi ngẫu nhiên
    } // trộm đc db nhg ko thể dịch ngược

    @Bean
    // xác thực tk
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(); // DaoAuthen... trọng tài xác thực tk
                                                                                  // khibaamws lgoin
        authProvider.setUserDetailsService(userDetailsService);// tim thong tin tk
        authProvider.setPasswordEncoder(passwordEncoder());// xac thuc mk đưa giải mã bcrupt vào
        return authProvider;
    }

    @Bean
    // AuthenticationManager tổng quản lý điều phối quá trình đăngn nhập
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    // bộ lọc chính bảo vê ngóc ngách hệ thôgns
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/ws/**"))
                // cho phép mọi người truy cập
                .authorizeHttpRequests(auth -> auth
                        // Static resources
                        .requestMatchers(
                                "/", "/home", "/about", "/explore", "/admission", "/research",
                                "/ai-chat", "/api/ai/**", "/api/v1/ai/**",
                                "/academic/**", "/api/academic/**",
                                "/grades", "/timetable", "/curriculum",
                                "/faqs", "/faq", "/api/v1/faqs/**",
                                "/feed/official/**", "/feed/forum/**", "/api/v1/forum/**",
                                "/auth/**", "/api/v1/auth/**",
                                "/error",
                                "/css/**", "/js/**", "/vendor/**", "/images/**", "/uploads/**", "/webjars/**", "/favicon.ico")
                        .permitAll()

                        // Admin & Staff boundaries
                        .requestMatchers("/admin/**", "/api/admin/**", "/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**", "/api/v1/staff/**", "/api/v1/internal-messages/**").hasAnyRole("STAFF", "ADMIN")
                        // All other requests require authentication
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/home", false)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                        .logoutSuccessUrl("/?logout=true")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error"))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

// CSRF croos site request forgery : kẻ xấu lừa trình duyệt gửi yêu cầu trộm khi
// b đang log in
// link /api hay websocket /ws thì bỏ qua csrf vì api dùng token jwwt ko dùng
// session côkie