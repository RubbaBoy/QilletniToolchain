package is.yarr.qilletni.toolchain.qll;

import is.yarr.qilletni.api.auth.ServiceProvider;
import is.yarr.qilletni.api.lib.Library;
import is.yarr.qilletni.toolchain.qll.classes.DirectoryClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NativeClassHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeClassHandler.class);

    public NativeClassOutput collectNativeClasses(Path outPath, Path javaBuildPath) throws IOException {
        String libraryClass = null;
        String providerClass = null;

        var classOutDir = outPath.resolve("native");
        Files.createDirectories(classOutDir);

        List<String> classNames;
        try (var walk = Files.walk(javaBuildPath, FileVisitOption.FOLLOW_LINKS)) {
            classNames = walk.filter(path -> path.getFileName().toString().endsWith(".class"))
                    .map(classFile -> {
                        var relativeBuildDir = javaBuildPath.relativize(classFile);
                        var classTarget = classOutDir.resolve(relativeBuildDir);

                        var className = relativeBuildDir.toString()
                                .replace(File.separator, ".")
                                .replace(".class", "");

                        try {
                            Files.createDirectories(classTarget.getParent());
                            Files.copy(classFile, classTarget);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }

                        return className;
                    }).toList();
        }
        
        LOGGER.debug("Moved {} .class files to {}", classNames.size(), classOutDir.toAbsolutePath());

        try (var loader = new DirectoryClassLoader(classOutDir)) {
            for (var className : classNames) {
                var clazz = loader.loadClassByName(className);

                if (Library.class.isAssignableFrom(clazz)) {
                    if (libraryClass != null) {
                        LOGGER.warn("Loading .qll with multiple library classes, ignoring {}", className);
                    } else {
                        libraryClass = className;
                    }
                }

                if (ServiceProvider.class.isAssignableFrom(clazz)) {
                    if (providerClass != null) {
                        LOGGER.warn("Loading .qll with multiple provider classes, ignoring {}", className);
                    } else {
                        providerClass = className;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error("Unable to load a native class", e);
        }

        if (libraryClass != null) {
            LOGGER.debug("Library class: {}", libraryClass);
        }

        if (providerClass != null) {
            LOGGER.debug("Provider class: {}", providerClass);
        }

        return new NativeClassOutput(libraryClass, providerClass);
    }

    public void deleteDirectory(Path file) {
        
    }

    public record NativeClassOutput(String libraryClass, String providerClass) {}

}
