package com.example.demo.controller;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import com.example.demo.repository.MemberUserRepository;
import com.example.demo.service.ProfileService;
import com.example.demo.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<?> addProfile(@RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        try {
            // 토큰 유효성 검사
            Map<String, Object> userInfo = validateToken(token);
            Optional<MemberUser> memberUser = memberUserRepository.findByUserEmail((String) userInfo.get("email"));

            if (memberUser.isPresent()) {
                profileData.setJoinDate(LocalDate.now());
                profileData.setMemberUser(memberUser.get());

                // 데이터 검증 추가
                if (profileData.getName() == null || profileData.getBirthdate() == null || profileData.getSex() == null) {
                    logger.warn("Incomplete profile data provided");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incomplete profile data");
                }

                Profile_child savedProfile = profileService.saveProfile(profileData);
                logger.info("Profile added for user: {}", memberUser.get().getUserEmail());
                return ResponseEntity.ok(savedProfile);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
            }
        } catch (Exception e) {
            logger.error("Error in addProfile: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
    }

    // 토큰 유효성 검사 메서드
    private Map<String, Object> validateToken(String token) {
        if (!token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization format");
        }
        String jwtToken = token.replace("Bearer ", "");
        return jwtTokenProvider.decodeToken(jwtToken);
    }
}
