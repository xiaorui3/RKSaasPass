package com.tianji.api.dto.user;

import lombok.Data;

@Data
public class EmailLoginCandidateQueryDTO {

    private String email;

    private Long tenantId;
}
