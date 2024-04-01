package is.yarr.qilletni.toolchain.qll;

import com.google.gson.Gson;
import is.yarr.qilletni.toolchain.config.QllInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class QllInfoGenerator {
    
    private static final Gson gson = new Gson();
    
    public void writeQllInfo(QllInfo qllInfo, Path destinationDir) throws IOException {
        var json = gson.toJson(qllInfo);
        
        // TODO: Gson adapters for versions
        Files.writeString(destinationDir.resolve("qll.info"), json);
    }
    
}
