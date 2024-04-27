package is.yarr.qilletni.toolchain.qll;

import is.yarr.qilletni.lib.LibrarySourceFileResolver;
import is.yarr.qilletni.toolchain.config.QllInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class QllLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QllLoader.class);
    
    private static final Path SRC_PATH = Paths.get("src");
    
    public QllInfo loadQll(LibrarySourceFileResolver librarySourceFileResolver, Path qllPath) throws IOException, URISyntaxException {
        LOGGER.debug("Loading {}", qllPath);
        
        QllInfo qllInfo;

        var qllInfoGenerator = new QllInfoGenerator();

        try (var zipFile = new ZipFile(qllPath.toFile())) {
            qllInfo = qllInfoGenerator.readQllInfo(zipFile.getInputStream(zipFile.getEntry("qll.info")));
            
            LOGGER.debug("qllInfo = {}", qllInfo);
            
//            qllJarClassLoader.loadJar(zipFile.getInputStream(zipFile.getEntry("native.jar")));
            
            var sourceMap = new HashMap<String, String>();
            
            zipFile.stream().filter(Predicate.not(ZipEntry::isDirectory))
                    .forEach(entry -> {
                        var name = Path.of(entry.getName());
                        var root = getRoot(name);
                        LOGGER.debug("Entry: {} is of: {}, {} ({})", entry, name.getFileName(), root, SRC_PATH.relativize(name));
                        
                        if (name.getFileName().toString().endsWith(".ql") && root.equals("src")) {
                            try {
                                sourceMap.put(SRC_PATH.relativize(name).toString().replace("\\", "/"), new String(zipFile.getInputStream(entry).readAllBytes()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
            
            librarySourceFileResolver.addLibraryResolver(qllInfo.name(), key -> {
//                LOGGER.debug("Getting file: {}   where all files are: {}", key, sourceMap.keySet());
//                var got = sourceMap.get(key);
//                LOGGER.debug("\t^ got: {}", got.substring(0, Math.min(10, got.length())));
                return sourceMap.get(key);
            });
        }

        return qllInfo;
    }
    
    private String getRoot(Path path) {
        if (path.getNameCount() > 0) {
            return path.getName(0).toString();
        }

        return path.getFileName().toString();
    }
    
}
