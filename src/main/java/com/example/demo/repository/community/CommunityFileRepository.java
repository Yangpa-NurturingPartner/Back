package com.example.demo.repository.community;

import com.example.demo.entity.community.CommunityFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityFileRepository extends JpaRepository<CommunityFile, Long> {
}
