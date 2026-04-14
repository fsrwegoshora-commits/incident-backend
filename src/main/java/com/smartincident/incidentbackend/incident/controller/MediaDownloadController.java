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

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaDownloadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Authenticated
    @GetMapping("/download")
    public Response<MediaDownloadResponse> downloadMedia(@RequestParam String fileUrl) {

        log.info("Downloading media file: {}", fileUrl);

        try {
            String filename = extractFilenameFromUrl(fileUrl);
            if (filename == null) {
                return Response.error("Invalid file URL");
            }

            String mediaType = determineMediaType(filename);
            String subfolder = mediaType.toLowerCase() + "s";

            Path filePath = Paths.get(uploadDir, subfolder, filename);

            if (!Files.exists(filePath)) {
                log.error("File not found: {}", filePath);
                return Response.error("File not found");
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);

            String mimeType = getMimeType(filename);
            String dataUrl = "data:" + mimeType + ";base64," + base64Data;

            MediaDownloadResponse response = new MediaDownloadResponse();
            response.setFileName(filename);
            response.setFileSize((long) fileBytes.length);
            response.setMediaType(mediaType);
            response.setBase64Data(dataUrl);
            response.setMimeType(mimeType);

            log.info("Media downloaded successfully: {} ({} bytes)", filename, fileBytes.length);

            return Response.success(response);

        } catch (IOException e) {
            log.error("Failed to download media: {}", e.getMessage());
            return Response.error("Failed to download media: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during download: {}", e.getMessage());
            return Response.error("Unexpected error: " + e.getMessage());
        }
    }

    @Authenticated
    @GetMapping("/info")
    public Response<MediaInfoResponse> getMediaInfo(@RequestParam String fileUrl) {

        log.info("Getting media info: {}", fileUrl);

        try {
            String filename = extractFilenameFromUrl(fileUrl);
            if (filename == null) {
                return Response.error("Invalid file URL");
            }

            String mediaType = determineMediaType(filename);
            String subfolder = mediaType.toLowerCase() + "s";
            Path filePath = Paths.get(uploadDir, subfolder, filename);

            if (!Files.exists(filePath)) {
                return Response.error("File not found");
            }

            long fileSize = Files.size(filePath);
            String mimeType = getMimeType(filename);

            MediaInfoResponse response = new MediaInfoResponse();
            response.setFileName(filename);
            response.setFileSize(fileSize);
            response.setMediaType(mediaType);
            response.setMimeType(mimeType);
            response.setExists(true);

            return Response.success(response);

        } catch (Exception e) {
            log.error("Error getting media info: {}", e.getMessage());
            return Response.error("Error getting media info: " + e.getMessage());
        }
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return null;
        }
        String[] parts = fileUrl.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    private String determineMediaType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".gif")) {
            return "IMAGE";
        } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") ||
                lowerName.endsWith(".mov") || lowerName.endsWith(".mkv")) {
            return "VIDEO";
        } else if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                lowerName.endsWith(".m4a") || lowerName.endsWith(".aac")) {
            return "AUDIO";
        } else {
            return "FILE";
        }
    }

    private String getMimeType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerName.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (lowerName.endsWith(".mov")) {
            return "video/quicktime";
        } else if (lowerName.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerName.endsWith(".wav")) {
            return "audio/wav";
        } else {
            return "application/octet-stream";
        }
    }

    @Data
    public static class MediaDownloadResponse {
        private String fileName;
        private Long fileSize;
        private String mediaType;
        private String base64Data;
        private String mimeType;
    }

    @Data
    public static class MediaInfoResponse {
        private String fileName;
        private Long fileSize;
        private String mediaType;
        private String mimeType;
        private Boolean exists;
    }
}
