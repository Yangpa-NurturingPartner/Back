package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String userNo, String email, String name) {
        return Jwts.builder()
                .setSubject(userNo)
                .claim("email", email)
                .claim("name", name)
                .setIssuedAt(new Date())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Map<String, Object> decodeToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", claims.getSubject());
        userInfo.put("email", claims.get("email"));
        userInfo.put("name", claims.get("name"));

        return userInfo;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            System.out.println("Token validation error: " + e);
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Map<String, Object> userInfo = decodeToken(token);
        String email = (String) userInfo.get("email");

        User user = new User(email, "", Collections.emptyList());
        return new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities());
    }
}
