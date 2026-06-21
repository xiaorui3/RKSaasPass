package com.tianji.common.autoconfigure.media;

import com.tianji.common.domain.R;
import com.tianji.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class MediaPathHelper {

    private static final Set<String> MEDIA_FIELD_NAMES = Set.of(
            "coverImage",
            "videoUrl",
            "attachmentUrl",
            "imageUrl",
            "photo",
            "rulesFile",
            "materialsFile",
            "resultsFile",
            "avatar",
            "icon",
            "cover",
            "coverImageUrl",
            "demoVideo",
            "fileUrl"
    );

    private final MediaPathProperties properties;

    public String normalizeForStorage(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (!isAbsoluteUrl(trimmed)) {
            return stripLeadingSlash(trimmed);
        }

        String rootUrl = getResolvedPublicRootUrl();
        if (StringUtils.isNotBlank(rootUrl) && trimmed.startsWith(rootUrl)) {
            return normalizeStrippedPath(trimmed.substring(rootUrl.length()), false);
        }

        String baseUrl = properties.getResolvedPublicBaseUrl();
        if (StringUtils.isNotBlank(baseUrl) && trimmed.startsWith(baseUrl)) {
            return normalizeStrippedPath(trimmed.substring(baseUrl.length()), true);
        }

        String fallbackBaseUrl = buildFallbackBaseUrl();
        if (StringUtils.isNotBlank(fallbackBaseUrl) && trimmed.startsWith(fallbackBaseUrl)) {
            return normalizeStrippedPath(trimmed.substring(fallbackBaseUrl.length()), true);
        }

        String pathFromAbsoluteUrl = extractManagedPathFromAbsoluteUrl(trimmed);
        if (StringUtils.isNotBlank(pathFromAbsoluteUrl)) {
            return pathFromAbsoluteUrl;
        }

        return stripLeadingSlash(trimmed);
    }

    public String toPublicUrl(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("/")) {
            return trimmed;
        }
        if (isAbsoluteUrl(trimmed)) {
            String managedPath = extractManagedPathFromAbsoluteUrl(trimmed);
            if (StringUtils.isBlank(managedPath)) {
                return trimmed;
            }
            String rootUrl = getResolvedPublicRootUrl();
            return StringUtils.isBlank(rootUrl) ? managedPath : rootUrl + managedPath;
        }

        String normalized = stripLeadingSlash(trimmed);
        String rootUrl = getResolvedPublicRootUrl();
        if (StringUtils.isBlank(rootUrl)) {
            return normalized;
        }

        if (looksLikeBucketScopedPath(normalized)) {
            return rootUrl + normalized;
        }
        if (StringUtils.isNotBlank(properties.getBucketName())) {
            return rootUrl + stripLeadingSlash(properties.getBucketName()) + "/" + normalized;
        }
        return rootUrl + normalized;
    }

    public void normalizeRequestBody(Object body) {
        mutate(body, true, new IdentityHashMap<>());
    }

    public void expandResponseBody(Object body) {
        mutate(body, false, new IdentityHashMap<>());
    }

    private void mutate(Object value, boolean normalize, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isSimpleValue(value.getClass())) {
            return;
        }
        if (value instanceof R) {
            mutate(((R<?>) value).getData(), normalize, visited);
            return;
        }
        if (visited.containsKey(value)) {
            return;
        }
        visited.put(value, Boolean.TRUE);

        if (value instanceof Map) {
            mutateMap((Map<?, ?>) value, normalize, visited);
            return;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                mutate(item, normalize, visited);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                mutate(Array.get(value, i), normalize, visited);
            }
            return;
        }
        mutateBean(value, normalize, visited);
    }

    @SuppressWarnings("unchecked")
    private void mutateMap(Map<?, ?> map, boolean normalize, IdentityHashMap<Object, Boolean> visited) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object item = entry.getValue();
            if (!(key instanceof String)) {
                mutate(item, normalize, visited);
                continue;
            }
            String fieldName = (String) key;
            if (item instanceof String && shouldTransformMapField(fieldName, map)) {
                ((Map<Object, Object>) map).put(key, normalize ? normalizeForStorage((String) item) : toPublicUrl((String) item));
            } else {
                mutate(item, normalize, visited);
            }
        }
    }

    private void mutateBean(Object bean, boolean normalize, IdentityHashMap<Object, Boolean> visited) {
        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
            String fieldName = descriptor.getName();
            if ("class".equals(fieldName) || descriptor.getReadMethod() == null) {
                continue;
            }
            Object fieldValue;
            try {
                fieldValue = wrapper.getPropertyValue(fieldName);
            } catch (Exception e) {
                continue;
            }
            if (fieldValue == null) {
                continue;
            }
            if (isFileDtoRelativePathField(bean.getClass(), fieldName)) {
                continue;
            }
            if (fieldValue instanceof String && shouldTransformBeanField(bean.getClass(), fieldName)) {
                if (descriptor.getWriteMethod() != null) {
                    String converted = normalize ? normalizeForStorage((String) fieldValue) : toPublicUrl((String) fieldValue);
                    wrapper.setPropertyValue(fieldName, converted);
                }
                continue;
            }
            mutate(fieldValue, normalize, visited);
        }
    }

    private boolean shouldTransformMapField(String fieldName, Map<?, ?> map) {
        if (MEDIA_FIELD_NAMES.contains(fieldName)) {
            return true;
        }
        if ("url".equals(fieldName) || "path".equals(fieldName)) {
            return map.containsKey("fileName") || map.containsKey("filename") || map.containsKey("fileUrl");
        }
        return false;
    }

    private boolean shouldTransformBeanField(Class<?> ownerType, String fieldName) {
        if (MEDIA_FIELD_NAMES.contains(fieldName)) {
            return true;
        }
        return ownerType.getName().equals("com.tianji.media.domain.dto.FileDTO")
                && ("url".equals(fieldName) || "fileUrl".equals(fieldName));
    }

    private boolean isFileDtoRelativePathField(Class<?> ownerType, String fieldName) {
        return ownerType.getName().equals("com.tianji.media.domain.dto.FileDTO")
                && "path".equals(fieldName);
    }

    private boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type)
                || Enum.class.isAssignableFrom(type)
                || type.getName().startsWith("java.time.")
                || type.getName().startsWith("java.lang.")
                || type.getName().startsWith("java.math.");
    }

    private String getResolvedPublicRootUrl() {
        String publicBaseUrl = properties.getPublicBaseUrl();
        if (StringUtils.isNotBlank(publicBaseUrl)) {
            String normalizedBase = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
            String bucketSegment = StringUtils.isNotBlank(properties.getBucketName())
                    ? stripLeadingSlash(properties.getBucketName()) + "/"
                    : "";
            if (StringUtils.isNotBlank(bucketSegment) && normalizedBase.endsWith(bucketSegment)) {
                return normalizedBase.substring(0, normalizedBase.length() - bucketSegment.length());
            }
            return normalizedBase;
        }
        if (StringUtils.isBlank(properties.getEndpoint())) {
            return "";
        }
        return properties.getEndpoint().endsWith("/") ? properties.getEndpoint() : properties.getEndpoint() + "/";
    }

    private String buildFallbackBaseUrl() {
        if (StringUtils.isBlank(properties.getEndpoint()) || StringUtils.isBlank(properties.getBucketName())) {
            return "";
        }
        String endpoint = properties.getEndpoint().endsWith("/")
                ? properties.getEndpoint()
                : properties.getEndpoint() + "/";
        return endpoint + stripLeadingSlash(properties.getBucketName()) + "/";
    }

    private String normalizeStrippedPath(String strippedPath, boolean prefixDefaultBucket) {
        String normalized = stripLeadingSlash(strippedPath);
        if (StringUtils.isBlank(normalized)) {
            return normalized;
        }
        if (looksLikeBucketScopedPath(normalized)) {
            return normalized;
        }
        if (prefixDefaultBucket && StringUtils.isNotBlank(properties.getBucketName())) {
            return stripLeadingSlash(properties.getBucketName()) + "/" + normalized;
        }
        return normalized;
    }

    private boolean looksLikeBucketScopedPath(String value) {
        if (StringUtils.isBlank(value) || !value.contains("/")) {
            return false;
        }
        String firstSegment = value.substring(0, value.indexOf('/'));
        if (StringUtils.isBlank(firstSegment)) {
            return false;
        }
        return firstSegment.equals(properties.getBucketName()) || firstSegment.startsWith("rk-");
    }

    private boolean isAbsoluteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String extractManagedPathFromAbsoluteUrl(String value) {
        if (!isAbsoluteUrl(value)) {
            return "";
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            String path = stripLeadingSlash(uri.getPath());
            if (StringUtils.isBlank(path)) {
                return "";
            }
            if (looksLikeBucketScopedPath(path)) {
                return path;
            }
            String defaultBucket = properties.getBucketName();
            if (StringUtils.isNotBlank(defaultBucket)) {
                String bucketPrefix = stripLeadingSlash(defaultBucket) + "/";
                if (path.startsWith(bucketPrefix)) {
                    return path;
                }
            }
            return "";
        } catch (Exception e) {
            log.debug("failed to extract managed media path from absolute url: {}", value, e);
            return "";
        }
    }

    private String stripLeadingSlash(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
