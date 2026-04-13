package com.lostfound.controller;

import com.lostfound.common.Result;
import com.lostfound.service.UploadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/image")
    public Result<String> uploadImage(MultipartFile file) {
        return Result.success(uploadService.uploadImage(file));
    }
}
