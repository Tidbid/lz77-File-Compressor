package com.romanov.lz77;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;

@SpringBootApplication
public class Lz77Application {

    public static String FILES_DIR;

    public static String ENCODE_DIR;

    public static String DECODE_DIR;

    public static String ENCODE_FILE = "encode";

    public static String DECODE_FILE = "decode";

    public static String ENCODE_FORMAT = ".lz77";

    public static String DECODE_FORMAT;

    public static void main(String[] args) throws IOException {
        FILES_DIR = new File(".").getCanonicalPath() + File.separator + "arch" + File.separator;
        ENCODE_DIR = FILES_DIR + "encode" + File.separator;
        DECODE_DIR = FILES_DIR + "decode" + File.separator;
        File encodeDir = new File(ENCODE_DIR), decodeDir = new File(DECODE_DIR);
        if (!encodeDir.exists()) {
            encodeDir.mkdirs();
        }
        if (!decodeDir.exists()) {
            decodeDir.mkdirs();
        }
        SpringApplication.run(Lz77Application.class, args);
    }

}
