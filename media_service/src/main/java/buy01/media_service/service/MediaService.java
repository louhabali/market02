package buy01.media_service.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    private static final Path UPLOAD_DIR = Paths.get("/app/uploads").toAbsolutePath().normalize();
    private static final long MAX_SIZE = 2L * 1024 * 1024; 
    private static final String UPLOAD_DIR_STRING = "/uploads/";
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png", "image/webp","image/gif","image/x-avif","image/avif");
    // for single avatar upload
    public String uploadSingleAvatar(MultipartFile avatar) {
        validateImage(avatar);
        return saveFile(avatar);
    }

    // for multiple images upload
    public List<String> upload(MultipartFile[] images) {
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("At least one image must be provided");
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile image : images) {
            validateImage(image);
            imageUrls.add(saveFile(image));
        }

        return imageUrls;
    }
    // Save the file to the upload directory and return its URL
    private String saveFile(MultipartFile file) {
        try {
            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
            }

            // Extract & sanitize extension
            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg"
            );
            String fileExtension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                fileExtension = originalFilename.substring(dotIndex).toLowerCase();
            }

            // Generate unique, safe filename using System time + hash
           String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + fileExtension;
            Path filePath = UPLOAD_DIR.resolve(fileName).normalize();

            
            if (!filePath.startsWith(UPLOAD_DIR)) {
                throw new SecurityException("Cannot store file outside upload directory");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return UPLOAD_DIR_STRING + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed");
        }

        if (image.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Image size must not exceed 2MB");
        }
    }

    public boolean deleteImage(String fileName) {
        try {
            if (fileName == null || fileName.isBlank()) {
                return false;
            }

           
            if (fileName.startsWith(UPLOAD_DIR_STRING)) {
                fileName = fileName.substring(UPLOAD_DIR_STRING.length());
            }

            Path filePath = UPLOAD_DIR.resolve(fileName).normalize();

            // Directory Traversal Prevention
            if (!filePath.startsWith(UPLOAD_DIR)) {
                throw new SecurityException("Cannot delete files outside upload directory");
            }

            return Files.deleteIfExists(filePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image: " + e.getMessage(), e);
        }
    }
}