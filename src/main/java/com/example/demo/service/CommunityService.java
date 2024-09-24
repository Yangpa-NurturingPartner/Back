package com.example.demo.service;

import com.example.demo.model.CommunityBoardVO;
import com.example.demo.model.PageRequestVO;
import com.example.demo.model.PageResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunityService {

    @Autowired
    private CommunityMapper communityMapper;

    public PageResponseVO<CommunityBoardVO> getBoardList(PageRequestVO pageRequestVO) {
        int skip = pageRequestVO.getSkip();
        int size = pageRequestVO.getSize();

        List<CommunityBoardVO> list = communityMapper.getBoardList(size, skip);
        int total = communityMapper.getTotalCount();

        return new PageResponseVO<>(list, total, pageRequestVO.getPageNo(), size);
    }
}
