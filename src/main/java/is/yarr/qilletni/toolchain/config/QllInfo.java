package is.yarr.qilletni.toolchain.config;

import java.util.List;

public record QllInfo(String name, Version version, String author, List<QilletniInfoData.Dependency> dependencies, String libraryClass, String providerClass) {
    public QllInfo(QilletniInfoData qilletniInfoData, String libraryClass, String providerClass) {
        this(qilletniInfoData.name(), qilletniInfoData.version(), qilletniInfoData.author(), qilletniInfoData.dependencies(), libraryClass, providerClass);
    }
}
