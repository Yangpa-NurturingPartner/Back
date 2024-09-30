package com.example.demo.controller;

import com.example.demo.entity.AuthToken;
import com.example.demo.entity.MemberUser;
import com.example.demo.repository.AuthTokenRepository;
import com.example.demo.repository.member.MemberUserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoogleLoginController {

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Value("${google.client.id}")
    private String googleClientId;

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> tokenData) {
        String idTokenString = tokenData.get("token");

        try {
            // Google ID Token 검증 로직
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
            }

            // 토큰 검증 후 사용자 정보 가져오기
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 사용자 정보 저장 또는 검색
            MemberUser memberUser = memberUserRepository.findByUserEmail(email).orElseGet(() -> {
                MemberUser newUser = new MemberUser(email);
                return memberUserRepository.save(newUser);
            });

            // 로그 추가: 사용자 정보와 토큰 요청 정보 출력
            System.out.println("User email: " + email);
            System.out.println("User name: " + name);
            System.out.println("User ID: " + memberUser.getUserNo());

            // JwtController의 토큰 생성 API 호출
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://localhost:8080/api/generate-token";
            Map<String, Object> requestBody = Map.of(
                    "id", memberUser.getUserNo(),
                    "email", email,
                    "name", name
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null || !response.getBody().containsKey("token")) {
                System.err.println("Failed to generate JWT token: " + response);
                throw new RuntimeException("Failed to generate JWT token");
            }

            // 응답에서 JWT 토큰 추출
            String jwtToken = (String) response.getBody().get("token");

            // AuthToken 엔티티에 토큰 저장
            AuthToken authToken = new AuthToken();
            authToken.setMemberUser(memberUser);
            authToken.setAccessToken(jwtToken);
            authToken.setIat(new Date());
            authTokenRepository.save(authToken);

            return ResponseEntity.ok(Map.of(
                    "token", jwtToken,
                    "email", email,
                    "name", name
            ));

        } catch (Exception e) {
            System.err.println("Error during token validation: " + e.getMessage());
            e.printStackTrace();  // 전체 스택 트레이스 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error during token validation: " + e.getMessage()));
        }
    }
}