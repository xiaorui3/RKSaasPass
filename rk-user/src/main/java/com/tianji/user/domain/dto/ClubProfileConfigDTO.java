package com.tianji.user.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClubProfileConfigDTO {
    private String pageTitle;
    private String pageDescription;
    private String introTitle;
    private List<String> introParagraphs;
    private List<ClubProfileSectionItemDTO> history;
    private String missionTitle;
    private List<ClubProfileSectionItemDTO> missionCards;
    private String contactEmail;
    private String contactAddress;
}
