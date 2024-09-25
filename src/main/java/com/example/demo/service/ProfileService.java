package com.example.demo.service;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import com.example.demo.repository.MemberUserRepository;
import com.example.demo.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Transactional
    public Profile_child saveProfile(Profile_child profile) {
        logger.info("Saving profile for user: {}", profile.getMemberUser().getUserNo());
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<Profile_child> getProfilesByUserNo(Long userNo) {
        logger.info("Fetching profiles for user: {}", userNo);
        MemberUser memberUser = memberUserRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userNo));
        return profileRepository.findByMemberUser(memberUser);
    }

    @Transactional(readOnly = true)
    public Optional<Profile_child> getProfileByChildId(Long childId) {
        logger.info("Fetching profile with child id: {}", childId);
        return profileRepository.findByChildId(childId);
    }

    @Transactional
    public Profile_child updateProfile(Long childId, Profile_child updatedProfile) {
        logger.info("Updating profile with child id: {}", childId);
        Profile_child existingProfile = profileRepository.findByChildId(childId)
                .orElseThrow(() -> new RuntimeException("Profile not found with id: " + childId));

        existingProfile.setName(updatedProfile.getName());
        existingProfile.setSex(updatedProfile.getSex());
        existingProfile.setBirthdate(updatedProfile.getBirthdate());
        existingProfile.setImageProfile(updatedProfile.getImageProfile());

        return profileRepository.save(existingProfile);
    }

    @Transactional
    public void deleteProfile(Long childId) {
        logger.info("Deleting profile with child id: {}", childId);
        profileRepository.deleteById(childId);
    }

    @Transactional(readOnly = true)
    public List<Profile_child> getProfilesByBirthdate(LocalDate birthdate) {
        logger.info("Fetching profiles with birthdate: {}", birthdate);
        return profileRepository.findByBirthdate(birthdate);
    }

    @Transactional(readOnly = true)
    public List<Profile_child> getProfilesByName(String name) {
        logger.info("Fetching profiles with name containing: {}", name);
        return profileRepository.findByNameContaining(name);
    }

    @Transactional(readOnly = true)
    public long countProfilesByUser(Long userNo) {
        logger.info("Counting profiles for user: {}", userNo);
        MemberUser memberUser = memberUserRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userNo));
        return profileRepository.countByMemberUser(memberUser);
    }
}