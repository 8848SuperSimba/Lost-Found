package com.lostfound.service.impl;

import com.lostfound.common.ResultCode;
import com.lostfound.exception.BusinessException;
import com.lostfound.service.UploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.max-size}")
    private long maxSize;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件超过大小限制");
        }

        String filename = file.getOriginalFilename();
        String extension = getExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "仅支持 jpg/jpeg/png/webp 格式");
        }

        String monthDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String generatedName = UUID.randomUUID().toString().replace("-", "") + "." + extension.toLowerCase(Locale.ROOT);

        String rootPath = StringUtils.trimTrailingCharacter(uploadPath, '/');
        Path targetDir = Paths.get(rootPath, monthDir);
        try {
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetFile = targetDir.resolve(generatedName);
            file.transferTo(targetFile.toFile());
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.ERROR, "上传文件失败");
        }

        return "/" + rootPath + "/" + monthDir + "/" + generatedName;
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
