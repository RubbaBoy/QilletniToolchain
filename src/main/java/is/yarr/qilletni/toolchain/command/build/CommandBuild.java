package is.yarr.qilletni.toolchain.command.build;

import is.yarr.qilletni.toolchain.FileUtil;
import is.yarr.qilletni.toolchain.config.QilletniInfoParser;
import is.yarr.qilletni.toolchain.config.QllInfo;
import is.yarr.qilletni.toolchain.qll.NativeClassHandler;
import is.yarr.qilletni.toolchain.qll.QilletniSourceHandler;
import is.yarr.qilletni.toolchain.qll.QllInfoGenerator;
import is.yarr.qilletni.toolchain.qll.QllPackager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "build", description = "Build a Qilletni library")
public class CommandBuild implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBuild.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;
    
    @CommandLine.Parameters(description = "The directory that contains source Qilletni .ql files", index = "0")
    public Path sourcePath;
    
    @CommandLine.Option(names = {"--java-path", "-j"}, description = "The modular .jar path of native implementations")
    public Path javaBuildPath;
    
    @CommandLine.Option(names = {"--dependency-path", "-d"}, description = "The directory that contains all .qll dependencies")
    public Path dependencyPath;
    
    // Ends in .qll: use as file. Otherwise, use as destination directory
    @CommandLine.Option(names = {"--output-path", "-o"}, description = "The destination directory or .qll file output of the library")
    public Path outputFilePath;

    @Override
    public Integer call() throws IOException {
        LOGGER.debug("Called build! {}", this);

        LOGGER.debug("Java .jar is: {}", javaBuildPath);
        LOGGER.debug("Qilletni .ql files are in: {}", sourcePath);
        LOGGER.debug("Libraries are located in: {}", dependencyPath);
        LOGGER.debug("Output directory/file: {}", outputFilePath);

        var infoParser = new QilletniInfoParser();
        var qilletniInfo = infoParser.readQilletniInfo(sourcePath);

        LOGGER.debug("Qilletni Info = {}", qilletniInfo);

        var nativeClassHandler = new NativeClassHandler(qilletniInfo.name());
        var qilletniSourceHandler = new QilletniSourceHandler();

        var outParent = sourcePath.getParent().resolve("qilletni-out");
        FileUtil.deleteDirectory(outParent);
        Files.createDirectories(outParent);

        var analyzed = nativeClassHandler.collectNativeClasses(outParent, javaBuildPath);

        qilletniSourceHandler.moveQilletniSource(outParent, sourcePath);

        LOGGER.debug("analyzed = {}", analyzed);

        var qllInfoGenerator = new QllInfoGenerator();
        qllInfoGenerator.writeQllInfo(new QllInfo(qilletniInfo, analyzed.libraryClass(), analyzed.providerClass()), outParent);
        
        var defaultQllFileName = "%s-%s.qll".formatted(qilletniInfo.name(), qilletniInfo.version().getVersionString());
        var destinationFile = sourcePath.resolve(defaultQllFileName);

        if (outputFilePath != null) {
            if (outputFilePath.getFileName().toString().endsWith(".qll")) {
                Files.createDirectories(outputFilePath.getParent());
                Files.deleteIfExists(outputFilePath);
                destinationFile = outputFilePath;
            } else {
                // Is a parent directory
                Files.createDirectories(outputFilePath);
                destinationFile = outputFilePath.resolve(destinationFile);
            }
        }
        
        LOGGER.debug("Destination .qll file: {}", destinationFile);
        
        LOGGER.debug("Writing {}.qll", qilletniInfo.name());

        var qllPackager = new QllPackager();
        
        qllPackager.packageQll(outParent, destinationFile);

        return 0;
    }

    @Override
    public String toString() {
        return "CommandBuildArgs{" +
                "helpRequested=" + helpRequested +
                ", sourcePath=" + sourcePath +
                ", javaBuildPath=" + javaBuildPath +
                ", dependencyPath=" + dependencyPath +
                '}';
    }
}
