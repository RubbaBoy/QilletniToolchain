package dev.qilletni.toolchain.qll;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QllJarExtractor {
    
    private final List<URL> extractedJars = new ArrayList<>();
    
    public void extractJarTo(Path jarPath, Path destinationPath) {
        var destinationFile = destinationPath.resolve(createJarName(jarPath.getFileName().toString()));
        
        try (var fileSystem = FileSystems.newFileSystem(jarPath)) {
            var path = fileSystem.getPath("native.jar");
            
            // Not all libraries have native methods
            if (Files.notExists(path)) {
                return;
            }
            
            Files.copy(path, destinationFile);

            extractedJars.add(destinationFile.toUri().toURL());
            
//            return new URLClassLoader(new URL[]{destinationFile.toUri().toURL()}, parentClassLoader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public URLClassLoader createClassLoader() {
        return new URLClassLoader(extractedJars.toArray(URL[]::new));
    }
    
    private String createJarName(String qllName) {
        if (!qllName.endsWith(".qll")) {
            return qllName;
        }
        
        return qllName.substring(0, qllName.length() - 4) + ".jar";
    }
    
}
