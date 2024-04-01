package is.yarr.qilletni.toolchain.command.run;

import java.nio.file.Paths;

public class CommandRun {
    
    public void execute(CommandRunArgs args) {
        System.out.println("run qilletniPath = " + args.qilletniPath);
//        System.out.println("run file = " + args.getRunningFile());
//        System.out.println("run file args = " + args.getProgramArgs());
        System.out.println("run provider cache = " + args.providerCache);
        System.out.println("run use credentials = " + args.useCredentials);
    }
}
