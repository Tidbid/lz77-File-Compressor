package com.romanov.lz77.controller;

import com.romanov.lz77.service.ArchService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.romanov.lz77.Lz77Application.*;

@Controller
@RequiredArgsConstructor
public class ArchiveController {
    private final ArchService archService;

    @GetMapping("/")
    String viewIndex() throws IOException {
        FileUtils.cleanDirectory(new File(ENCODE_DIR));
        FileUtils.cleanDirectory(new File(DECODE_DIR));
        return "index";
    }

    @GetMapping("/encode")
    String viewEncodePage() {
        return "redirect:/upload?encode";
    }

    @GetMapping("/decode")
    String viewDecodePage() {
        return "redirect:/upload?decode";
    }

    @GetMapping("/upload")
    String viewUploadPage() {
        return "upload";
    }

    @PostMapping("/upload/decoded")
    String viewModeSelector(@RequestParam("file") MultipartFile multipartFile){
        try {
            archService.uploadFile(false, multipartFile);
            return "mode";
        } catch(Exception e) {
            return "redirect:/?error";
        }
    }

    @PostMapping("/upload/encoded")
    String viewDecodeResultsPage(@RequestParam("file") MultipartFile multipartFile) {
        try {
            archService.uploadFile(true, multipartFile);
            return "redirect:/process/decode";
        } catch(Exception e) {
            return "redirect:/?error";
        }
    }

    @GetMapping("/process/encode")
    String viewEncodeResultsPage(@RequestParam("rad") Boolean isRad) {
        try {
            int bufferSize = (isRad) ? 8192 * 2 : 8192;
            if (archService.encodeFile(bufferSize)) {
                double ratio = archService.getRatio();
                return "redirect:/results/encode/?encode&ratio=" + ratio;
            } else {
                throw new Exception("Something went wrong while encoding the file!");
            }
        } catch(Exception e) {
            return "redirect:/?error";
        }
    }

    @GetMapping("/process/decode")
    String viewDecodeResultsPage() {
        try {
            if (archService.decodeFile()){
                return "redirect:/results/decode?decode";
            } else {
                throw new Exception("Something went wrong while decoding the file!");
            }
        } catch(Exception e) {
            return "redirect:/?error";
        }
    }

    @GetMapping("/results/encode")
    String viewDecodeResultsPage(@RequestParam("ratio") Double ratio, Model model) {
        model.addAttribute("ratio", ratio);
        return "results";
    }

    @GetMapping("/results/decode")
    String viewResultsPage() {
        return "results";
    }

    @ResponseBody
    @GetMapping("/download")
    public void downloadResult(
            @RequestParam("enc") Boolean isEncoded,
            HttpServletResponse response
    ) throws IOException {
        String path = (isEncoded) ?
                ENCODE_DIR + ENCODE_FILE + ENCODE_FORMAT : DECODE_DIR + DECODE_FILE + DECODE_FORMAT;
        File file = new File(path);
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + file.getName();
        response.setHeader(headerKey, headerValue);
        try (ServletOutputStream outputStream = response.getOutputStream();
             BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}
