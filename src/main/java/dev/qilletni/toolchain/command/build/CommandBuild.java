package dev.qilletni.toolchain.command.build;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.toolchain.FileUtil;
import dev.qilletni.toolchain.config.QilletniInfoParser;
import dev.qilletni.toolchain.qll.QilletniSourceHandler;
import dev.qilletni.toolchain.qll.QllInfoGenerator;
import dev.qilletni.toolchain.qll.QllPackager;
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
    
    @CommandLine.Option(names = {"--build-directory", "-b"}, description = "The build directory", required = true)
    public Path buildDirectory;
    
    // Ends in .qll: use as file. Otherwise, use as destination directory
    @CommandLine.Option(names = {"--output-file", "-o"}, description = "The directory or file name of the build .qll")
    public Path outputFilePath;
    
//    @CommandLine.Option(names = {"--compile-jar", "-c"}, description = "If Qilletni should compile the Java .jar before packaging")
//    public boolean buildJar;
//    
//    @CommandLine.Option(names = {"--java-compile-cmd", "-p"}, defaultValue = "./gradlew :shadowJar", description = "The command to build the Java .jar")
//    public String javaBuildCmd;

    @Override
    public Integer call() throws IOException {
        LOGGER.debug("Called build! {}", this);

        LOGGER.debug("Java .jar is: {}", javaBuildPath);
        LOGGER.debug("Qilletni .ql files are in: {}", sourcePath);
        LOGGER.debug("Libraries are located in: {}", dependencyPath);
        LOGGER.debug("Build directory: {}", buildDirectory);
        LOGGER.debug("Output file/directory: {}", outputFilePath);

        var infoParser = new QilletniInfoParser();
        var qilletniInfo = infoParser.readQilletniInfo(sourcePath);

        LOGGER.debug("Qilletni Info = {}", qilletniInfo);

        var qilletniSourceHandler = new QilletniSourceHandler();
        
        var qllBuildPath = buildDirectory.resolve("ql-build");

        FileUtil.clearAndCreateDirectory(qllBuildPath);
        
        if (javaBuildPath != null) {
//            if (buildJar) {
//                LOGGER.debug("Building Java .jar with command: {}", javaBuildCmd);
//                var process = new ProcessBuilder(javaBuildCmd.split(" "))
//                        .directory(javaBuildPath.toFile())
//                        .inheritIO()
//                        .start();
//                
//                try {
//                    process.waitFor();
//                } catch (InterruptedException e) {
//                    LOGGER.error("Java .jar build process was interrupted!", e);
//                    return 1;
//                }
//            }
            
            if (Files.notExists(javaBuildPath)) {
                LOGGER.error("{} not found! Ensure you have ran `gradle :shadowJar` on the library first", javaBuildPath);
                return 1;
            }

            Files.copy(javaBuildPath, qllBuildPath.resolve("native.jar"));
        }

//        var analyzed = nativeClassHandler.collectNativeClasses(qllBuildPath, javaBuildPath);

        qilletniSourceHandler.moveQilletniSource(qllBuildPath, sourcePath);

//        LOGGER.debug("analyzed = {}", analyzed);

        var qllInfoGenerator = new QllInfoGenerator();
        qllInfoGenerator.writeQllInfo(new QllInfo(qilletniInfo), qllBuildPath);
        
        var defaultQllFileName = "%s-%s.qll".formatted(qilletniInfo.name(), qilletniInfo.version().getVersionString());
        Path destinationFile;

        if (outputFilePath != null) {
            if (outputFilePath.getFileName().toString().endsWith(".qll")) {
                Files.createDirectories(outputFilePath.getParent());
                Files.deleteIfExists(outputFilePath);
                destinationFile = outputFilePath;
            } else {
                // Is a parent directory
                Files.createDirectories(outputFilePath);
                destinationFile = outputFilePath.resolve(defaultQllFileName);
            }
        } else {
            var qllOutPath = buildDirectory.resolve("qll");
            FileUtil.clearAndCreateDirectory(qllOutPath);
            
            destinationFile = qllOutPath.resolve(defaultQllFileName);;
        }
        
        LOGGER.debug("Writing {}", destinationFile);
        
        var qllPackager = new QllPackager();
        
        qllPackager.packageQll(qllBuildPath, destinationFile);
        
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
