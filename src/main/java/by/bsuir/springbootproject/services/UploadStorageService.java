package by.bsuir.springbootproject.services;

import jakarta.annotation.PostConstruct;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface UploadStorageService {

    @PostConstruct
    public void init();

    public Path coversDirectory();

    public Path pagesDirectory();

    public String storeCover(MultipartFile file, String fileName) throws IOException;

    public void storePage(MultipartFile file, String fileName) throws IOException;

    public void copyPage(Path sourcePath, String fileName) throws IOException;

    public void deleteCoverIfExists(String fileName);

    public void deletePageIfExists(String fileName);

    public Path resolveCover(String fileName);

    public Path resolvePage(String fileName);
}
