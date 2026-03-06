package com.contract.contract_backend.common.utils;

import java.io.InputStream;
import java.security.MessageDigest;

public class HashUtil {

    private HashUtil() {
    }

    public static String sha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算文件哈希失败", e);
        }
    }
}