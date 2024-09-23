package com.example.demo.service;

import com.example.demo.model.CommunityBoardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityMapper {

    @Select("SELECT * FROM community_board ORDER BY board_no DESC LIMIT #{size} OFFSET #{skip}")
    List<CommunityBoardVO> getBoardList(int size, int skip);

    @Select("SELECT COUNT(*) FROM community_board")
    int getTotalCount();
}
