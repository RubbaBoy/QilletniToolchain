package is.yarr.qilletni.toolchain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {

    public static void deleteDirectory(Path file) {
        try {
            if (Files.isDirectory(file)) {
                try (var contents = Files.list(file)) {
                    contents.forEach(FileUtil::deleteDirectory);
                }
            }

            Files.delete(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void clearAndCreateDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                deleteDirectory(directory);
            }
            
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
