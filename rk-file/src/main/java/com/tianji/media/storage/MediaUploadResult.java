package com.tianji.media.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadResult {

    private String fileId;

    private String mediaUrl;

    private String coverUrl;

    private String requestId;

    private String filename;
}
