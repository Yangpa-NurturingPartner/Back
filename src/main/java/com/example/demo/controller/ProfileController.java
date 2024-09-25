package com.example.demo.controller;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import com.example.demo.repository.MemberUserRepository;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 이메일로 프로필 조회
     */
    @GetMapping("/{email}")
    public ResponseEntity<?> getProfileByEmail(@PathVariable String email, @RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰 검증
            Map<String, Object> userInfo = validateToken(token);

            // 사용자 조회
            Optional<MemberUser> user = memberUserRepository.findByUserEmail(email);
            if (user.isPresent()) {
                // 사용자에 해당하는 프로필 가져오기
                List<Profile_child> profiles = profileRepository.findByMemberUser(user.get());
                logger.info("Retrieved {} profiles for user: {}", profiles.size(), email);
                return ResponseEntity.ok(profiles);
            } else {
                logger.warn("User not found: {}", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
        } catch (Exception e) {
            logger.error("Error in getProfileByEmail: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
    }

    /**
     * 프로필 추가
     */
    @PostMapping
    public ResponseEntity<?> addProfile(@RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        try {
            // JWT 토큰 검증
            Map<String, Object> userInfo = validateToken(token);

            // 이메일로 사용자 조회
            Optional<MemberUser> memberUser = memberUserRepository.findByUserEmail(profileData.getMemberUser().getUserEmail());
            if (memberUser.isPresent()) {
                profileData.setJoinDate(LocalDate.now());
                profileData.setMemberUser(memberUser.get());

                // 프로필 저장
                Profile_child savedProfile = profileService.saveProfile(profileData);
                logger.info("Profile added for user: {}", memberUser.get().getUserEmail());
                return ResponseEntity.ok(savedProfile);
            } else {
                logger.warn("User not found for profile creation: {}", profileData.getMemberUser().getUserEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
            }
        } catch (Exception e) {
            logger.error("Error in addProfile: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
    }

    /**
     * 토큰을 검증하고 유효성을 체크하는 메서드
     */
    private Map<String, Object> validateToken(String token) {
        // Authorization 헤더에서 'Bearer ' 접두사가 있는지 확인
        if (!token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization format");
        }
        String jwtToken = token.replace("Bearer ", "");
        return jwtTokenProvider.decodeToken(jwtToken);
    }
}