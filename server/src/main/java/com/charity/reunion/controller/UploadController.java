package com.charity.reunion.controller;

import com.charity.reunion.common.BizException;
import com.charity.reunion.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 图片上传(头像 / 活动封面)
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final List<String> ALLOWED = List.of("jpg", "jpeg", "png", "gif", "webp");

    private final String uploadDir;

    public UploadController(@Value("${dating.upload-dir:./upload}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持图片文件: jpg/png/gif/webp");
        }
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("上传目录创建失败");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        file.transferTo(new File(dir.getAbsoluteFile(), name));
        return Result.ok("/upload/" + name);
    }
}
