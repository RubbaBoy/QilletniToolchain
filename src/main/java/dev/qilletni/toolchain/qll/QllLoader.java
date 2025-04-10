package dev.qilletni.toolchain.qll;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.impl.lib.LibrarySourceFileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class QllLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QllLoader.class);
    
    public QllInfo loadQll(LibrarySourceFileResolver librarySourceFileResolver, Path qllPath) throws IOException, URISyntaxException {
        LOGGER.debug("Loading {}", qllPath);
        
        QllInfo qllInfo;

        var zipUri = URI.create("jar:" + qllPath.toUri());
        var qllInfoGenerator = new QllInfoGenerator();

        try (var zipFile = new ZipFile(qllPath.toFile());
             var zipFs = FileSystems.newFileSystem(zipUri, Collections.emptyMap())) {
            qllInfo = qllInfoGenerator.readQllInfo(zipFile.getInputStream(zipFile.getEntry("qll.info")));
            
            LOGGER.debug("qllInfo = {}", qllInfo);
            
//            qllJarClassLoader.loadJar(zipFile.getInputStream(zipFile.getEntry("native.jar")));

            var srcPathInZip = zipFs.getPath("src");
            
            var sourceMap = new HashMap<String, String>();
            
            zipFile.stream().filter(Predicate.not(ZipEntry::isDirectory))
                    .forEach(entry -> {
                        String normalizedName = entry.getName().replace("\\", "/");

                        // Get a Path object *within the Zip FileSystem*
                        // This Path object will correctly use '/' separators internally
                        Path entryPathInZip = zipFs.getPath(normalizedName);

                        Path fileName = entryPathInZip.getFileName();
                        Path parentDir = entryPathInZip.getParent(); // null for entries in root

                        // Determine the "root" component (first directory name)
                        var root = "";
                        if (entryPathInZip.getNameCount() > 1) {
                            // Get the first path element (e.g., "src" from "src/core.ql")
                            root = entryPathInZip.getName(0).toString();
                        }

                        // Handle cases where the entry might not be under "src"
                        var relative = "N/A";
                        
                        try {
                            if (parentDir != null && parentDir.startsWith(srcPathInZip)) {
                                relative = srcPathInZip.relativize(entryPathInZip).toString();
                            } else if (entryPathInZip.startsWith(srcPathInZip) && entryPathInZip.getNameCount() == 1) {
                                // Handle files directly inside src, like "src/somefile.ql"
                                relative = entryPathInZip.getFileName().toString(); // Or srcPathInZip.relativize(entryPathInZip)
                            } else if (entryPathInZip.startsWith(srcPathInZip)) {
                                relative = srcPathInZip.relativize(entryPathInZip).toString();
                            }
                            
                            // If it's not under src, keep relative="N/A" or handle differently
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Cannot relativize {} against {}", entryPathInZip, srcPathInZip);
                        }

                        // Logging using Path components
                        LOGGER.debug("Entry: {} (Path: {}) is of: {}, {} ({})",
                                entry.getName(),
                                entryPathInZip,
                                fileName,
                                root,
                                relative);
                        
                        if (fileName.toString().endsWith(".ql") && root.equals("src")) {
                            try (var is = zipFile.getInputStream(entry)) {
                                sourceMap.put(relative.replace("\\", "/"), new String(is.readAllBytes()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            //                LOGGER.debug("Getting file: {}   where all files are: {}", key, sourceMap.keySet());
            //                var got = sourceMap.get(key);
            //                LOGGER.debug("\t^ got: {}", got.substring(0, Math.min(10, got.length())));
            librarySourceFileResolver.addLibraryResolver(qllInfo.name(), sourceMap::get);
        }

        return qllInfo;
    }
}
