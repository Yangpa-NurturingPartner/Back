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

        // user_no 별로 child_id를 1부터 증가시키는 로직
        MemberUser memberUser = profile.getMemberUser();
        Integer maxChildId = profileRepository.findMaxChildIdByMemberUser(memberUser);
        profile.setChildId(maxChildId + 1);

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
