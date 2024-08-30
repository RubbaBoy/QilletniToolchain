package is.yarr.qilletni.toolchain.docs;

import is.yarr.qilletni.api.lib.qll.QilletniInfoData;
import is.yarr.qilletni.docgen.DocGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class DocumentationOrchestrator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentationOrchestrator.class);
    
    public void beginDocGen(QilletniInfoData qilletniInfo, Path cacheDirectory, Path inputDirectory, Path outputDirectory) {
        LOGGER.debug("Generating docs for: {}", qilletniInfo.name());

        try {
            var docGenerator = new DocGenerator();
            docGenerator.generateDocs(outputDirectory, cacheDirectory, inputDirectory, qilletniInfo.name());
        } catch (IOException e) {
            LOGGER.error("Failed to generate docs for: {}", qilletniInfo.name(), e);
        }
        
    }
    
}
