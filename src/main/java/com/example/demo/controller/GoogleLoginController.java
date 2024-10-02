package com.example.demo.controller;

import com.example.demo.entity.AuthToken;
import com.example.demo.entity.MemberUser;
import com.example.demo.repository.AuthTokenRepository;
import com.example.demo.repository.member.MemberUserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoogleLoginController {

    private final MemberUserRepository memberUserRepository;
    private final AuthTokenRepository authTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final String googleClientId;

    @Autowired
    public GoogleLoginController(MemberUserRepository memberUserRepository,
                                 AuthTokenRepository authTokenRepository,
                                 JwtTokenProvider jwtTokenProvider,
                                 @Value("${google.client.id}") String googleClientId) {
        this.memberUserRepository = memberUserRepository;
        this.authTokenRepository = authTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleClientId = googleClientId;
    }

    @PostMapping("/google-login")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> tokenData) {
        String idTokenString = tokenData.get("token");

        try {
            GoogleIdToken idToken = verifyGoogleToken(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "status", "fail",
                                "message", "유효하지 않은 ID 토큰입니다."
                        ));
            }

            // 사용자 정보 추출
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            MemberUser memberUser = findOrCreateMemberUser(email);

            // 토큰 생성 및 저장
            String jwtToken = jwtTokenProvider.createToken(memberUser.getUserNo().toString(), email, name);
            saveAuthToken(memberUser, jwtToken);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "로그인이 성공적으로 완료되었습니다.",
                    "data", Map.of(
                            "token", jwtToken,
                            "email", email,
                            "name", name
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "토큰 유효성 검사 중 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    // 구글 credential 토큰이 올바른지 확인
    private GoogleIdToken verifyGoogleToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        return verifier.verify(idTokenString);
    }

    // 사용자 이메일을 이용하여 새로운 사람 저장 또는 찾기
    private MemberUser findOrCreateMemberUser(String email) {
        return memberUserRepository.findByUserEmail(email).orElseGet(() -> {
            MemberUser newUser = new MemberUser(email);
            return memberUserRepository.save(newUser);
        });
    }

    // 토큰 저장
    private void saveAuthToken(MemberUser memberUser, String jwtToken) {
        AuthToken authToken = new AuthToken();
        authToken.setMemberUser(memberUser);
        authToken.setAccessToken(jwtToken);
        authToken.setIat(new Date());
        authTokenRepository.save(authToken);
    }
}