package dev.qilletni.toolchain.command.auth;

import dev.qilletni.api.lib.persistence.PackageConfig;
import dev.qilletni.impl.lib.persistence.PackageConfigImpl;
import dev.qilletni.toolchain.gpg.AuthData;
import picocli.CommandLine;

import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "auth", description = "Authenticate with GitHub for QPM")
public class CommandAuth implements Callable<Integer> {
    
    private static final String AUTH_GITHUB = "auth_github";
    private static final String AUTH_KEY = "auth_key";
    
    /*
    
    qilletni auth
    qilletni auth RubbaBoy
    qilletni auth -k RubbaBoy
    qilletni auth --validate
    
     */

    // If empty, the current auth info will be printed
    @CommandLine.Parameters(description = "Your GitHub username", index = "0", arity = "0..1")
    public String username;

    @CommandLine.Option(names = {"-k", "--key"}, description = "The ID of the key on the system to use")
    private String keyId;
    
    @CommandLine.Option(names = {"-v", "--validate"}, description = "Checks with GitHub to see if the auth is valid")
    private boolean validate;
    
    @Override
    public Integer call() throws Exception {

        var packageConfig = PackageConfigImpl.createInternalConfig();
        packageConfig.loadConfig();
        
        if (validate) {
            System.out.println("Validating authentication...");
            // TODO: download keys from GH and check
            
            return 0;
        }

        // Print auth data
        if (username == null) {
            getPersistedAuthData(packageConfig).ifPresentOrElse(authData -> {
                System.out.println("Current authentication data:");
                System.out.println("GitHub username: " + authData.username());
                System.out.println("Key ID: " + authData.keyId());
            }, () -> {
                System.out.println("No authentication data found.");
             });
            
            return 0;
        }
        
        String storeKeyId = keyId;

        var keyGenerator = new GPGKeyGenerator();
        
        if (keyId == null) {
            var keyIdOptional = keyGenerator.generateGPGKey("Qilletni-%s".formatted(username), "ed25519", "sign", "2y");

            if (keyIdOptional.isEmpty()) {
                System.out.println("Failed to generate GPG key");
                return 1;
            }

            storeKeyId = keyIdOptional.get();
        }
        
        packageConfig.set(AUTH_GITHUB, username);
        packageConfig.set(AUTH_KEY, storeKeyId);
        packageConfig.saveConfig();

        System.out.println("Saved authentication data:");
        System.out.println("GitHub username: " + username);
        System.out.println("Key ID: " + storeKeyId);

        System.out.println("\nPlease go to the following URL and paste in your public key:");
        System.out.println("https://github.com/settings/gpg/new");
        System.out.println("\nTo get your public key, press 'p', or press any key to exit...");

        var scanner = new Scanner(System.in);
        if (scanner.nextByte() == 'p') {
            var publicKey = keyGenerator.getPublicKey(storeKeyId);
            System.out.println("Public key:");
            System.out.println(publicKey);
        }

        return 0;
    }
    
    private Optional<AuthData> getPersistedAuthData(PackageConfig packageConfig) {
        var usernameOptional = packageConfig.get(AUTH_GITHUB);
        var keyOptional = packageConfig.get(AUTH_KEY);

        if (usernameOptional.isPresent() && keyOptional.isPresent()) {
            return Optional.of(new AuthData(usernameOptional.get(), keyOptional.get()));
        } else {
            return Optional.empty();
        }
    }
}
