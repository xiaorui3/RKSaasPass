package com.tianji.media.controller;

import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.autoconfigure.media.MediaPathProperties;
import com.tianji.common.domain.R;
import com.tianji.media.domain.dto.FileDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileDtoMediaPathTest {

    @Test
    void expandResponseBody_shouldKeepFileDtoPathRelativeWhileExpandingPublicUrls() {
        MediaPathProperties properties = new MediaPathProperties();
        properties.setEndpoint("http://cdn.test");
        properties.setBucketName("rk-bucket");
        properties.setPublicBaseUrl("http://cdn.test/rk-bucket/");
        MediaPathHelper helper = new MediaPathHelper(properties);

        FileDTO dto = FileDTO.of(
                1L,
                "avatar.png",
                "rk-user/avatar/2026/04/24/avatar.png",
                "http://cdn.test/rk-user/avatar/2026/04/24/avatar.png"
        );

        helper.expandResponseBody(R.ok(dto));

        assertEquals("rk-user/avatar/2026/04/24/avatar.png", dto.getPath());
        assertEquals("http://cdn.test/rk-user/avatar/2026/04/24/avatar.png", dto.getUrl());
        assertEquals("http://cdn.test/rk-user/avatar/2026/04/24/avatar.png", dto.getFileUrl());
    }
}
