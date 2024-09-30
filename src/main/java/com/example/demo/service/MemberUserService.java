package com.example.demo.service;

import com.example.demo.entity.MemberUser;
import com.example.demo.repository.MemberUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MemberUserService {

    private static final Logger logger = LoggerFactory.getLogger(MemberUserService.class);

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Transactional(readOnly = true)
    public Optional<MemberUser> getUserByEmail(String email) {
        logger.debug("Fetching user by email: {}", email);
        return memberUserRepository.findByUserEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<MemberUser> getUserById(Long id) {
        logger.debug("Fetching user by id: {}", id);
        return memberUserRepository.findById(id);
    }

    @Transactional
    public MemberUser createUser(String email) {
        logger.info("Creating new user with email: {}", email);
        if (memberUserRepository.existsByUserEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }
        MemberUser newUser = new MemberUser(email);
        return memberUserRepository.save(newUser);
    }

    @Transactional
    public MemberUser updateUserEmail(Long userId, String newEmail) {
        logger.info("Updating email for user id: {} to {}", userId, newEmail);
        MemberUser user = memberUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.setUserEmail(newEmail);
        return memberUserRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Deleting user with id: {}", userId);
        memberUserRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public List<MemberUser> getAllUsers() {
        logger.debug("Fetching all users");
        return memberUserRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean isEmailTaken(String email) {
        logger.debug("Checking if email is taken: {}", email);
        return memberUserRepository.existsByUserEmail(email);
    }
}