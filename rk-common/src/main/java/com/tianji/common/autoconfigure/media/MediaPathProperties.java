package com.tianji.common.autoconfigure.media;

import com.tianji.common.utils.StringUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "minio")
public class MediaPathProperties {

    private String endpoint;
    private String bucketName;
    private String publicBaseUrl;

    public String getResolvedPublicBaseUrl() {
        String baseUrl = StringUtils.isNotBlank(publicBaseUrl) ? publicBaseUrl : buildDefaultBaseUrl();
        if (StringUtils.isBlank(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String buildDefaultBaseUrl() {
        if (StringUtils.isBlank(endpoint) || StringUtils.isBlank(bucketName)) {
            return "";
        }
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return normalizedEndpoint + "/" + bucketName + "/";
    }
}
