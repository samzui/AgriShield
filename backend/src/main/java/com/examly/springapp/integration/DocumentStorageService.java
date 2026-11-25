package com.examly.springapp.integration;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
public interface DocumentStorageService {
    String store(MultipartFile file)throws IOException;
    byte[] read(String key)throws IOException;
    void delete(String key)throws IOException;
}
