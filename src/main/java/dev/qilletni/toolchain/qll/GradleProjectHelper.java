package dev.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GradleProjectHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradleProjectHelper.class);

    /**
     * Runs a Gradle task with the specified arguments.
     *
     * @param pathToProjectRoot The path to the root of the Gradle project
     * @param task              The Gradle task to run
     * @param args              Additional arguments to pass to Gradle
     * @return The process result containing exit code and output
     */
    public static ProcessResult runGradleTask(Path pathToProjectRoot, boolean verboseGradleOutput, String task, String... args) {
        try {
            // Determine whether to use gradlew or gradlew.bat based on OS
            var gradleWrapper = System.getProperty("os.name").toLowerCase().contains("win")
                    ? "gradlew.bat"
                    : "./gradlew";

            // Build the command
            var command = new ArrayList<String>();
            command.add(gradleWrapper);
            command.add(task);

            Collections.addAll(command, args);

            var processBuilder = new ProcessBuilder(command);
            processBuilder.directory(pathToProjectRoot.toFile());

            LOGGER.info("Running Gradle task: '{}' in directory: {}", String.join(" ", command), pathToProjectRoot.toAbsolutePath());

            var process = processBuilder.start();

            var output = new StringBuilder();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(str -> {
                    if (verboseGradleOutput) {
                        System.out.println(str);
                    }
                    
                    output.append(str);
                });
            }

            boolean completed = process.waitFor(1, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                return new ProcessResult(-1, "Process timed out after 1 minute");
            }

            return new ProcessResult(process.exitValue(), output.toString());

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while running Gradle task: {}", task, e);
            return new ProcessResult(-1, e.getMessage());
        }
    }

    /**
     * Runs the shadowJar task.
     *
     * @param pathToProjectRoot The path to the root of the Gradle project
     * @return The process result containing exit code and output
     */
    public static ProcessResult runShadowJarTask(Path pathToProjectRoot, boolean verboseGradleOutput) {
        return runGradleTask(pathToProjectRoot, verboseGradleOutput, "shadowJar");
    }

    /**
     * Finds the jar file that will be created (or has been created) in the project in the given path.
     *
     * @param pathToProjectRoot The path to the root of the Gradle project
     * @return The path of the jar file
     */
    public static Optional<Path> findProjectJar(Path pathToProjectRoot, boolean verboseGradleOutput) {
        Path jarFindScript = null;

        try {
            jarFindScript = Files.createTempFile("qilletni-gradle-jar-find", ".groovy");
            Files.writeString(jarFindScript, "gradle.taskGraph.afterTask { t, _ -> if (t.name == 'shadowJar') println t.archiveFile.get() }");

            // Run the shadowJar task with the script to find the jar file
            ProcessResult result = runGradleTask(
                    pathToProjectRoot,
                    verboseGradleOutput,
                    "shadowJar",
                    "--console=plain",
                    "--quiet",
                    "-I",
                    jarFindScript.toString()
            );

            if (result.exitCode != 0 || result.output.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(Path.of(result.output));

        } catch (IOException e) {
            LOGGER.error("Error while finding the jar file", e);
            return Optional.empty();
        } finally {
            if (jarFindScript != null) {
                try {
                    Files.deleteIfExists(jarFindScript);
                } catch (IOException ignored) {}
            }
        }
    }

    public static boolean isGradleProject(Path pathToProjectRoot) {
        return Files.exists(pathToProjectRoot.resolve("build.gradle"));
    }

    /**
     * Represents the result of a process execution.
     */
    public record ProcessResult(int exitCode, String output) {
        public boolean isSuccessful() {
            return exitCode == 0;
        }
    }
}
