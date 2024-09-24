package com.example.demo.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PageResponseVO<E> {

    private int pageNo;//페이지번호
    private int size;  //페이지의 건수
    private int total; //전체건수

    //시작 페이지 번호
    private int start;
    //끝 페이지 번호
    private int end;

    //이전 페이지의 존재 여부
    private boolean prev;
    //다음 페이지의 존재 여부
    private boolean next;

    private List<E> list;

    @Builder
    public PageResponseVO(List<E> list, int total, int pageNo, int size){

        this.pageNo = pageNo;
        this.size = size;

        this.total = total;
        this.list = list;

        this.end =   (int)(Math.ceil(this.pageNo / 10.0 )) * 10;

        this.start = this.end - 9;

        int last =  (int)(Math.ceil((total/(double)size)));

        this.end =  end > last ? last: end;

        this.prev = this.start > 1;

        this.next =  total > this.end * this.size;

    }
}