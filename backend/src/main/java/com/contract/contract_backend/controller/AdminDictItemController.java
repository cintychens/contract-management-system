package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.dto.AdminDictDto;
import com.contract.contract_backend.service.SysDictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dict-items")
@RequiredArgsConstructor
public class AdminDictItemController {

    private final SysDictItemService sysDictItemService;

    @GetMapping
    public Result<Map<String, Object>> pageDictItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String dictType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return Result.success(
                sysDictItemService.pageDictItems(page, size, dictType, keyword, status)
        );
    }

    @GetMapping("/{id}")
    public Result<AdminDictDto.Row> getDictItemDetail(@PathVariable Long id) {
        return Result.success(sysDictItemService.getDictItemDetail(id));
    }

    @PostMapping
    public Result<AdminDictDto.Row> createDictItem(@RequestBody AdminDictDto.SaveReq req) {
        return Result.success(sysDictItemService.createDictItem(req));
    }

    @PutMapping("/{id}")
    public Result<AdminDictDto.Row> updateDictItem(
            @PathVariable Long id,
            @RequestBody AdminDictDto.SaveReq req
    ) {
        return Result.success(sysDictItemService.updateDictItem(id, req));
    }

    @PatchMapping("/{id}/status")
    public Result<String> changeStatus(
            @PathVariable Long id,
            @RequestBody AdminDictDto.StatusReq req
    ) {
        sysDictItemService.changeStatus(id, req.getStatus());
        return Result.success("状态更新成功");
    }
}