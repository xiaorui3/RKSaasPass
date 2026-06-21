package com.tianji.message.domain.dto;

import lombok.Data;

@Data
public class WikiDocumentDTO {

    private Long id;

    private String title;

    private String category;

    private String content;

    private Integer status;
}
