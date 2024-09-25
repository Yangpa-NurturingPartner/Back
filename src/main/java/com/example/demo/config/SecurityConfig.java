package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.disable())  // 필요 시 CORS 설정
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/google-login", "/api/user-info").permitAll()  // 로그인 API는 인증 없이 접근 가능
                        .requestMatchers("/api/profile/**").authenticated()  // 프로필 API는 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()  // 나머지 요청은 인증 필요
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")  // Google 공개 키로 JWT 검증
                        )
                )
                .build();
    }
}