package is.yarr.qilletni.toolchain.command.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

//@Parameters(commandDescription = "Run a qilletni program")
@CommandLine.Command(name = "run", description = "Runs a Qilletni program")
public class CommandRunArgs implements Callable<Integer> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRunArgs.class);

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;
    
    @CommandLine.Parameters(description = "The .ql file to run")
    private String file; // first is the file to run, after is the params
    
    @CommandLine.Option(names = {"--qilletni-path", "-p"}, description = "The directory that holds all dependencies")
    public String qilletniPath; // TODO: Make default
    
    @CommandLine.Option(names = {"--provider-cache", "-c"}, description = "The directory to providers' cache files")
    public String providerCache;
    
    @CommandLine.Option(names = {"--use-credentials", "-u"}, description = "All the credential files to use with available providers")
    public List<String> useCredentials;
    
    @Override
    public Integer call() {
        LOGGER.debug("Called run! {}", this);
        return 0;
    }

    @Override
    public String toString() {
        return "CommandRunArgs{" +
                "helpRequested=" + helpRequested +
                ", file=" + file +
                ", qilletniPath='" + qilletniPath + '\'' +
                ", providerCache='" + providerCache + '\'' +
                ", useCredentials=" + useCredentials +
                '}';
    }
}
