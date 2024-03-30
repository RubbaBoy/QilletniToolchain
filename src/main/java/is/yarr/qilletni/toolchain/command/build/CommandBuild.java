package is.yarr.qilletni.toolchain.command.build;

import is.yarr.qilletni.toolchain.config.QilletniInfoParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.PSource;
import java.io.IOException;

public class CommandBuild {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBuild.class);
    
    public void execute(CommandBuildArgs args) throws IOException {
        LOGGER.debug("Java .class files are in: {}", args.javaBuildPath);
        LOGGER.debug("Qilletni .ql files are in: {}", args.sourcePath);
        LOGGER.debug("Libraries are located in: {}", args.dependencyPath);
        
        var infoParser = new QilletniInfoParser();
        var qilletniInfo = infoParser.readQilletniInfo(args.sourcePath);

        LOGGER.debug("Qilletni Info = {}", qilletniInfo);
    }
    
}
