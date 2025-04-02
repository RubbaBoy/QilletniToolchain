package dev.qilletni.toolchain.command.doc;

import dev.qilletni.toolchain.config.QilletniInfoParser;
import dev.qilletni.toolchain.docs.DocumentationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "doc", description = "Generated HTML docs for Qilletni")
public class CommandDoc implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDoc.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Parameters(description = "The directory that contains source Qilletni .ql files, and a qilletni_info.yml", index = "0")
    public Path sourcePath;

    @CommandLine.Option(names = {"--output-file", "-o"}, description = "The directory to put the generated docs in")
    public Path outputFilePath;

    @CommandLine.Option(names = {"--cache-path", "-c"}, description = "The directory containing the cache of the docs")
    public Path cachePath;
    
    @Override
    public Integer call() throws Exception {
        LOGGER.debug("Generating docs from: {}", sourcePath);
        LOGGER.debug("Doc output path: {}", outputFilePath);
        
        if (cachePath == null) {
            cachePath = getCachePath();
        }
        
        LOGGER.debug("Cache path: {}", cachePath);
        
        var infoParser = new QilletniInfoParser();
        var qilletniInfo = infoParser.readQilletniInfo(sourcePath);

        var documentationOrchestrator = new DocumentationOrchestrator();
        return documentationOrchestrator.beginDocGen(qilletniInfo, cachePath, sourcePath, outputFilePath);
    }

    private Path getCachePath() throws IOException {
        var userHome = System.getProperty("user.home");

        var qilletniDir = Paths.get(userHome, ".qilletni", "doc-cache");

        Files.createDirectories(qilletniDir);

        return qilletniDir;
    }
}
