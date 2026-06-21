package com.tianji.common.utils;


import com.tianji.common.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Map;

@Slf4j
public class WebUtils {
    private static final String[] CLIENT_IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "True-Client-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "Forwarded"
    };

    /**
     * 获取ServletRequestAttributes
     *
     * @return ServletRequestAttributes
     */
    public static ServletRequestAttributes getServletRequestAttributes() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra == null) {
            return null;
        }
        return (ServletRequestAttributes) ra;
    }

    /**
     * 获取request
     *
     * @return HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = getServletRequestAttributes();
        return servletRequestAttributes == null ? null : servletRequestAttributes.getRequest();
    }

    /**
     * 获取response
     *
     * @return HttpServletResponse
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = getServletRequestAttributes();
        return servletRequestAttributes == null ? null : servletRequestAttributes.getResponse();
    }

    /**
     * 获取request header中的内容
     *
     * @param headerName 请求头名称
     * @return 请求头的值
     */
    public static String getHeader(String headerName) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return getRequest().getHeader(headerName);
    }

    public static void setResponseHeader(String key, String value){
        HttpServletResponse response = getResponse();
        if (response == null) {
            return;
        }
        response.setHeader(key, value);
    }


    public static String getRequestId() {
        return getHeader(Constant.REQUEST_ID_HEADER);
    }

    public static boolean isGatewayRequest() {
        String originName = getHeader(Constant.REQUEST_FROM_HEADER);
        return Constant.GATEWAY_ORIGIN_NAME.equals(originName);
    }

    public static boolean isFeignRequest() {
        String originName = getHeader(Constant.REQUEST_FROM_HEADER);
        return Constant.FEIGN_ORIGIN_NAME.equals(originName);
    }

    public static boolean isSuccess() {
        HttpServletResponse response = getResponse();
        return response != null && response.getStatus() < 300;
    }

    /**
     * 获取请求地址中的请求参数组装成 key1=value1&key2=value2
     * 如果key对应多个值，中间使用逗号隔开例如 key1对应value1，key2对应value2，value3， key1=value1&key2=value2,value3
     *
     * @param request
     * @return 返回拼接字符串
     */
    public static String getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        return getParameters(parameterMap);
    }

    /**
     * 获取请求地址中的请求参数组装成 key1=value1&key2=value2
     * 如果key对应多个值，中间使用逗号隔开例如 key1对应value1，key2对应value2，value3， key1=value1&key2=value2,value3
     *
     * @param queries
     * @return
     */
    public  static <T> String getParameters(final Map<String, T> queries) {
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, T> entry : queries.entrySet()) {
            if(entry.getValue() instanceof String[]){
                buffer.append(entry.getKey()).append(String.join(",", ((String[])entry.getValue())))
                    .append("&");
            }else if(entry.getValue() instanceof Collection){
                buffer.append(entry.getKey()).append(
                        CollUtils.join(((Collection<String>)entry.getValue()),",")
                ).append("&");
            }
        }
        return buffer.length() > 0 ? buffer.substring(0, buffer.length() - 1) : StringUtils.EMPTY;
    }

    /**
     * 获取请求url中的uri
     *
     * @param url
     * @return
     */
    public static String getUri(String url){
        if(StringUtils.isEmpty(url)) {
            return null;
        }

        String uri = url;
        //uri中去掉 http:// 或者https
        if(uri.contains("http://") ){
            uri = uri.replace("http://", StringUtils.EMPTY);
        }else if(uri.contains("https://")){
            uri = uri.replace("https://", StringUtils.EMPTY);
        }

        int endIndex = uri.length(); //uri 在url中的最后一个字符的序号+1
        if(uri.contains("?")){
            endIndex = uri.indexOf("?");
        }
        return uri.substring(uri.indexOf("/"), endIndex);
    }

    public static String getRemoteAddr() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }
        return getClientIp(request);
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        String firstValidIp = "";
        for (String header : CLIENT_IP_HEADERS) {
            String value = request.getHeader(header);
            for (String candidate : splitClientIpHeader(header, value)) {
                if ("Forwarded".equalsIgnoreCase(header) && !candidate.toLowerCase().contains("for=")) {
                    continue;
                }
                String ip = normalizeClientIp(candidate);
                if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
                    continue;
                }
                if (StringUtils.isEmpty(firstValidIp)) {
                    firstValidIp = ip;
                }
                if (!isInternalIp(ip)) {
                    return ip;
                }
            }
        }

        String remoteAddr = normalizeClientIp(request.getRemoteAddr());
        if (!StringUtils.isEmpty(remoteAddr) && !isInternalIp(remoteAddr)) {
            return remoteAddr;
        }
        return StringUtils.isEmpty(firstValidIp) ? remoteAddr : firstValidIp;
    }

    private static String[] splitClientIpHeader(String header, String value) {
        if (StringUtils.isEmpty(value)) {
            return new String[0];
        }
        if ("Forwarded".equalsIgnoreCase(header)) {
            return value.replace(";", ",").split(",");
        }
        return value.split(",");
    }

    private static String normalizeClientIp(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        String ip = value.trim();
        int forwardedForIndex = ip.toLowerCase().indexOf("for=");
        if (forwardedForIndex >= 0) {
            ip = ip.substring(forwardedForIndex + 4).trim();
        }
        if (ip.startsWith("\"") && ip.endsWith("\"") && ip.length() > 1) {
            ip = ip.substring(1, ip.length() - 1).trim();
        }
        if (ip.startsWith("[")) {
            int end = ip.indexOf(']');
            if (end > 0) {
                return ip.substring(1, end);
            }
        }
        int colonCount = 0;
        for (int i = 0; i < ip.length(); i++) {
            if (ip.charAt(i) == ':') {
                colonCount++;
            }
        }
        if (colonCount == 1) {
            int colon = ip.indexOf(':');
            String possiblePort = ip.substring(colon + 1);
            if (possiblePort.matches("\\d+")) {
                ip = ip.substring(0, colon);
            }
        }
        return ip;
    }

    private static boolean isInternalIp(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return true;
        }
        String value = ip.trim().toLowerCase();
        if ("localhost".equals(value)
                || "::1".equals(value)
                || "0:0:0:0:0:0:0:1".equals(value)
                || value.startsWith("fe80:")
                || value.startsWith("fc")
                || value.startsWith("fd")) {
            return true;
        }
        if (value.startsWith("10.")
                || value.startsWith("127.")
                || value.startsWith("192.168.")
                || value.startsWith("169.254.")
                || value.startsWith("100.64.")) {
            return true;
        }
        String[] parts = value.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 172 && second >= 16 && second <= 31;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static CookieBuilder cookieBuilder(){
        return new CookieBuilder(getRequest(), getResponse());
    }
}
