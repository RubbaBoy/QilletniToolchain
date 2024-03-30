package is.yarr.qilletni.toolchain;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import is.yarr.qilletni.toolchain.command.build.CommandBuild;
import is.yarr.qilletni.toolchain.command.build.CommandBuildArgs;
import is.yarr.qilletni.toolchain.command.run.CommandRun;
import is.yarr.qilletni.toolchain.command.run.CommandRunArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class QilletniToolchainApplication {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniToolchainApplication.class);

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help;
    
    private final CommandRunArgs runArgs = new CommandRunArgs();
    private final CommandBuildArgs buildArgs = new CommandBuildArgs();

    public static void main(String[] args) {
        var application = new QilletniToolchainApplication();

        try {
            var jCommander = JCommander.newBuilder()
                    .addObject(application)
                    .addCommand("run", application.runArgs)
                    .addCommand("build", application.buildArgs)
                    .build();

            jCommander.parse(args);

            application.run(jCommander);
        } catch (ParameterException e) {
            e.usage();
        }
    }
    
    public void run(JCommander jCommander) {
        if (help) {
            System.out.println("Help! Some shit idk");
            return;
        }

        try {
            switch (jCommander.getParsedCommand()) {
                case "run" -> new CommandRun().execute(runArgs);
                case "build" -> new CommandBuild().execute(buildArgs);
            }
        } catch (Exception e) {
            LOGGER.error("An exception occurred while running the command " + jCommander.getParsedCommand(), e);
        }
    }

}
