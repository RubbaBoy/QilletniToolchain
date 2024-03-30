package is.yarr.qilletni.toolchain.command.run;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

@Parameters(commandDescription = "Run a qilletni program")
public class CommandRunArgs {
    
    @Parameter(description = "The .ql file to run", required = true)
    private List<String> file; // first is the file to run, after is the params
    
    @Parameter(names = {"--qilletni-path", "-p"}, description = "The directory that holds all dependencies")
    public String qilletniPath; // TODO: Make default
    
    @Parameter(names = {"--provider-cache", "-c"}, description = "The directory to providers' cache files")
    public String providerCache;
    
    @Parameter(names = {"--use-credentials", "-u"}, description = "All the credential files to use with available providers")
    public List<String> useCredentials;
    
    public String getRunningFile() {
        return file.getFirst();
    }
    
    public List<String> getProgramArgs() {
        return file.subList(1, file.size());
    }
    
}
