package com.example.demo.service;

import com.example.demo.entity.AuthToken;
import com.example.demo.entity.MemberUser;
import com.example.demo.repository.AuthTokenRepository;
import com.example.demo.repository.MemberUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenService.class);

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private MemberUserRepository memberUserRepository;

    @Transactional
    public AuthToken saveAuthToken(Long userNo, String accessToken, Date iat) {
        logger.info("Saving auth token for user: {}", userNo);
        MemberUser memberUser = memberUserRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userNo));
        AuthToken authToken = new AuthToken(memberUser, accessToken, iat);
        return authTokenRepository.save(authToken);
    }

    @Transactional(readOnly = true)
    public Optional<AuthToken> findAuthTokenByUserNo(Long userNo) {
        logger.debug("Finding auth token for user: {}", userNo);
        return authTokenRepository.findById(userNo);
    }

    @Transactional
    public void deleteAuthToken(Long tokenId) {
        logger.info("Deleting auth token with id: {}", tokenId);
        authTokenRepository.deleteById(tokenId);
    }

    @Transactional(readOnly = true)
    public List<AuthToken> findAllAuthTokensByUserNo(Long userNo) {
        logger.debug("Finding all auth tokens for user: {}", userNo);
        MemberUser memberUser = memberUserRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userNo));
        return authTokenRepository.findByMemberUser(memberUser);
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        logger.debug("Checking if token is valid: {}", token);
        return authTokenRepository.existsByAccessToken(token);
    }
}