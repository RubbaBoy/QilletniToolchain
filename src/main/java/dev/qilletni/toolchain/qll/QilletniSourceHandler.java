package dev.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;

public class QilletniSourceHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniSourceHandler.class);

    public void moveQilletniSource(Path outPath, Path qilletniSourcePath) throws IOException {
        var sourceOutDir = outPath.resolve("src");
        Files.createDirectories(sourceOutDir);

        try (var walk = Files.walk(qilletniSourcePath, FileVisitOption.FOLLOW_LINKS)) {
            var filesMoved = walk.filter(path -> path.getFileName().toString().endsWith(".ql"))
                    .map(qilletniFile -> {
                        var relativeBuildDir = qilletniSourcePath.relativize(qilletniFile);
                        var classTarget = sourceOutDir.resolve(relativeBuildDir);

                        try {
                            Files.createDirectories(classTarget.getParent());
                            Files.copy(qilletniFile, classTarget);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }

                        return 1;
                    }).mapToInt(i -> i).sum();

            LOGGER.debug("Moved {} .class files to {}", filesMoved, sourceOutDir.toAbsolutePath());
        }
    }
}
