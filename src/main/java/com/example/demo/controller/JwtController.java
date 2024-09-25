package com.example.demo.controller;

import com.example.demo.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class JwtController {

    // 환경 변수 또는 application.properties 파일에서 JWT 시크릿 키를 가져옵니다.
    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostMapping("/generate-token")
    public Map<String, String> generateToken(@RequestBody Map<String, Object> userPayload) {
        // userPayload에서 필요한 데이터를 추출합니다.
        String id = String.valueOf(userPayload.get("id"));
        String email = (String) userPayload.get("email");
        String name = (String) userPayload.get("name");
        String createdAt = (String) userPayload.get("createdAt");

        // JWT 토큰 생성에 사용할 키를 HS256 알고리즘을 사용하여 생성합니다.
        Key hmacKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());

        // 현재 시간
        Date now = new Date();

        // 토큰 생성
        String jwtToken = Jwts.builder()
                .setSubject(id) // 사용자 ID를 subject로 설정
                .claim("email", email) // 이메일과 이름을 claim에 추가
                .claim("name", name)
                .claim("createdAt", createdAt)
                .setIssuedAt(now) // 발급 시간 설정
                //.setExpiration(new Date(now.getTime() + 3600000)) // 1시간 뒤 만료
                .signWith(hmacKey, SignatureAlgorithm.HS256) // 서명에 사용할 알고리즘과 키 설정
                .compact();

        // 결과를 Map으로 반환
        Map<String, String> response = new HashMap<>();
        response.put("token", jwtToken);

        return response;
    }


    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/decode-jwt")
    public ResponseEntity<Map<String, Object>> decodeJwtToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);

        return ResponseEntity.ok(userInfo);
    }
}