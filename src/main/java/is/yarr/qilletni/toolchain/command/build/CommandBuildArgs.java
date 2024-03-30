package is.yarr.qilletni.toolchain.command.build;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import is.yarr.qilletni.toolchain.command.converter.PathConverter;

import java.nio.file.Path;

@Parameters(commandDescription = "Build a qilletni library")
public class CommandBuildArgs {
    
//    @Parameter(description = "The library to build", required = true, converter = PathConverter.class)
//    public Path file;
    
    @Parameter(description = "The directory that contains source Qilletni .ql files", converter = PathConverter.class)
    public Path sourcePath;
    
    @Parameter(names = {"--java-path", "-j"}, description = "The directory that contains build Java .class files", converter = PathConverter.class)
    public Path javaBuildPath;
    
    @Parameter(names = {"--dependency-path", "-d"}, description = "The directory that contains all .qll dependencies", converter = PathConverter.class)
    public Path dependencyPath;
    
}
