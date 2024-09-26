package com.example.demo.service.community;

import com.example.demo.model.community.CommunityBoardVO;
import com.example.demo.model.community.CommunityCommentVO;
import com.example.demo.model.community.CommunityFileVO;
import com.example.demo.model.PageRequestVO;
import com.example.demo.model.PageResponseVO;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommunityService {

    @Autowired
    private CommunityMapper communityMapper;

    // 게시물 목록 조회 (파일 정보 포함)
    public PageResponseVO<CommunityBoardVO> getBoardList(PageRequestVO pageRequestVO) {
        int skip = pageRequestVO.getSkip();
        int size = pageRequestVO.getSize();

        List<CommunityBoardVO> list = communityMapper.getBoardListWithFiles(size, skip, pageRequestVO.getBoardCode());
        int total = communityMapper.getTotalCount(pageRequestVO.getBoardCode());

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
            CommunityFileVO fileVO = new CommunityFileVO();
            fileVO.setBoard_no(Long.valueOf(boardVO.getBoard_no()));  // 게시물 번호 참조
            fileVO.setName(file.getOriginalFilename());
            fileVO.setAttached_img(file.getBytes());
            communityMapper.insertFile(fileVO);
        }
    }

    // 가장 최근에 삽입된 board_no를 가져오는 메서드
    public Long getLastInsertedBoardNo() {
        return communityMapper.getLastInsertedBoardNo();
    }

    // board_no를 기준으로 댓글을 가져오는 메서드
    public List<CommunityCommentVO> getCommentsByBoardNo(Long boardNo) {
        return communityMapper.getCommentsByBoardNo(boardNo);
    }

    // 특정 board_no로 게시물을 가져오는 새로운 메서드
    public CommunityBoardVO getBoardByNo(Long boardNo) {
        return communityMapper.getBoardByNo(boardNo);
    }

    // CommunityService에 파일 정보를 가져오는 메서드
    public List<CommunityFileVO> getFilesByBoardNo(Long boardNo) {
        List<CommunityFileVO> files = communityMapper.getFilesByBoardNo(boardNo);
        List<CommunityFileVO> filesWithEncodedImages = new ArrayList<>();

        for (CommunityFileVO file : files) {
            String base64Image = Base64.encodeBase64String(file.getAttached_img());
            String imageUrl = "data:image/png;base64," + base64Image;
            file.setName(imageUrl);
            filesWithEncodedImages.add(file);
        }

        return filesWithEncodedImages;
    }

    // 새로운 comment_no를 생성하는 메서드
    public Long getNewCommentNo() {
        Long lastCommentNo = communityMapper.getLastInsertedCommentNo();
        return (lastCommentNo != null) ? lastCommentNo + 1 : 1;
    }

    // 댓글 저장 메서드
    public void addComment(CommunityCommentVO commentVO) {
        communityMapper.insertComment(commentVO);
    }

    // board의 count 값을 +1로 증가시키는 메서드
    public void incrementBoardCount(Long boardNo) {
        communityMapper.updateBoardCount(boardNo);
    }

    public List<CommunityBoardVO> getBoardsByNos(List<Integer> boardNos) {
        return communityMapper.getBoardsWithFilesByNos(boardNos);
    }
}