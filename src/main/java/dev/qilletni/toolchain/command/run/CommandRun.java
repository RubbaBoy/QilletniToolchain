package dev.qilletni.toolchain.command.run;

import dev.qilletni.api.exceptions.QilletniException;
import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.impl.ServiceManager;
import dev.qilletni.impl.lang.runner.QilletniProgramRunner;
import dev.qilletni.impl.lib.LibrarySourceFileResolver;
import dev.qilletni.toolchain.LogSetup;
import dev.qilletni.toolchain.PathUtility;
import dev.qilletni.toolchain.qll.LibraryValidator;
import dev.qilletni.toolchain.qll.QllJarExtractor;
import dev.qilletni.toolchain.qll.QllLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "Runs a Qilletni program")
public class CommandRun implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRun.class);
    
    @CommandLine.Option(names = {"--dependency-path", "-d"}, description = "The directory that holds all dependencies")
    public Path dependencyPath;
    
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"--local-library", "-l"}, description = "If running a library example, the path of the library root it's in")
    private Path localLibrary;

    @CommandLine.Option(names = {"--log-port", "-p"}, defaultValue = "-1", description = "The port to use for logging")
    private int logPort;
    
    @CommandLine.Parameters(description = "The .ql file to run", index = "0")
    private Path file; // first is the file to run, after is the params
    
    @CommandLine.Parameters(description = "The program arguments", index = "1..")
    private List<String> args; // first is the file to run, after is the params

    @Override
    public Integer call() throws IOException {

        if (logPort > 0) {
            LogSetup.setupLogSocket(logPort);
        }

        if (Files.notExists(file)) {
            LOGGER.error("Qilletni input file {} does not exist!", file.toAbsolutePath());
            return 1;
        }
        
        if (dependencyPath == null) {
            dependencyPath = PathUtility.getDependencyPath();
        }

        var tempRunDir = Files.createTempDirectory("ql-run");

        var qllLoader = new QllLoader();
        var qllJarExtractor = new QllJarExtractor();
        var librarySourceFileResolver = new LibrarySourceFileResolver();
        
        var loadedLibraries = new ArrayList<QllInfo>();
        QllInfo localLibraryQll = null;

        if (localLibrary != null) {
            LOGGER.info("Loading local library at {}", localLibrary);
            localLibraryQll = qllLoader.loadLocalLibrary(librarySourceFileResolver, localLibrary);
            loadedLibraries.add(localLibraryQll);
        }

        var localLibraryName = localLibraryQll != null ? localLibraryQll.name() : null;

        try (var deps = Files.list(dependencyPath)) {
            deps.filter(path -> path.getFileName().toString().endsWith(".qll"))
                    .forEach(path -> {
                        qllJarExtractor.extractJarTo(path, tempRunDir);

                        try {
                            var loadedQll = qllLoader.loadQll(librarySourceFileResolver, path);

                            if (loadedQll.name().equals(localLibraryName)) {
                                LOGGER.debug("Skipping loading local library {} from dependencies", localLibraryName);
                                return;
                            }

                            loadedLibraries.add(loadedQll);
                        } catch (IOException | URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    });

        } catch (IOException e) {
            LOGGER.error("An exception occurred while reading dependencies", e);
        }

        var qllJarClassLoader = qllJarExtractor.createClassLoader();

        var libraryValidator = new LibraryValidator(loadedLibraries);
        if (!libraryValidator.validate()) {
            LOGGER.error("Exiting due to unmet dependencies");
            return 1;
        }

        LOGGER.debug("Loaded libraries!");

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(qllJarClassLoader);
            
            var dynamicProvider = ServiceManager.createDynamicProvider(loadedLibraries);

            var runner = new QilletniProgramRunner(dynamicProvider, librarySourceFileResolver, loadedLibraries);

            LOGGER.debug("Importing initial files");
            
            runner.importInitialFiles();

            try {
                LOGGER.debug("Running program: {}", file.getFileName());
                runner.runProgram(file);
            } catch (QilletniException | IOException e) {
                LOGGER.error("An exception occurred while running {}", file.getFileName(), e);
                runner.shutdown();
                return 1;
            }

            runner.shutdown();
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }

        return 0;
    }

    @Override
    public String toString() {
        return "CommandRunArgs{" +
                "helpRequested=" + helpRequested +
                ", file=" + file +
                ", args=" + args +
                ", dependencyPath='" + dependencyPath + '\'' +
                '}';
    }
}
