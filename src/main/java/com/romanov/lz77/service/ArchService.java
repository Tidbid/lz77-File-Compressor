package com.romanov.lz77.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ArchService {
    void uploadFile(Boolean isEncoded, MultipartFile multipartFile) throws IOException;
    boolean encodeFile(int bufferSize);

    boolean decodeFile();

    double getRatio() throws Exception;
}
