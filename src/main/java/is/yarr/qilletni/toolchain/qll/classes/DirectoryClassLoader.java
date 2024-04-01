package is.yarr.qilletni.toolchain.qll.classes;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class DirectoryClassLoader extends URLClassLoader {

    public DirectoryClassLoader(Path directory) throws MalformedURLException {
        super(new URL[] { directory.toUri().toURL() }, getSystemClassLoader());
    }

    // Convenience method to load a class by name
    public Class<?> loadClassByName(String name) throws ClassNotFoundException {
        return loadClass(name);
    }
    
}
