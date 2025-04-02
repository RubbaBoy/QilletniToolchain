package dev.qilletni.toolchain.qll;

import com.google.gson.Gson;
import dev.qilletni.api.lib.qll.QllInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class QllInfoGenerator {
    
    private static final Gson gson = new Gson();
    
    public void writeQllInfo(QllInfo qllInfo, Path destinationDir) throws IOException {
        var json = gson.toJson(qllInfo);
        
        // TODO: Gson adapters for versions
        Files.writeString(destinationDir.resolve("qll.info"), json);
    }
    
    public QllInfo readQllInfo(InputStream qllInfoPath) {
        return gson.fromJson(new InputStreamReader(qllInfoPath), QllInfo.class);
    }
    
}
