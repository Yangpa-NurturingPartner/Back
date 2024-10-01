package com.example.demo.controller;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import com.example.demo.exception.InvalidProfileDataException;
import com.example.demo.exception.ProfileNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.member.MemberUserRepository;
import com.example.demo.repository.ProfileRepository;
import com.example.demo.service.member.ProfileService;
import com.example.demo.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 사용자 조회
    @GetMapping("/search")
    public ResponseEntity<?> getProfiles(@RequestHeader("Authorization") String token) {
        MemberUser memberUser = getMemberUserByToken(token);
        List<Profile_child> profiles = profileService.getProfilesByUserNo(memberUser.getUserNo());
        return ResponseEntity.ok(profiles);
    }

    // 자식 프로필 등록
    @PostMapping("/add")
    public ResponseEntity<?> addProfile(@RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            MemberUser memberUser = getMemberUserByToken(token);
            validateProfileData(profileData);

            profileData.setJoinDate(LocalDate.now());
            profileData.setMemberUser(memberUser);

            profileService.saveProfile(profileData);

            response.put("status", "success");
            response.put("message", "Profile added successfully");

            return ResponseEntity.ok(response);

        } catch (UserNotFoundException | InvalidProfileDataException e) {
            response.put("status", "fail");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 자식 프로필 수정
    @PutMapping("/{childId}")
    public ResponseEntity<?> updateProfile(@PathVariable Integer childId, @RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            validateProfileData(profileData);

            Profile_child profileToUpdate = getProfileById(childId);

            profileToUpdate.setName(profileData.getName());
            profileToUpdate.setBirthdate(profileData.getBirthdate());
            profileToUpdate.setSex(profileData.getSex());
            profileToUpdate.setImageProfile(profileData.getImageProfile());

            profileRepository.save(profileToUpdate);

            response.put("status", "success");
            response.put("message", "Profile updated successfully");

            return ResponseEntity.ok(response);

        } catch (UserNotFoundException | ProfileNotFoundException | InvalidProfileDataException e) {
            response.put("status", "fail");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 자식 프로필 삭제
    @DeleteMapping("/{childId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Integer childId, @RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            Profile_child existingProfile = getProfileById(childId);

            profileRepository.delete(existingProfile);

            response.put("status", "success");
            response.put("message", "Profile deleted successfully");
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException | ProfileNotFoundException e) {
            response.put("status", "fail");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 토큰으로 유저 정보 가져오기
    private MemberUser getMemberUserByToken(String token) {
        Map<String, Object> userInfo = validateToken(token);
        String email = (String) userInfo.get("email");
        return memberUserRepository.findByUserEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    // 프로필 데이터 검증
    private void validateProfileData(Profile_child profileData) {
        if (profileData.getName() == null || profileData.getBirthdate() == null || profileData.getSex() == null) {
            throw new InvalidProfileDataException("Incomplete profile data: name, birthdate, and sex are required.");
        }
    }

    // 프로필 ID로 프로필 정보 가져오기
    private Profile_child getProfileById(Integer childId) {
        return profileRepository.findById(childId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found with ID: " + childId));
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