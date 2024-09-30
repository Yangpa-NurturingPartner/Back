package com.example.demo.service.member;

import com.example.demo.entity.MemberUser;
import com.example.demo.entity.Profile_child;
import com.example.demo.repository.member.MemberUserRepository;
import com.example.demo.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}