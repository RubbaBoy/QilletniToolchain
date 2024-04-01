module QilletniToolchain.main {
    uses is.yarr.qilletni.api.auth.ServiceProvider;
    uses is.yarr.qilletni.api.lib.Library;
    
    requires org.slf4j;
    requires org.yaml.snakeyaml;
    requires Qilletni.qilletni.api.main;
    requires info.picocli;
    requires com.google.gson;
}
