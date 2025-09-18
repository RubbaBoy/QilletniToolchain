package dev.qilletni.toolchain.command.init;

import dev.qilletni.toolchain.init.ProjectInit;
import dev.qilletni.toolchain.init.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "init", description = "Initializes a Qilletni project")
public class CommandInit implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInit.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @CommandLine.Parameters(description = "The directory to initialize the project in", index = "0")
    public Path sourcePath;

    @CommandLine.Option(names = {"--name", "-n"}, description = "The name of the project")
    public String projectName;

    @CommandLine.Option(names = {"--author", "-a"}, description = "The author of the project")
    public String authorName;

    @CommandLine.Option(names = {"--native-class", "-c"}, description = "The native canonical class name to initialize a project with. If no class is specified, native bindings will not be set up.")
    public String nativeClass;

    @CommandLine.Option(names = {"--no-native", "-o"}, description = "Do not set up native bindings (noninteractive)")
    public boolean noNative;

    @CommandLine.Option(names = {"--type", "-t"}, description = "The type of the project to initialize, either 'library' or 'application'", defaultValue = "application")
    public String projectType;
    
    @Override
    public Integer call() throws Exception {
        LOGGER.debug("Initializing in: {}", sourcePath);

        ProjectType projectTypeEnum;
        
        if ("library".equals(projectType)) {
            LOGGER.debug("Creating library project");
            projectTypeEnum = ProjectType.LIBRARY;
        } else if ("application".equals(projectType)) {
            LOGGER.debug("Creating application project");
            projectTypeEnum = ProjectType.APPLICATION;
        } else {
            LOGGER.error("Invalid project type: {}", projectType);
            return 1;
        }

        var scanner = new Scanner(System.in);
        
        if (projectName == null) {
            System.out.println("What is the project name?");
            projectName = scanner.nextLine().trim();
        }
        
        if (authorName == null) {
            System.out.println("Who is the author?");
            authorName = scanner.nextLine().trim();
        }
        
        if (!noNative && nativeClass == null) {
            System.out.println("What is the fully qualified native Java class name? (e.g. dev.qilletni.lib.Demo, Leave blank for no native bindings)");
            var receivedNativeClass = scanner.nextLine().trim();
            if (!receivedNativeClass.isBlank()) {
                nativeClass = receivedNativeClass;
            }
        }

        ProjectInit.NativeInit nativeInit = null;
        
        if (nativeClass != null) {
            var splitClass = nativeClass.split("\\.");
            var packageName = String.join(".", Arrays.copyOf(splitClass, splitClass.length - 1));
            var className = splitClass[splitClass.length - 1];
            
            LOGGER.debug("Creating project with native package of {} and class name of {}", packageName, className);
            
            nativeInit = new ProjectInit.NativeInit(packageName, className);
        }

        new ProjectInit().initialize(sourcePath.toAbsolutePath(), projectName, authorName, nativeInit, projectTypeEnum);

        
        return 0;
    }
}
