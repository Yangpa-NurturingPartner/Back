package com.example.demo.service.community;

import com.example.demo.model.community.CommunityBoardVO;
import com.example.demo.model.community.CommunityFileVO;
import com.example.demo.model.PageRequestVO;
import com.example.demo.model.PageResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class CommunityService {

    @Autowired
    private CommunityMapper communityMapper;

    // 게시물 목록 조회
    public PageResponseVO<CommunityBoardVO> getBoardList(PageRequestVO pageRequestVO) {
        int skip = pageRequestVO.getSkip();
        int size = pageRequestVO.getSize();

        List<CommunityBoardVO> list = communityMapper.getBoardList(size, skip);
        int total = communityMapper.getTotalCount();

        return new PageResponseVO<>(list, total, pageRequestVO.getPageNo(), size);
    }

    // 게시물 생성 및 파일 저장
    public void createBoard(CommunityBoardVO boardVO) throws IOException {
        // 게시물 저장
        communityMapper.insertBoard(boardVO);

        // board_no 값이 정상적으로 할당되었는지 확인하는 로그 추가
        if (boardVO.getBoard_no() == null) {
            throw new RuntimeException("Board creation failed: board_no is null");
        }

        // 파일 저장 처리 (파일이 있는 경우)
        MultipartFile file = boardVO.getFile();
        if (file != null && !file.isEmpty()) {
            // 파일 정보를 CommunityFileVO로 변환하여 저장
            CommunityFileVO fileVO = new CommunityFileVO();
            fileVO.setBoard_no(boardVO.getBoard_no());  // 게시물 번호 참조
            fileVO.setName(file.getOriginalFilename());
            fileVO.setAttached_img(file.getBytes());

            // 파일 저장 로그 추가
            System.out.println("Saving file with board_no: " + boardVO.getBoard_no());
            communityMapper.insertFile(fileVO);
        }
    }

    // 가장 최근에 삽입된 board_no를 가져오는 메서드
    public Long getLastInsertedBoardNo() {
        return communityMapper.getLastInsertedBoardNo();
    }

}