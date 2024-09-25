package com.example.demo.controller;

import com.example.demo.entity.AuthToken;
import com.example.demo.entity.MemberUser;
import com.example.demo.repository.AuthTokenRepository;
import com.example.demo.repository.MemberUserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoogleLoginController {

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;  // auth_token 테이블 저장을 위한 repository

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${jwt.secret}")
    private String jwtSecret;

    // JWT 토큰 생성 메서드
    private String createToken(String userNo, String email, String name) {
        Key hmacKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
        Date now = new Date();

        return Jwts.builder()
                .setSubject(userNo)
                .claim("email", email)
                .claim("name", name)
                .setIssuedAt(now)
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> tokenData) {
        String idTokenString = tokenData.get("token");

        try {
            // Google ID 토큰 검증
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
            }

            // Google ID 토큰의 페이로드에서 사용자 정보 추출
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 사용자 데이터베이스 저장 또는 조회
            MemberUser memberUser = memberUserRepository.findByUserEmail(email).orElseGet(() -> {
                MemberUser newUser = new MemberUser(email);
                return memberUserRepository.save(newUser);
            });

            // JWT 토큰 생성
            String jwtToken = createToken(String.valueOf(memberUser.getUserNo()), email, name);
            System.out.println("Generated JWT Token: " + jwtToken);

            // auth_token 테이블에 JWT 토큰 저장
            AuthToken authToken = new AuthToken();
            authToken.setMemberUser(memberUser);
            authToken.setAccessToken(jwtToken);
            authToken.setIat(new Date());
            authTokenRepository.save(authToken);  // 저장

            // 클라이언트로 JWT와 사용자 정보 반환
            return ResponseEntity.ok(Map.of(
                    "token", jwtToken,
                    "email", email,
                    "name", name
            ));

        } catch (Exception e) {
            System.err.println("Error during token validation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during token validation: " + e.getMessage());
        }
    }

    // 토큰 디코딩 및 정보 추출
    @GetMapping("/decode-google-token")
    public ResponseEntity<Map<String, Object>> decodeGoogleToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);

        return ResponseEntity.ok(userInfo);
    }
}