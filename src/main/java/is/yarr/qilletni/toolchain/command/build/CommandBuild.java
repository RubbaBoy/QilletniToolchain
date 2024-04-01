package is.yarr.qilletni.toolchain.command.build;

import is.yarr.qilletni.toolchain.FileUtil;
import is.yarr.qilletni.toolchain.config.QilletniInfoParser;
import is.yarr.qilletni.toolchain.config.QllInfo;
import is.yarr.qilletni.toolchain.qll.NativeClassHandler;
import is.yarr.qilletni.toolchain.qll.QilletniSourceHandler;
import is.yarr.qilletni.toolchain.qll.QllInfoGenerator;
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
    
    @CommandLine.Option(names = {"--java-path", "-j"}, description = "The directory that contains build Java .class files")
    public Path javaBuildPath;
    
    @CommandLine.Option(names = {"--dependency-path", "-d"}, description = "The directory that contains all .qll dependencies")
    public Path dependencyPath;

    @Override
    public Integer call() throws IOException {
        LOGGER.debug("Called build! {}", this);

        LOGGER.debug("Java .class files are in: {}", javaBuildPath);
        LOGGER.debug("Qilletni .ql files are in: {}", sourcePath);
        LOGGER.debug("Libraries are located in: {}", dependencyPath);

        var infoParser = new QilletniInfoParser();
        var qilletniInfo = infoParser.readQilletniInfo(sourcePath);

        LOGGER.debug("Qilletni Info = {}", qilletniInfo);

        var nativeClassHandler = new NativeClassHandler();
        var qilletniSourceHandler = new QilletniSourceHandler();

        var outParent = sourcePath.getParent().resolve("qilletni-out");
        FileUtil.deleteDirectory(outParent);
        Files.createDirectories(outParent);

        var analyzed = nativeClassHandler.collectNativeClasses(outParent, javaBuildPath);

        qilletniSourceHandler.moveQilletniSource(outParent, sourcePath);

        LOGGER.debug("analyzed = {}", analyzed);

        var qllInfoGenerator = new QllInfoGenerator();
        qllInfoGenerator.writeQllInfo(new QllInfo(qilletniInfo, analyzed.libraryClass(), analyzed.providerClass()), outParent);
        
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
