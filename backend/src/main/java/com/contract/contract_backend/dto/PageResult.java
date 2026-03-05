package com.contract.contract_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * ✅ 通用分页结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResult<T> {

    /** 当前页数据 */
    @Builder.Default
    private List<T> records = Collections.emptyList();

    /** 总条数 */
    private long total;

    /** 当前页码（从1开始） */
    private int page;

    /** 每页大小 */
    private int size;

    /** 总页数 */
    private int totalPages;

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int totalPages = (int) Math.ceil(total * 1.0 / Math.max(size, 1));
        return PageResult.<T>builder()
                .records(records == null ? Collections.emptyList() : records)
                .total(total)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }
}