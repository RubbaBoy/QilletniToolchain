package is.yarr.qilletni.toolchain.qll;

import is.yarr.qilletni.api.auth.ServiceProvider;
import is.yarr.qilletni.api.lib.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;

public class NativeClassHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeClassHandler.class);

    private final String libraryName;

    public NativeClassHandler(String libraryName) {
        this.libraryName = libraryName;
    }

    public NativeClassOutput collectNativeClasses(Path outPath, Path javaBuildJar) throws IOException {
        Files.copy(javaBuildJar, outPath.resolve("native.jar"));

        try (var loader = new URLClassLoader(new URL[]{javaBuildJar.toUri().toURL()}, NativeClassHandler.class.getClassLoader())) {
            var libraryClass = findFirstOfService(loader, Library.class, "libraries");
            var providerClass = findFirstOfService(loader, ServiceProvider.class, "providers");

            libraryClass.ifPresent(library -> LOGGER.debug("Library class: {}", library.getCanonicalName()));
            providerClass.ifPresent(provider -> LOGGER.debug("Provider class: {}", provider.getCanonicalName()));

            return new NativeClassOutput(libraryClass.orElse(null), providerClass.orElse(null));
        }
    }

    private <T> Optional<Class<? extends T>> findFirstOfService(ClassLoader classLoader, Class<T> serviceType, String displayName) {
        var serviceLoader = ServiceLoader.load(serviceType, classLoader);

        var libraries = serviceLoader.stream().map(ServiceLoader.Provider::type).toList();

        if (libraries.isEmpty()) {
            return Optional.empty();
        }

        if (libraries.size() > 1) {
            LOGGER.warn("Found multiple {} in {}, using only {}", displayName, libraryName, libraries.getFirst().getCanonicalName());
        }

        return Optional.of(libraries.getFirst());
    }

    public record NativeClassOutput(String libraryClass, String providerClass) {
        public NativeClassOutput(Class<?> libraryClass, Class<?> providerClass) {
            this(libraryClass != null ? libraryClass.getCanonicalName() : null, providerClass != null ? providerClass.getCanonicalName() : null);
        }
    }
}
