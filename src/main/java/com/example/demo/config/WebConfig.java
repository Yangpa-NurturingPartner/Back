package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // API 요청에 대한 CORS 허용
                .allowedOriginPatterns("http://localhost:3000")  // 프론트엔드 주소
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)  // 인증 정보(쿠키) 포함 허용
                .allowedHeaders("*")  // 모든 헤더 허용
                .exposedHeaders("Authorization")  // 필요한 헤더를 클라이언트에 노출
                .maxAge(3600);
    }
}