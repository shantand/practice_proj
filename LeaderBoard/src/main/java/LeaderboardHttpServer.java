import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class LeaderboardHttpServer {

    private static final int DEFAULT_PORT = 8080;
    private static final int WORKER_THREADS = 8;

    public static void start(Supplier<List<LeaderboardEntry>> leaderboardSource) throws IOException {
        start(DEFAULT_PORT, leaderboardSource);
    }

    public static void start(int port, Supplier<List<LeaderboardEntry>> leaderboardSource)
            throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/leaderboard", exchange ->
                handleLeaderboard(exchange, leaderboardSource));
        server.setExecutor(Executors.newFixedThreadPool(WORKER_THREADS));
        server.start();
        System.out.printf(
                "Leaderboard API running at http://localhost:%d/api/leaderboard (%d HTTP workers)%n",
                port,
                WORKER_THREADS
        );
    }

    private static void handleLeaderboard(
            HttpExchange exchange,
            Supplier<List<LeaderboardEntry>> leaderboardSource
    ) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        addCorsHeaders(headers);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try {
            byte[] body = toJson(leaderboardSource.get()).getBytes(StandardCharsets.UTF_8);
            headers.set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } catch (Exception e) {
            byte[] body = "{\"error\":\"Redis unavailable\"}".getBytes(StandardCharsets.UTF_8);
            headers.set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private static void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String toJson(List<LeaderboardEntry> entries) {
        StringBuilder json = new StringBuilder("[");
        int index = 0;
        for (LeaderboardEntry entry : entries) {
            if (index++ > 0) {
                json.append(',');
            }
            json.append(String.format(
                    "{\"rank\":%d,\"playerId\":\"%s\",\"score\":%.0f}",
                    entry.rank(),
                    entry.playerId().replace("\\", "\\\\").replace("\"", "\\\""),
                    entry.score()
            ));
        }
        json.append("]");
        return json.toString();
    }
}
