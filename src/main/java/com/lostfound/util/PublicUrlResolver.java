package com.lostfound.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将库内相对路径（如 /uploads/...）转为浏览器可访问的绝对地址。
 * 部署为「前端域名 + 反代 API」且未反代 /uploads 时，可配置 app.public-base-url 为后端公网根地址（无尾斜杠）。
 */
@Component
public class PublicUrlResolver {

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    /** 返回给前端的图片/静态资源 URL */
    public String resolveUploadUrl(String path) {
        if (!StringUtils.hasText(path)) {
            return path;
        }
        String p = path.trim();
        if (p.startsWith("http://") || p.startsWith("https://")) {
            return p;
        }
        if (!StringUtils.hasText(publicBaseUrl)) {
            return p;
        }
        if (!p.startsWith("/uploads")) {
            return p;
        }
        return trimTrailingSlash(publicBaseUrl) + p;
    }

    /** 写入数据库前统一成相对路径，避免换环境后库内全是绝对地址 */
    public String normalizeStoredPath(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String p = url.trim();
        if (!StringUtils.hasText(publicBaseUrl)) {
            return p;
        }
        String base = trimTrailingSlash(publicBaseUrl);
        if (p.startsWith(base + "/uploads") || p.equals(base + "/uploads")) {
            String rest = p.substring(base.length());
            return rest.startsWith("/") ? rest : "/" + rest;
        }
        return p;
    }

    private static String trimTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }
}
