package is.yarr.qilletni.toolchain.config;

import is.yarr.qilletni.toolchain.exceptions.InvalidVersionException;
import is.yarr.qilletni.toolchain.exceptions.QilletniInfoFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class QilletniInfoParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniInfoParser.class);

    /**
     * The name of the Qilletni program/library info file, which has an extension in the
     * {@link #QILLETNI_FILE_EXTENSIONS} array.
      */
    private static final String QILLETNI_INFO = "qilletni_info";

    /**
     * The potential extensions of the file of name {@link #QILLETNI_INFO}.
     */
    private static final String[] QILLETNI_FILE_EXTENSIONS = {"yml", "yaml"};

    /**
     * Locates a {@link #QILLETNI_INFO} file directly in the given directory, and returns its path.
     * 
     * @param directory The direct parent of the info file
     * @return The {@code qilletni_info} file, if it exists
     */
    public Optional<Path> findQilletniInfoFile(Path directory) {
        for (var fileExtension : QILLETNI_FILE_EXTENSIONS) {
            var qilletniInfoFile = directory.resolve(String.format("%s.%s", QILLETNI_INFO, fileExtension));

            if (Files.exists(qilletniInfoFile)) {
                return Optional.of(qilletniInfoFile);
            }
        }
        
        return Optional.empty();
    }

    /**
     * Reads the {@link #QILLETNI_INFO} file in the given Qilletni source parent directory.
     * 
     * @param qilletniDirectory The parent directory of the {@link #QILLETNI_INFO} file and all {@code .ql} source
     *                          files.
     * @return The read {@link #QILLETNI_INFO} file data
     * @throws IOException
     */
    public QilletniInfoData readQilletniInfo(Path qilletniDirectory) throws IOException {
        var qilletniInfoFile = findQilletniInfoFile(qilletniDirectory)
                .orElseThrow(() -> new FileNotFoundException(QILLETNI_INFO + " file not found!"));

        var yaml = new Yaml();
        Map<String, Object> obj = yaml.load(Files.newInputStream(qilletniInfoFile));

        var nameString = (String) Objects.requireNonNull(obj.get("name"), "'name' required in qilletni_info");
        var authorString = (String) Objects.requireNonNull(obj.get("author"), "'author' required in qilletni_info");
        
        var version = Version.parseVersionString((String) Objects.requireNonNull(obj.get("version"), "'version' required in qilletni_info"))
                .orElseThrow(() -> new InvalidVersionException("Invalid version"));
        
        var dependencies = (List<String>) obj.getOrDefault("dependencies", Collections.emptyList());
        
        var dependencyList = dependencies.stream().map(dependencyString -> {
            var dependencySplit = dependencyString.split(":");
            
            if (dependencySplit.length != 2) {
                throw new QilletniInfoFormatException("Dependencies require a name and a version");
            }
            
            var dependencyName = dependencySplit[0];
            var dependencyVersion = ComparableVersion.parseComparableVersionString(dependencySplit[1])
                    .orElseThrow(() -> new QilletniInfoFormatException("Invalid version for dependency " + dependencyName));
            
            return new QilletniInfoData.Dependency(dependencyName, dependencyVersion);
        }).toList();
        
        return new QilletniInfoData(nameString, version, authorString, dependencyList);
    }
    
}
