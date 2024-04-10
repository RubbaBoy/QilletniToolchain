package is.yarr.qilletni.toolchain.command.run;

import is.yarr.qilletni.ServiceManager;
import is.yarr.qilletni.api.auth.ServiceProvider;
import is.yarr.qilletni.api.exceptions.QilletniException;
import is.yarr.qilletni.api.lib.Library;
import is.yarr.qilletni.lang.runner.QilletniProgramRunner;
import is.yarr.qilletni.lib.LibrarySourceFileResolver;
import is.yarr.qilletni.toolchain.qll.QllJarClassLoader;
import is.yarr.qilletni.toolchain.qll.QllLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

//@Parameters(commandDescription = "Run a qilletni program")
@CommandLine.Command(name = "run", description = "Runs a Qilletni program")
public class CommandRun implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRun.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;
    
    @CommandLine.Parameters(description = "The .ql file to run", index = "0")
    private Path file; // first is the file to run, after is the params
    
    @CommandLine.Parameters(description = "The program arguments", index = "1..")
    private List<String> args; // first is the file to run, after is the params
    
    @CommandLine.Option(names = {"--dependency-path", "-d"}, description = "The directory that holds all dependencies", required = true)
    public Path dependencyPath; // TODO: Make default
    
    @CommandLine.Option(names = {"--provider-cache", "-c"}, description = "The directory to providers' cache files")
    public String providerCache;
    
    @CommandLine.Option(names = {"--use-credentials", "-u"}, description = "All the credential files to use with available providers")
    public List<String> useCredentials;
    
    @Override
    public Integer call() {

        if (Files.notExists(file)) {
            LOGGER.error("Qilletni input file {} does not exist!", file.toAbsolutePath());
            return 1;
        }

        var qllLoader = new QllLoader();
        var qllJarClassLoader = new QllJarClassLoader();
        var librarySourceFileResolver = new LibrarySourceFileResolver();

        try (var deps = Files.list(dependencyPath)) {
            deps.filter(path -> path.getFileName().toString().endsWith(".qll"))
                    .forEach(path -> {
                        try {
                            qllLoader.loadQll(qllJarClassLoader, librarySourceFileResolver, path);
                        } catch (IOException | URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    });

        } catch (IOException e) {
            LOGGER.error("An exception occurred while reading dependencies", e);
        }
        
        LOGGER.debug("Loaded libraries!");

        var dynamicProvider = ServiceManager.createDynamicProvider(qllJarClassLoader);
        var runner = new QilletniProgramRunner(dynamicProvider, librarySourceFileResolver, qllJarClassLoader);

        runner.importInitialFiles();
        
        try {
            runner.runProgram(file);
        } catch (QilletniException | IOException e) {
            LOGGER.error("An exception occurred while running " + file.getFileName(), e);
            return 1;
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
                ", providerCache='" + providerCache + '\'' +
                ", useCredentials=" + useCredentials +
                '}';
    }
}
