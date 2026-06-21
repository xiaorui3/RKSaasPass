package com.tianji.api.dto.user;

import lombok.Data;

@Data
public class EmailLoginCandidateDTO {

    private Long authUserId;

    private Long tenantId;

    private Long roleId;

    private Long localUserId;

    private String username;

    private String displayName;

    private String avatar;

    private String email;
}
