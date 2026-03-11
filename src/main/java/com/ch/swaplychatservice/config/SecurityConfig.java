package com.ch.swaplychatservice.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable()) // ✅ Gateway에서 처리하므로 중복 방지를 위해 disable
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // ✅ 모든 경로 개방 (인증은 헤더로 판단)
                )
                // 401을 강제로 응답하는 EntryPoint 제거 또는 로깅만 수행
                .exceptionHandling(ex -> ex.disable())
                .formLogin(f -> f.disable())
                .httpBasic(h -> h.disable());

        return http.build();
    }
}