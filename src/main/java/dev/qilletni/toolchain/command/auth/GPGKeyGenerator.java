package dev.qilletni.toolchain.command.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GPGKeyGenerator {
    
    public boolean validateGpgKey(String keyId) throws IOException, InterruptedException {
        var pbValidate = new ProcessBuilder("gpg", "--list-keys", keyId);
        pbValidate.redirectErrorStream(true);

        var processValidate = pbValidate.start();
        var validateOutput = new BufferedReader(new InputStreamReader(processValidate.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        processValidate.waitFor();

        return validateOutput.contains(keyId);
    }
    
    public String getPublicKey(String keyId) throws IOException, InterruptedException {
        var publicKeyProcess = new ProcessBuilder("gpg", "--export", "--armor", keyId);
        publicKeyProcess.redirectErrorStream(true);

        var processGen = publicKeyProcess.start();
        var publicKeyOutput = new BufferedReader(new InputStreamReader(processGen.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        processGen.waitFor();

        return publicKeyOutput;
    }
    
    public Optional<String> generateGPGKey(String name, String type, String usage, String expiration) throws IOException, InterruptedException {
        var pbGen = new ProcessBuilder("gpg", "--quick-generate-key", name, type, usage, expiration);
        pbGen.redirectErrorStream(true);
        
        pbGen.redirectInput(ProcessBuilder.Redirect.INHERIT);
        
        var processGen = pbGen.start();
        var reader = new BufferedReader(new InputStreamReader(processGen.getInputStream()));
        var stringBuilder = new StringBuilder();
        
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);  // Print to console
            stringBuilder.append(line).append("\n");
        }

        processGen.waitFor();

        var genOutput = stringBuilder.toString();

        System.out.println("<<< " + genOutput + " >>>");
        
        return extractKeyId(genOutput);
    }
    
    public void signFile(String keyId, Path outputSigFile, Path fileToSign) throws IOException, InterruptedException {
        var pbSign = new ProcessBuilder("gpg", "--local-user", keyId, "--output", outputSigFile.toString(), "--sign", fileToSign.toString());
        pbSign.redirectErrorStream(true);
        
        var processSign = pbSign.start();
        var signOutput = new BufferedReader(new InputStreamReader(processSign.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));
        
        processSign.waitFor();
        
        System.out.println("Signing Output:\n" + signOutput);
    }

    /**
     * Extracts the key id from the output of the key generation command.
     * Expects a line in the output like "gpg: key ABCDEF12: ..."
     */
    private static Optional<String> extractKeyId(String output) {
        var pattern = Pattern.compile("^gpg: key ([A-F0-9]{16})\\s");
        var matcher = pattern.matcher(output);
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        }
        
        return Optional.empty();
    }
}
