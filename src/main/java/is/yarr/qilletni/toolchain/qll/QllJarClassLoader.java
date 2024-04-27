package is.yarr.qilletni.toolchain.qll;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class QllJarClassLoader extends ClassLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(QllJarClassLoader.class);

    private final Map<String, byte[]> classes = new HashMap<>();

    // name of META-INF file, list of impls
    private final Map<String, List<String>> serviceContents = new HashMap<>();
    private final Map<String, URL> services = new HashMap<>();
    
    private final Map<String, InputStream> inputStreams = new HashMap<>();
    
    public QllJarClassLoader(Path tempRunDir) {
    }

    public void loadJar(InputStream inputStream) throws IOException {
        var jarInputStream = new JarInputStream(inputStream);
        JarEntry entry;

        while ((entry = jarInputStream.getNextJarEntry()) != null) {
            var entryName = entry.getName();
            
            if (entryName.endsWith(".class")) {
                var className = entryName.replace('/', '.')
                        .substring(0, entryName.length() - 6); 

                classes.put(className, jarInputStream.readAllBytes());
            } else if (entryName.startsWith("META-INF/services/")) {
                var list = serviceContents.computeIfAbsent(entryName, k -> new ArrayList<>());
                list.add(new String(jarInputStream.readAllBytes()));
                serviceContents.put(entryName, list);
            } else {
//                inputStreams.putIfAbsent(entryName, jarInputStream.)
            }
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (!classes.containsKey(name)) {
            return getParent().loadClass(name);
        }

        var classBytes = classes.get(name);
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    @Override
    protected URL findResource(String name) {
        LOGGER.debug("findResource {}", name);

        if (hasService(name)) {
            return getServiceURL(name);
        }

        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        LOGGER.debug("findResources {}", name);

        var urls = new ArrayList<URL>();

        if (hasService(name)) {
            urls.add(getServiceURL(name));
        }

        var superResources = super.findResources(name);
        while (superResources.hasMoreElements()) {
            urls.add(superResources.nextElement());
        }

        LOGGER.debug("\treturning: {}", urls);

        return Collections.enumeration(urls);
    }

    private boolean hasService(String resourceName) {
        return serviceContents.containsKey(resourceName);
    }

    private URL getServiceURL(String resourceName) {
        return services.computeIfAbsent(resourceName, k -> {
            var serviceLines = serviceContents.get(resourceName);
            var serviceFile = String.join("\n", serviceLines);
            
            LOGGER.debug("servcuieFile: {}", serviceFile);

            try {
                return URL.of(new URI("ql://services/" + resourceName), new ByteURLStreamHandler(serviceFile.getBytes(StandardCharsets.UTF_8)));
            } catch (MalformedURLException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static class ByteURLStreamHandler extends URLStreamHandler {

        private final byte[] data;

        ByteURLStreamHandler(byte[] data) {
            this.data = data;
        }

        @Override
        protected URLConnection openConnection(URL url) {
            return new URLConnection(url) {

                @Override
                public void connect() {
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(data);
                }
            };
        }
    }
}
