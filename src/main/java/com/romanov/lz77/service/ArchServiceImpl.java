package com.romanov.lz77.service;

import com.romanov.lz77.util.HexConv;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.romanov.lz77.Lz77Application.*;

@Slf4j
@Service
public class ArchServiceImpl implements ArchService {
    @Override
    public void uploadFile(Boolean isEncoded, MultipartFile multipartFile) throws IOException {
        try {
            //We are uploading an encoded file
            //to be decoded
            if (isEncoded) {
                FileUtils.cleanDirectory(new File(ENCODE_DIR));
                String fileName = ENCODE_FILE + ENCODE_FORMAT;
                byte[] bytes = multipartFile.getBytes();
                Path path =
                        Paths.get(ENCODE_DIR + fileName);
                Files.write(path, bytes);
                return;
            }
            //We are uploading a file that is not encoded
            //to be encoded
            FileUtils.cleanDirectory(new File(DECODE_DIR));
            String fileName =
                    StringUtils.cleanPath(Objects.requireNonNull(multipartFile.getOriginalFilename()));
            String[] parts = fileName.split("[.]");
            //This is the format of our original file
            DECODE_FORMAT = "." + parts[parts.length - 1];
            //TODO mb some files won't work that way?
            fileName = DECODE_FILE + DECODE_FORMAT;
            byte[] bytes = multipartFile.getBytes();
            Path path =
                    Paths.get(DECODE_DIR + fileName);
            Files.write(path, bytes);
        } catch (IOException e) {
            log.error("Error while uploading the file: {}", multipartFile.getOriginalFilename());
            throw e;
        }
    }

    @Override
    public boolean encodeFile() {
        //DECODE_FORMAT is defined by an upload procedure
        //it is the format of the uploaded file
        String pathDec = DECODE_DIR + DECODE_FILE + DECODE_FORMAT;
        String pathEnc = ENCODE_DIR + ENCODE_FILE + ENCODE_FORMAT;
        //processing this file
        File fileDec = new File(pathDec);
        //into this one
        File fileEnc = new File(pathEnc);
        try {
            FileUtils.cleanDirectory(new File(ENCODE_DIR));
            fileEnc.createNewFile();
            try (FileOutputStream outputStream = new FileOutputStream(fileEnc);
                 BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(fileDec))) {
                //adding format for decoding purposes
                outputStream.write((DECODE_FORMAT + '\n').getBytes(Charset.defaultCharset()));
                byte[] buffer = new byte[8192];
                int bytesRead = -1;
                String hexRep;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    hexRep = HexConv.bytesToHex(buffer, bytesRead);
                    StringBuilder searchBuf = new StringBuilder(bytesRead);
                    StringBuilder line = new StringBuilder(bytesRead);
                    int i = 0;
                    //TODO check constraints on i
                    while (i < hexRep.length()) {
                        List<Integer> competingIndexes = new LinkedList<>();
                        int j = -1;
                        int lastMatch = -1;
                        int len = 0;
                        //will this thing die if j >= hexRep.length() ?
                        while((j = searchBuf.indexOf(String.valueOf(hexRep.charAt(i)), j + 1)) != -1) {
                            competingIndexes.add(j);
                        }
                        if (!competingIndexes.isEmpty()) {
                            len = 1;
                            lastMatch = competingIndexes.get(competingIndexes.size() - 1);
                        }
                        //limit on lookahead can be set here
                        while(i < hexRep.length() - 1 && !competingIndexes.isEmpty()) {
                            i++;
                            ListIterator<Integer> iter = competingIndexes.listIterator();
                            while (iter.hasNext()) {
                                if (hexRep.charAt(iter.next() + len) != hexRep.charAt(i)) {
                                    iter.remove();
                                }
                            }
                            if (iter.hasPrevious()) {
                                lastMatch = iter.previous();
                                len++;
                            }
                        }
                        if (lastMatch == -1) {
                            line.append(String.format("0,0,%c", hexRep.charAt(i)));
                        } else {
                            if (i != hexRep.length() - 1 || competingIndexes.isEmpty()) {
                                line.append(String.format("%d,%d,%c", i - len - lastMatch, len, hexRep.charAt(i)));
                            } else {
                                line.append(String.format("%d,%d,", i + 1 - len - lastMatch, len));
                            }
                        }
                        if (i != hexRep.length() - 1) {
                            line.append('~');
                        }
                        searchBuf.append(hexRep, i - len, ++i);
                    }
                    line.append('\n');
                    outputStream.write(line.toString().getBytes(Charset.defaultCharset()));
                }
                return true;
            }
        } catch(Exception e) {
            log.error("Something went wrong while encoding the file. Error message: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean decodeFile() {
        try {
            //processing this file
            File fileEnc = new File(ENCODE_DIR + ENCODE_FILE + ENCODE_FORMAT);
            FileUtils.cleanDirectory(new File(DECODE_DIR));
            //default charset is UTF-8
            try (BufferedReader parser = new BufferedReader(new InputStreamReader(new FileInputStream(fileEnc)))) {
                String line;
                line = parser.readLine();
                if (line == null) {
                    throw new IllegalStateException("File for decoding is corrupted!");
                }
                //TODO check if first line is a correct file extension
                DECODE_FORMAT = line;
                //writing into this one
                File fileDec = new File(DECODE_DIR + DECODE_FILE + DECODE_FORMAT);
                fileDec.createNewFile();
                String[] blocks;
                String[] content;
                try (FileOutputStream outputStream = new FileOutputStream(fileDec)) {
                    while ((line = parser.readLine()) != null && !Objects.equals(line, "\n")) {
                        //TODO hardcoded 8192*2 capacity, fix
                        StringBuilder stringBuilder = new StringBuilder(8192 * 2);
                        blocks = line.split("~");
                        for (String block : blocks) {
                            content = block.split(",",3);
                            int offset = Integer.parseInt(content[0]);
                            int length = Integer.parseInt(content[1]);
                            int start = stringBuilder.length() - offset;
                            if (start + length > stringBuilder.length()) {
                                for (int k = start; k < start + length; k++) {
                                    stringBuilder.append(stringBuilder.charAt(k));
                                }
                            } else {
                                stringBuilder.append(stringBuilder.substring(
                                        start,
                                        start + length
                                ));
                            }
                            stringBuilder.append(content[2]);
                        }
                        outputStream.write(HexConv.hexToBytes(stringBuilder.toString()));
                    }
                }
            }
            return true;
        } catch(Exception e) {
            log.error("Something went wrong while decoding the file. Error message: {}", e.getMessage());
            return false;
        }
    }
}
