package com.tianji.common.autoconfigure.media;

import com.tianji.common.domain.R;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaPathHelperTest {

    @Test
    void normalizeRequestBody_shouldKeepBucketScopedRelativePath() {
        MediaPathHelper helper = helper();

        SamplePayload payload = new SamplePayload();
        payload.setCoverImage("rk-content/news-image/2026/04/23/cover.png");
        payload.setAttachmentUrl("rk-message/notice-attachment/2026/04/23/file.pdf");

        helper.normalizeRequestBody(payload);

        assertEquals("rk-content/news-image/2026/04/23/cover.png", payload.getCoverImage());
        assertEquals("rk-message/notice-attachment/2026/04/23/file.pdf", payload.getAttachmentUrl());
    }

    @Test
    void normalizeRequestBody_shouldStripPublicRootAndKeepBucket() {
        MediaPathHelper helper = helper();

        SamplePayload payload = new SamplePayload();
        payload.setCoverImage("http://cdn.test/rk-content/news-image/2026/04/23/cover.png");
        payload.setAttachmentUrl("http://cdn.test/rk-message/notice-attachment/2026/04/23/file.pdf");

        helper.normalizeRequestBody(payload);

        assertEquals("rk-content/news-image/2026/04/23/cover.png", payload.getCoverImage());
        assertEquals("rk-message/notice-attachment/2026/04/23/file.pdf", payload.getAttachmentUrl());
    }

    @Test
    void normalizeRequestBody_shouldFallbackOldSingleBucketUrlToBucketScopedPath() {
        MediaPathHelper helper = helper();

        SamplePayload payload = new SamplePayload();
        payload.setCoverImage("http://cdn.test/rk-bucket/news/cover.png");

        helper.normalizeRequestBody(payload);

        assertEquals("rk-bucket/news/cover.png", payload.getCoverImage());
    }

    @Test
    void normalizeRequestBody_shouldStripUnknownHostWhenPathIsManagedMedia() {
        MediaPathHelper helper = helper();

        SamplePayload payload = new SamplePayload();
        payload.setCoverImage("http://203.0.113.10:19000/rk-activity/activity-cover/2026/04/24/cover.png");
        payload.setAttachmentUrl("https://files.example.com/rk-content/news-attachment/2026/04/24/file.pdf");

        helper.normalizeRequestBody(payload);

        assertEquals("rk-activity/activity-cover/2026/04/24/cover.png", payload.getCoverImage());
        assertEquals("rk-content/news-attachment/2026/04/24/file.pdf", payload.getAttachmentUrl());
    }

    @Test
    void expandResponseBody_shouldPrependRootForBucketScopedPath() {
        MediaPathHelper helper = helper();

        SamplePayload payload = new SamplePayload();
        payload.setCoverImage("rk-content/news-image/2026/04/23/cover.png");
        payload.setAttachmentUrl("rk-message/notice-attachment/2026/04/23/file.pdf");
        payload.setPath("system/users");

        NestedPayload nested = new NestedPayload();
        nested.setIcon("rk-user/avatar/2026/04/23/avatar.png");
        nested.setDemoVideo("rk-content/news-video/2026/04/23/demo.mp4");
        payload.setNested(nested);

        R<SamplePayload> response = R.ok(payload);
        helper.expandResponseBody(response);

        assertEquals("http://cdn.test/rk-content/news-image/2026/04/23/cover.png", response.getData().getCoverImage());
        assertEquals("http://cdn.test/rk-message/notice-attachment/2026/04/23/file.pdf", response.getData().getAttachmentUrl());
        assertEquals("http://cdn.test/rk-user/avatar/2026/04/23/avatar.png", response.getData().getNested().getIcon());
        assertEquals("http://cdn.test/rk-content/news-video/2026/04/23/demo.mp4", response.getData().getNested().getDemoVideo());
        assertEquals("system/users", response.getData().getPath());
    }

    @Test
    void expandResponseBody_shouldFallbackOldRelativePathToDefaultBucket() {
        MediaPathHelper helper = helper();

        Map<String, Object> body = new HashMap<>();
        body.put("coverImage", "activity/cover.png");
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("attachmentUrl", "activity/file.pdf");
        body.put("items", List.of(nestedMap));

        helper.expandResponseBody(body);

        assertEquals("http://cdn.test/rk-bucket/activity/cover.png", body.get("coverImage"));
        Object nestedItem = ((List<?>) body.get("items")).get(0);
        assertEquals("http://cdn.test/rk-bucket/activity/file.pdf", ((Map<?, ?>) nestedItem).get("attachmentUrl"));
    }

    @Test
    void expandResponseBody_shouldRewriteLegacyManagedAbsoluteUrlToCurrentPublicRoot() {
        MediaPathHelper helper = helper();

        SamplePayload payload = new SamplePayload();
        payload.setCoverImage("http://127.0.0.1:9000/rk-activity/activity-cover/2026/04/25/cover.png");
        payload.setAttachmentUrl("http://127.0.0.1:9000/rk-activity/activity-attachment/2026/04/25/file.pdf");

        helper.expandResponseBody(payload);

        assertEquals("http://cdn.test/rk-activity/activity-cover/2026/04/25/cover.png", payload.getCoverImage());
        assertEquals("http://cdn.test/rk-activity/activity-attachment/2026/04/25/file.pdf", payload.getAttachmentUrl());
    }

    @Test
    void expandResponseBody_shouldTransformAdditionalMediaFieldNames() {
        MediaPathHelper helper = helper();

        ExtraMediaPayload payload = new ExtraMediaPayload();
        payload.setImageUrl("rk-user/banner/2026/04/25/banner.png");
        payload.setPhoto("http://127.0.0.1:9000/rk-user/teacher-photo/2026/04/25/photo.png");

        helper.expandResponseBody(payload);

        assertEquals("http://cdn.test/rk-user/banner/2026/04/25/banner.png", payload.getImageUrl());
        assertEquals("http://cdn.test/rk-user/teacher-photo/2026/04/25/photo.png", payload.getPhoto());
    }

    private MediaPathHelper helper() {
        MediaPathProperties properties = new MediaPathProperties();
        properties.setEndpoint("http://cdn.test");
        properties.setBucketName("rk-bucket");
        properties.setPublicBaseUrl("http://cdn.test/rk-bucket/");
        return new MediaPathHelper(properties);
    }

    public static class SamplePayload {
        private String coverImage;
        private String attachmentUrl;
        private String path;
        private NestedPayload nested;

        public String getCoverImage() {
            return coverImage;
        }

        public void setCoverImage(String coverImage) {
            this.coverImage = coverImage;
        }

        public String getAttachmentUrl() {
            return attachmentUrl;
        }

        public void setAttachmentUrl(String attachmentUrl) {
            this.attachmentUrl = attachmentUrl;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public NestedPayload getNested() {
            return nested;
        }

        public void setNested(NestedPayload nested) {
            this.nested = nested;
        }
    }

    public static class NestedPayload {
        private String icon;
        private String demoVideo;

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getDemoVideo() {
            return demoVideo;
        }

        public void setDemoVideo(String demoVideo) {
            this.demoVideo = demoVideo;
        }
    }

    public static class ExtraMediaPayload {
        private String imageUrl;
        private String photo;

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getPhoto() {
            return photo;
        }

        public void setPhoto(String photo) {
            this.photo = photo;
        }
    }
}
