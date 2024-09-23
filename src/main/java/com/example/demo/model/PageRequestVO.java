package com.example.demo.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestVO {

    @Builder.Default
    @Min(value = 1)
    @Positive
    private int pageNo = 1;

    @Builder.Default
    @Min(value = 10)
    @Max(value = 100)
    @Positive
    private int size = 10;

    private String link;

    private String searchKey;

    public int getSkip() {
        return (pageNo - 1) * size;
    }

    public String getLink() {
        if (link == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("page=").append(this.pageNo);
            builder.append("&size=").append(this.size);

            if (this.searchKey != null && this.searchKey.length() != 0) {
                try {
                    builder.append("&searchKey=").append(URLEncoder.encode(this.searchKey, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            link = builder.toString();
        }
        return link;
    }
}