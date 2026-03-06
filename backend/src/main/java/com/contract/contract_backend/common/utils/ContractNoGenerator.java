package com.contract.contract_backend.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class ContractNoGenerator {

    private ContractNoGenerator() {
    }

    public static String generate() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "CT-" + time + "-" + random;
    }
}