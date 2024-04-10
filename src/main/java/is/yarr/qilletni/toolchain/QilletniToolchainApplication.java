package is.yarr.qilletni.toolchain;

import is.yarr.qilletni.toolchain.command.build.CommandBuild;
import is.yarr.qilletni.toolchain.command.run.CommandRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "qilletni", subcommands = {CommandRun.class, CommandBuild.class})
public class QilletniToolchainApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(QilletniToolchainApplication.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    public static void main(String[] args) {
        var application = new QilletniToolchainApplication();

        if (args.length == 0) {
            LOGGER.error("Invalid command!");
            return;
        }

        new CommandLine(application)
                .execute(args);
    }
}
