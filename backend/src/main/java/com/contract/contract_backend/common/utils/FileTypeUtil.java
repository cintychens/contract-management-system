package com.contract.contract_backend.common.utils;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public class FileTypeUtil {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private FileTypeUtil() {
    }

    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowedExtension(String extension, Set<String> allowedExtensions) {
        return allowedExtensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    public static boolean isAllowedContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
    }

    public static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unknown";
        }
        return originalFilename.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}