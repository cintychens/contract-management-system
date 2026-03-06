package com.contract.contract_backend.common.utils;

import java.time.LocalDateTime;
import java.util.UUID;

public class ObjectKeyUtil {

    private ObjectKeyUtil() {
    }

    public static String buildContractObjectKey(String contractNo, String fileName) {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear());
        String month = String.format("%02d", now.getMonthValue());
        String safeName = FileTypeUtil.sanitizeFileName(fileName);

        return "contracts/" + year + "/" + month + "/" + contractNo + "/"
                + UUID.randomUUID() + "_" + safeName;
    }
}