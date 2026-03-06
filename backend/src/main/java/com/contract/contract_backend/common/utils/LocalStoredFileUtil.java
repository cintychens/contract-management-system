package com.contract.contract_backend.common.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalStoredFileUtil {

    private LocalStoredFileUtil() {
    }

    public static Path buildFullPath(String baseDir, String objectKey) {
        return Paths.get(baseDir, objectKey).normalize();
    }
}