package com.example.demo.repository.community;
import com.example.demo.entity.CommunityBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {
}