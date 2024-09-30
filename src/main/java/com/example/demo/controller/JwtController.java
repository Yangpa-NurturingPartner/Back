package com.example.demo.controller;

import com.example.demo.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class JwtController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/generate-token")
    public Map<String, String> generateToken(@RequestBody Map<String, Object> userPayload) {
        String userNo = String.valueOf(userPayload.get("id"));
        String email = (String) userPayload.get("email");
        String name = (String) userPayload.get("name");

        // JWT 토큰 생성
        String jwtToken = jwtTokenProvider.createToken(userNo, email, name);

        return Map.of("token", jwtToken);
    }

    @GetMapping("/decode-jwt")
    public ResponseEntity<Map<String, Object>> decodeJwtToken(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);

        return ResponseEntity.ok(userInfo);
    }
}
