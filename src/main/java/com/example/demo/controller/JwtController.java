package com.example.demo.controller;

import com.example.demo.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class JwtController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/generate-token")
    public ResponseEntity<Map<String, Object>> generateToken(@RequestBody Map<String, Object> userPayload) {
        try {
            String userNo = String.valueOf(userPayload.get("id"));
            String email = (String) userPayload.get("email");
            String name = (String) userPayload.get("name");

            // JWT 토큰 생성
            String jwtToken = jwtTokenProvider.createToken(userNo, email, name);
            
            return ResponseEntity.ok(
                    Map.of(
                            "status", "success",
                            "data", Map.of("token", jwtToken)
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "status", "fail",
                            "data", Map.of("message", "토큰 생성 중 오류가 발생했습니다.")
                    )
            );
        }
    }

    @GetMapping("/decode-jwt")
    public ResponseEntity<Map<String, Object>> decodeJwtToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "success",
                            "data", userInfo
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "status", "fail",
                            "data", Map.of("message", "토큰 디코딩 중 오류가 발생했습니다.")
                    )
            );
        }
    }
}