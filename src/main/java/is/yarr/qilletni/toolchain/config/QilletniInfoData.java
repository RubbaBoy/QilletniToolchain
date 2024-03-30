package is.yarr.qilletni.toolchain.config;

import java.util.List;

public record QilletniInfoData(String name, Version version, String author, List<Dependency> dependencies) {
    
    public record Dependency(String name, ComparableVersion version) {}
    
}
