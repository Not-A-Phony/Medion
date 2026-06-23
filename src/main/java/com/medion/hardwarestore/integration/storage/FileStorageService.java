package com.medion.hardwarestore.integration.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    public String uploadFile(MultipartFile file, String folderName) {
        log.info("Uploading file {} to Cloudinary folder {}", file.getOriginalFilename(), folderName);
        // Stub: In reality, use Cloudinary SDK
        // Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", folderName));
        // return uploadResult.get("secure_url").toString();
        
        return "https://res.cloudinary.com/demo/image/upload/v1/" + folderName + "/" + UUID.randomUUID() + ".jpg";
    }

    public void deleteFile(String publicId) {
        log.info("Deleting file {} from Cloudinary", publicId);
        // Stub: cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
