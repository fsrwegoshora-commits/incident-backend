package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.utils.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Authenticated
    @PostMapping("/upload")
    public Response<MediaUploadResponse> uploadMedia(
            @RequestParam String base64File,
            @RequestParam String fileName,
            @RequestParam String mediaType) {

        log.info("Uploading media file: {} type: {}", fileName, mediaType);

        try {
            if (base64File == null || base64File.trim().isEmpty()) {
                return Response.error("Base64 file data is required");
            }

            String base64Data = base64File;
            if (base64File.contains(",")) {
                base64Data = base64File.split(",")[1];
            }

            byte[] fileBytes;
            try {
                fileBytes = Base64.getDecoder().decode(base64Data);
            } catch (IllegalArgumentException e) {
                log.error("Invalid base64 data: {}", e.getMessage());
                return Response.error("Invalid base64 file data");
            }

            if (fileBytes.length > 50 * 1024 * 1024) {
                return Response.error("File size too large. Maximum size is 50MB");
            }

            if (!isValidMediaType(mediaType)) {
                return Response.error("Invalid media type: " + mediaType);
            }

            Path uploadPath = Paths.get(uploadDir, mediaType.toLowerCase() + "s");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileExtension = getFileExtension(fileName);
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.write(filePath, fileBytes);

            String fileUrl = "/uploads/" + mediaType.toLowerCase() + "s/" + uniqueFileName;

            MediaUploadResponse response = new MediaUploadResponse();
            response.setFileUrl(fileUrl);
            response.setFileName(uniqueFileName);
            response.setOriginalFileName(fileName);
            response.setFileSize((long) fileBytes.length);
            response.setMediaType(mediaType);

            log.info("Media uploaded successfully: {}", fileUrl);

            return Response.success(response);

        } catch (IOException e) {
            log.error("Failed to upload media: {}", e.getMessage());
            return Response.error("Failed to upload media: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during upload: {}", e.getMessage());
            return Response.error("Unexpected error: " + e.getMessage());
        }
    }

    private boolean isValidMediaType(String mediaType) {
        if (mediaType == null) return false;
        switch (mediaType.toUpperCase()) {
            case "IMAGE":
            case "AUDIO":
            case "VIDEO":
                return true;
            default:
                return false;
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".dat";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @Data
    public static class MediaUploadResponse {
        private String fileUrl;
        private String fileName;
        private String originalFileName;
        private Long fileSize;
        private String mediaType;
    }
}
