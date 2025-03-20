module qilletni.toolchain {
    uses is.yarr.qilletni.api.auth.ServiceProvider;
    
    requires org.slf4j;
    requires qilletni.api;
    requires qilletni.impl;
    requires qilletni.docgen;
    requires org.yaml.snakeyaml;
    requires info.picocli;
    requires com.google.gson;
}
