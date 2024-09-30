package com.example.demo.controller;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import com.example.demo.repository.MemberUserRepository;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.service.ProfileService;
import com.example.demo.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // 프로필 조회
    @GetMapping("/{email}")
    public ResponseEntity<?> getProfiles(@PathVariable String email, @RequestHeader("Authorization") String token) {
        try {
            // 토큰 유효성 검사
            Map<String, Object> userInfo = validateToken(token);
            if (!email.equals(userInfo.get("email"))) {
                return ResponseEntity.status(403).body("Forbidden: Invalid user.");
            }

            Optional<MemberUser> memberUser = memberUserRepository.findByUserEmail(email);
            if (memberUser.isPresent()) {
                List<Profile_child> profiles = profileService.getProfilesByUserNo(memberUser.get().getUserNo());
                return ResponseEntity.ok(profiles);
            } else {
                return ResponseEntity.status(404).body("User not found");
            }
        } catch (Exception e) {
            log.error("Error in getProfiles: ", e);
            return ResponseEntity.status(401).body("Invalid token: " + e.getMessage());
        }
    }

    // 프로필 추가
    @PostMapping("/add")
    public ResponseEntity<?> addProfile(@RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        try {
            // 토큰 유효성 검사
            Map<String, Object> userInfo = validateToken(token);
            if (userInfo == null) {
                log.error("Token validation failed");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }

            String email = (String) userInfo.get("email");
            log.info("Adding profile for user: " + email);

            Optional<MemberUser> memberUser = memberUserRepository.findByUserEmail(email);

            if (memberUser.isPresent()) {
                profileData.setJoinDate(LocalDate.now());
                profileData.setMemberUser(memberUser.get());

                // 데이터 검증 추가
                if (profileData.getName() == null || profileData.getBirthdate() == null || profileData.getSex() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incomplete profile data");
                }

                Profile_child savedProfile = profileService.saveProfile(profileData);
                return ResponseEntity.ok(savedProfile);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
            }
        } catch (Exception e) {
            log.error("Error in addProfile: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
    }

    // 프로필 수정
    @PutMapping("/{childId}")
    public ResponseEntity<?> updateProfile(@PathVariable Integer childId, @RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        try {
            // 토큰 유효성 검사
            Map<String, Object> userInfo = validateToken(token);
            Optional<MemberUser> memberUser = memberUserRepository.findByUserEmail((String) userInfo.get("email"));

            if (memberUser.isPresent()) {
                // 프로필 정보 찾기
                Optional<Profile_child> existingProfile = profileRepository.findById(childId);
                if (existingProfile.isPresent()) {
                    Profile_child profileToUpdate = existingProfile.get();
                    profileToUpdate.setName(profileData.getName());
                    profileToUpdate.setBirthdate(profileData.getBirthdate());
                    profileToUpdate.setSex(profileData.getSex());
                    profileToUpdate.setImageProfile(profileData.getImageProfile());

                    profileRepository.save(profileToUpdate);
                    return ResponseEntity.ok(profileToUpdate);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
            }
        } catch (Exception e) {
            log.error("Error in updateProfile: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
        }
    }

    // 프로필 삭제
    @DeleteMapping("/{childId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Integer childId, @RequestHeader("Authorization") String token) {
        try {
            // 토큰 유효성 검사
            Map<String, Object> userInfo = validateToken(token);
            String email = (String) userInfo.get("email");
            Optional<MemberUser> memberUser = memberUserRepository.findByUserEmail(email);

            if (memberUser.isPresent()) {
                // 프로필 정보 찾기
                Optional<Profile_child> existingProfile = profileRepository.findById(childId);
                if (existingProfile.isPresent()) {
                    // 삭제 처리
                    profileRepository.delete(existingProfile.get());
                    log.info("Profile deleted for user: " + email);
                    return ResponseEntity.ok("Profile deleted successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
            }
        } catch (Exception e) {
            log.error("Error in deleteProfile: ", e);
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
