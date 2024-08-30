module QilletniToolchain.main {
    uses is.yarr.qilletni.api.auth.ServiceProvider;
    
    requires org.slf4j;
    requires org.yaml.snakeyaml;
    requires Qilletni.qilletni.api.main;
    requires QilletniDocgen.main;
    requires info.picocli;
    requires com.google.gson;
    requires is.yarr.qilletni.Qilletni.main;
}
