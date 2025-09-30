package dev.qilletni.toolchain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.core.net.Protocol;

import java.nio.charset.StandardCharsets;

public class LogSetup {

    public static void setupLogSocket(int port) {
        var ctx = (LoggerContext) LogManager.getContext(false);
        var cfg = ctx.getConfiguration();

        var layout = JsonLayout.newBuilder()
                .setConfiguration(cfg)
                .setCompact(true)
                .setEventEol(true)           // newline per event for easier parsing
                .setCharset(StandardCharsets.UTF_8)
                .setIncludeStacktrace(true)
                .build();

        SocketAppender appender = SocketAppender.newBuilder()
                .setConfiguration(cfg)
                .setName("JarSocketLogs")
                .setHost("127.0.0.1")
                .setPort(port)
                .setProtocol(Protocol.TCP)
                .setReconnectDelayMillis(2000) // auto-retry if server not up yet
                .setImmediateFlush(true)
                .setIgnoreExceptions(true)
                .setLayout(layout)
                .build();

        appender.start();
        cfg.addAppender(appender);

        // Remove console appenders

        for (LoggerConfig lc : cfg.getLoggers().values()) {
            // Copy keys to avoid concurrent modification
            for (String name : lc.getAppenders().keySet()) {
                var app = lc.getAppenders().get(name);
                if (app instanceof ConsoleAppender) {
                    lc.removeAppender(name);
                }
            }
        }
        var root = cfg.getRootLogger();
        for (String name : root.getAppenders().keySet()) {
            var app = root.getAppenders().get(name);
            if (app instanceof ConsoleAppender) {
                root.removeAppender(name);
            }
        }


        // Attach to Root so additive loggers flow here.
        cfg.getRootLogger().addAppender(appender, null, null);

        // Also attach to all explicitly configured loggers (covers additivity="false").
        for (LoggerConfig lc : cfg.getLoggers().values()) {
            lc.addAppender(appender, null, null);
        }

        ctx.updateLoggers();
    }

}
