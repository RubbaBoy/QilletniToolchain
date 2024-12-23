package is.yarr.qilletni.toolchain.command.run;

import is.yarr.qilletni.ServiceManager;
import is.yarr.qilletni.api.exceptions.QilletniException;
import is.yarr.qilletni.api.lib.qll.QllInfo;
import is.yarr.qilletni.lang.runner.QilletniProgramRunner;
import is.yarr.qilletni.lib.LibrarySourceFileResolver;
import is.yarr.qilletni.toolchain.qll.QllJarExtractor;
import is.yarr.qilletni.toolchain.qll.QllLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", description = "Runs a Qilletni program")
public class CommandRun implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRun.class);
    
    @CommandLine.Option(names = {"--dependency-path", "-d"}, description = "The directory that holds all dependencies")
    public Path dependencyPath;
    
//    @CommandLine.Option(names = {"--use-credentials", "-u"}, description = "All the credential files to use with available providers")
//    public List<String> useCredentials;
    
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;
    
    @CommandLine.Parameters(description = "The .ql file to run", index = "0")
    private Path file; // first is the file to run, after is the params
    
    @CommandLine.Parameters(description = "The program arguments", index = "1..")
    private List<String> args; // first is the file to run, after is the params

    @Override
    public Integer call() throws IOException {

        if (Files.notExists(file)) {
            LOGGER.error("Qilletni input file {} does not exist!", file.toAbsolutePath());
            return 1;
        }
        
        if (dependencyPath == null) {
            dependencyPath = getDependencyPath();
        }

        var tempRunDir = Files.createTempDirectory("ql-run");

        var qllLoader = new QllLoader();
        var qllJarExtractor = new QllJarExtractor();
        var librarySourceFileResolver = new LibrarySourceFileResolver();
        
        var loadedLibraries = new ArrayList<QllInfo>();

        try (var deps = Files.list(dependencyPath)) {
            deps.filter(path -> path.getFileName().toString().endsWith(".qll"))
                    .forEach(path -> {
                        qllJarExtractor.extractJarTo(path, tempRunDir);

                        try {
                            loadedLibraries.add(qllLoader.loadQll(librarySourceFileResolver, path));
                        } catch (IOException | URISyntaxException e) {
                            throw new RuntimeException(e);
                        }
                    });

        } catch (IOException e) {
            LOGGER.error("An exception occurred while reading dependencies", e);
        }

        var qllJarClassLoader = qllJarExtractor.createClassLoader();

        LOGGER.debug("Loaded libraries!");

        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(qllJarClassLoader);

//            System.out.println("loadedLibraries = " + loadedLibraries);
//
//            System.out.println(loadedLibraries.stream()
//                    .map(QllInfo::providerClass)
//                    .filter(Objects::nonNull).toList());
//
//            try {
//                System.out.println(qllJarClassLoader.loadClass("is.yarr.qilletni.music.spotify.provider.SpotifyServiceProvider"));
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
            
            var dynamicProvider = ServiceManager.createDynamicProvider(loadedLibraries);

            var runner = new QilletniProgramRunner(dynamicProvider, librarySourceFileResolver, loadedLibraries);

            runner.importInitialFiles();

            try {
                runner.runProgram(file);
            } catch (QilletniException | IOException e) {
                LOGGER.error("An exception occurred while running " + file.getFileName(), e);
                runner.shutdown();
                return 1;
            }

            runner.shutdown();
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }

        return 0;
    }
    
    private Path getDependencyPath() throws IOException {
        var userHome = System.getProperty("user.home");
        
        var qilletniDir = Paths.get(userHome, ".qilletni", "libraries");

        // 3. Create the directory if it doesn't exist (including any parent dirs)
        Files.createDirectories(qilletniDir);
        
        return qilletniDir;
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
