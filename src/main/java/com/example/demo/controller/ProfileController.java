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
        try {
            MemberUser memberUser = getMemberUserByToken(token);
            List<Profile_child> profiles = profileService.getProfilesByUserNo(memberUser.getUserNo());
            return ResponseEntity.ok(Map.of("status", "success", "data", profiles));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "사용자를 찾을 수 없습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "예상치 못한 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    // 자식 프로필 등록
    @PostMapping("/add")
    public ResponseEntity<?> addProfile(@RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        try {
            MemberUser memberUser = getMemberUserByToken(token);
            validateProfileData(profileData);

            profileData.setJoinDate(LocalDate.now());
            profileData.setMemberUser(memberUser);

            profileService.saveProfile(profileData);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "프로필이 성공적으로 추가되었습니다."
            ));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "사용자를 찾을 수 없습니다."
            ));
        } catch (InvalidProfileDataException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "유효하지 않은 프로필 데이터입니다: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "예상치 못한 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    // 자식 프로필 수정
    @PutMapping("/{childId}")
    public ResponseEntity<?> updateProfile(@PathVariable Integer childId, @RequestBody Profile_child profileData, @RequestHeader("Authorization") String token) {
        try {
            validateProfileData(profileData);

            Profile_child profileToUpdate = getProfileById(childId);

            profileToUpdate.setName(profileData.getName());
            profileToUpdate.setBirthdate(profileData.getBirthdate());
            profileToUpdate.setSex(profileData.getSex());
            profileToUpdate.setImageProfile(profileData.getImageProfile());

            profileRepository.save(profileToUpdate);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "프로필 수정 완료 했습니다."
            ));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "사용자를 찾을 수 없습니다."
            ));
        } catch (ProfileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "프로필을 찾을 수 없습니다."
            ));
        } catch (InvalidProfileDataException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "유효하지 않은 프로필 데이터입니다: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "예상치 못한 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    // 자식 프로필 삭제
    @DeleteMapping("/{childId}")
    public ResponseEntity<?> deleteProfile(@PathVariable Integer childId, @RequestHeader("Authorization") String token) {
        try {
            Profile_child existingProfile = getProfileById(childId);

            profileRepository.delete(existingProfile);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", "프로필 삭제에 성공 했습니다."
            ));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "사용자를 찾을 수 없습니다."
            ));
        } catch (ProfileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", "fail",
                    "message", "프로필을 찾을 수 없습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "예상치 못한 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    // 토큰으로 유저 정보 가져오기
    private MemberUser getMemberUserByToken(String token) {
        Map<String, Object> userInfo = validateToken(token);
        String email = (String) userInfo.get("email");
        return memberUserRepository.findByUserEmail(email)
                .orElseThrow(() -> new UserNotFoundException("이메일로 사용자를 찾을 수 없습니다: " + email));
    }

    // 프로필 데이터 검증
    private void validateProfileData(Profile_child profileData) {
        if (profileData.getName() == null || profileData.getBirthdate() == null || profileData.getSex() == null) {
            throw new InvalidProfileDataException("프로필 데이터가 불완전합니다: 이름, 생년월일 및 성별이 필요합니다.");
        }
    }

    // 프로필 ID로 프로필 정보 가져오기
    private Profile_child getProfileById(Integer childId) {
        return profileRepository.findById(childId)
                .orElseThrow(() -> new ProfileNotFoundException("ID로 프로필을 찾을 수 없습니다: " + childId));
    }

    // 토큰 유효성 검사 메서드
    private Map<String, Object> validateToken(String token) {
        if (!token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("잘못된 Authorization 형식입니다.");
        }
        String jwtToken = token.replace("Bearer ", "");
        return jwtTokenProvider.decodeToken(jwtToken);
    }
}