package com.examly.springapp.integration;
import com.examly.springapp.util.FileEncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
@Service public class LocalDocumentStorageService implements DocumentStorageService {
    private final Path root;
    private final FileEncryptionService encryption;
    public LocalDocumentStorageService(@Value("${app.upload-dir}")String dir,FileEncryptionService encryption) {
        this.root=Paths.get(dir);
        this.encryption=encryption;
        try {
            Files.createDirectories(root);
        }catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }
    public String store(MultipartFile file)throws IOException {
        String original=file.getOriginalFilename()==null?"file":Path.of(file.getOriginalFilename()).getFileName().toString();
        String key=UUID.randomUUID()+"-"+original+".enc";
        Files.write(root.resolve(key),encryption.encrypt(file.getBytes()),StandardOpenOption.CREATE_NEW);
        return key;
    }
    public byte[] read(String key)throws IOException {
        return encryption.decrypt(Files.readAllBytes(root.resolve(Path.of(key).getFileName())));
    }
    public void delete(String key)throws IOException {
        Files.deleteIfExists(root.resolve(Path.of(key).getFileName()));
    }
}
