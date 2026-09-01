package com.hzj.aicodemother.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.hzj.aicodemother.common.BaseResponse;
import com.hzj.aicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@Tag(name = "health")
public class HealthController {
    @GetMapping("/")
    @Operation(summary = "健康检查")
    public BaseResponse <String> health(){
        return ResultUtils.success("ok") ;
    }
}
