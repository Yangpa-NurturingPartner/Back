package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해 CORS 적용
                .allowedOrigins("http://example.com") // 허용할 특정 도메인
                .allowedMethods("GET", "POST", "PUT", "DELETE") // 허용할 HTTP 메서드
                .allowCredentials(true) // 자격 증명 허용 (쿠키, 인증 정보)
                .allowedHeaders("*") // 모든 헤더 허용
                .maxAge(3600); // preflight 요청 캐시 시간
    }
}
