package dev.bilal.ccmm.collab.server;

import java.net.InetSocketAddress;
import java.util.UUID;

public final class ServerMain {
    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        try {
            run(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
        }
    }

    private static void run(String[] args) throws Exception {
        String token = token(args, System.getenv("CCMM_INVITE_TOKEN"));
        if (token == null) {
            token = UUID.randomUUID().toString();
            System.out.println("invite token: " + token);
        }
        Relay relay = Relay.start(token, new InetSocketAddress(port(args, System.getenv("CCMM_PORT"))));
        Runtime.getRuntime().addShutdownHook(new Thread(relay::close));
        relay.await();
    }

    static String token(String[] args, String env) {
        String selected = arg(args, "--token");
        if (selected != null) {
            return selected;
        }
        if (env == null || env.isBlank()) {
            return null;
        }
        return env;
    }

    static int port(String[] args, String env) {
        String selected = arg(args, "--port");
        if (selected == null) {
            selected = (env == null || env.isBlank()) ? "8080" : env;
        }
        try {
            int port = Integer.parseInt(selected);
            if (port < 0 || port > 65535) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("port is invalid");
        }
    }

    private static String arg(String[] args, String name) {
        for (int i = 0; i < args.length; i++) {
            if (name.equals(args[i]) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }
}
