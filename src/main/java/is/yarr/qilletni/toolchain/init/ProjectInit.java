package is.yarr.qilletni.toolchain.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectInit {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectInit.class);
    
    private static final String GRADLE_VERSION = "8.8";
    
    /**
     * @param sourcePath      The project directory
     * @param projectName
     * @param authorName
     * @param nativeInit
     * @param projectTypeEnum
     */
    public void initialize(Path sourcePath, String projectName, String authorName, NativeInit nativeInit, ProjectType projectTypeEnum) throws IOException, URISyntaxException {
        Files.createDirectories(sourcePath);
        
        var qilletniSrc = sourcePath.resolve("qilletni-src");
        Files.createDirectories(qilletniSrc);
        
        var yml = """
                name: %s
                version: 1.0.0
                author: %s%s
                """.formatted(projectName, authorName, nativeInit == null ? "" : "\n" + nativeInit.getNativeClassesList());
        
        Files.writeString(qilletniSrc.resolve("qilletni_info.yml"), yml);

        if (projectTypeEnum == ProjectType.LIBRARY) {
            var qilletniExamples = sourcePath.resolve("examples");
            Files.createDirectories(qilletniExamples);

            LOGGER.debug("Creating library starter files...");

            createExample(projectName, qilletniExamples, nativeInit);
            createLibraryStarterFile(projectName, qilletniSrc, nativeInit);
        } else if (projectTypeEnum == ProjectType.APPLICATION) {
            LOGGER.debug("Creating application starter files...");
            
            createApplicationStarterFile(projectName, qilletniSrc, nativeInit);
        }
        
        if (nativeInit != null) {
            LOGGER.debug("Creating Gradle project...");
            var gradleProjectInitializer = new GradleProjectInitializer(sourcePath, sourcePath.getFileName().toString(), nativeInit.packageName, nativeInit.className, GRADLE_VERSION);
            gradleProjectInitializer.initializeProject();
        }
    }
    
    private void createApplicationStarterFile(String projectName, Path destinationDir, NativeInit nativeInit) throws IOException {
        var starterFileContents = "";

        if (nativeInit != null) {
            starterFileContents += """
                    native fun sayGoodbye()
                    """;
        }
        
        starterFileContents += """
                print("Hello, World!")
                """;
        
        if (nativeInit != null) {
            starterFileContents += """
                    
                    sayGoodbye()
                    """;
        }
        
        Files.writeString(destinationDir.resolve("%s.ql".formatted(projectName)), starterFileContents);
    }
    
    private void createLibraryStarterFile(String projectName, Path destinationDir, NativeInit nativeInit) throws IOException {
        var starterFileContents = """
                fun sayHello() {
                    print("Hello, World!")
                }
                """;

        if (nativeInit != null) {
            starterFileContents += """
                    native fun sayGoodbye()
                    """;
        }
        
        Files.writeString(destinationDir.resolve("%s.ql".formatted(projectName)), starterFileContents);
    }
    
    private void createExample(String projectName, Path destinationDir, NativeInit nativeInit) throws IOException {
        var exampleContents = """
                import "%s:%s.ql"
                
                // Say hello from the sample function
                sayHello()
                """.formatted(projectName, projectName);

        if (nativeInit != null) {
            exampleContents += """
                    // Say goodbye from the native function, printed from Java
                    sayGoodbye()
                    """;
        }
        
        Files.writeString(destinationDir.resolve("example1.ql"), exampleContents);
    }
    
    public record NativeInit(String packageName, String className) {
        String getNativeClassesList() {
            return """
                    native_classes:
                      - %s.%s
                    """.formatted(packageName, className);
        }
    }
}
