package dev.qilletni.toolchain.command.build;

import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.toolchain.FileUtil;
import dev.qilletni.toolchain.PathUtility;
import dev.qilletni.toolchain.config.QilletniInfoParser;
import dev.qilletni.toolchain.qll.GradleProjectHelper;
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

    @CommandLine.Parameters(description = "The root directory of the project", index = "0", defaultValue = ".")
    public Path projectRoot;

    // Ends in .qll: use as file. Otherwise, use as a destination directory
    @CommandLine.Option(names = {"--output-file", "-o"}, description = "The directory or file name of the build .qll")
    public Path outputFilePath;

    @CommandLine.Option(names = {"--no-build-jar", "-n"}, description = "Qilletni should not rebuild build the native .jar")
    public boolean noBuildJar;
    
    @CommandLine.Option(names = {"--verbose", "-v"}, description = "Verbose Gradle output")
    public boolean verboseGradleOutput;

    @Override
    public Integer call() throws IOException {
        LOGGER.debug("Called build! {}", this);

        LOGGER.debug("Project root: {}", projectRoot);

        LOGGER.debug("Output file/directory: {}", outputFilePath);

        var sourcePath = projectRoot.resolve("qilletni-src");
        var buildDirectory = projectRoot.resolve("build");

        var infoParser = new QilletniInfoParser();
        var qilletniInfo = infoParser.readQilletniInfo(sourcePath);

        LOGGER.debug("Qilletni Info = {}", qilletniInfo);

        var qilletniSourceHandler = new QilletniSourceHandler();

        var qllBuildPath = buildDirectory.resolve("ql-build");

        FileUtil.clearAndCreateDirectory(qllBuildPath);

        if (GradleProjectHelper.isGradleProject(projectRoot)) {
            var gradleJarOptional = GradleProjectHelper.findProjectJar(projectRoot, verboseGradleOutput);
            
            if (gradleJarOptional.isPresent()) {
                var gradleJar = gradleJarOptional.get();

                // Build the jar if it doesn't exist, or if it's not told to NOT rebuild
                if (!Files.exists(gradleJar) || !noBuildJar) {
                    LOGGER.debug("Building Java .jar with shadowJar task");
                    GradleProjectHelper.runShadowJarTask(projectRoot, verboseGradleOutput);
                }

                // Copy it if it's been created
                if (Files.exists(gradleJar)) {
                    Files.copy(gradleJar, qllBuildPath.resolve("native.jar"));
                }
            }
        }

        qilletniSourceHandler.moveQilletniSource(qllBuildPath, sourcePath);

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
            destinationFile = PathUtility.getDependencyPath().resolve(defaultQllFileName);;
        }

        LOGGER.debug("Writing package to: {}", destinationFile);

        var qllPackager = new QllPackager();

        qllPackager.packageQll(qllBuildPath, destinationFile);

        return 0;
    }

    @Override
    public String toString() {
        return "CommandBuildArgs{" +
                "helpRequested=" + helpRequested +
                '}';
    }
}
