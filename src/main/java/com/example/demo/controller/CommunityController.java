package com.example.demo.controller;

import com.example.demo.model.CommunityBoardVO;
import com.example.demo.model.PageRequestVO;
import com.example.demo.model.PageResponseVO;
import com.example.demo.service.CommunityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @GetMapping("/boards")
    public PageResponseVO<CommunityBoardVO> getItems(PageRequestVO pageRequestVO) {
        return communityService.getBoardList(pageRequestVO);
    }
}
