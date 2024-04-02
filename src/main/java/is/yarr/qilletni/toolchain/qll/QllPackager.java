package is.yarr.qilletni.toolchain.qll;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class QllPackager {

    public void packageQll(Path qllDirectoryPath, Path qllDestination) throws IOException {
        try (
                var fos = Files.newOutputStream(qllDestination);
                var zos = new ZipOutputStream(fos);
                var walking = Files.walk(qllDirectoryPath)) {
            
            walking.filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        var zipEntry = new ZipEntry(qllDirectoryPath.relativize(path).toString());

                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
}
