package is.yarr.qilletni.toolchain.command.persist;

import is.yarr.qilletni.api.lib.persistence.PackageConfig;
import is.yarr.qilletni.lib.persistence.PackageConfigImpl;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = "persist", description = "Modifies persistent data in Qilletni")
public class CommandPersist implements Callable<Integer> {

    // Positional parameter for the package name
    @CommandLine.Parameters(index = "0", description = "The name of the package to manage (or `internal`)")
    private String packageName;

    // Positional parameter for the key=value or key
    @CommandLine.Parameters(index = "1", arity = "0..*", description = "The key=value pair to set a parameter or key to view/remove a parameter")
    private List<String> data;

    // Optional flag to remove a parameter
    @CommandLine.Option(names = {"-r", "--remove"}, description = "Remove any properties not being set")
    private boolean remove;

    // Optional flag to remove a parameter
    @CommandLine.Option(names = {"-a", "--all"}, description = "List all properties for the package")
    private boolean all;

    @Override
    public Integer call() {
        PackageConfig packageConfig;
        
        if (packageName.equals("internal")) {
            packageConfig = PackageConfigImpl.createInternalConfig();
        } else {
            packageConfig = PackageConfigImpl.createPackageConfig(packageName);
        }
        
        packageConfig.loadConfig();
        
        if (all) {
            printKeyValueMap(packageConfig.getAll());
            return 0;
        }

        var keys = data.stream().filter(kv -> !kv.contains("=")).toList();
        var keyValues = data.stream().filter(kv -> kv.contains("=")).toList();
        
        if (!keys.isEmpty()) {
            if (remove) {
                System.out.println("Removing: " + String.join(", ", keys));
                keys.forEach(packageConfig::remove);
            } else {
                var kvMap = keys.stream().collect(Collectors.toMap(Function.identity(), key -> packageConfig.get(key).orElse("- empty -")));
                printKeyValueMap(kvMap);
            }
        }

        if (!keyValues.isEmpty()) {
            keyValues.forEach(kv -> {
                var parts = kv.split("=", 2);
                var key = parts[0];
                var value = parts[1];

                System.out.printf("Parameter '%s' set to '%s' in package '%s'.%n", key, value, packageName);
                packageConfig.set(key, value);
            });
            
            packageConfig.saveConfig();
        }
        
        return 0;
    }
    
    private void printKeyValueMap(Map<String, String> kvMap) {
        var maxKeyLength = Math.max(kvMap.keySet().stream().mapToInt(String::length).max().orElse(1), "Property".length());
        var maxValueLength = Math.max(kvMap.values().stream().mapToInt(String::length).max().orElse(1), "Value".length());

        var format = "| %-" + maxKeyLength + "s | %-" + maxValueLength + "s |%n";

        System.out.printf(format, "Property", "Value");
        System.out.printf(format, "-".repeat(maxKeyLength), "-".repeat(maxValueLength));

        kvMap.forEach((key, value) -> System.out.printf(format, key, value));
    }
}
