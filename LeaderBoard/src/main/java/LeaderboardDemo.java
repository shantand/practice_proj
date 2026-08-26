import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class LeaderboardDemo {

    static final String REDIS_HOST = "localhost";
    static final int REDIS_PORT = 6379;
    static final String LEADERBOARD = "leaderboard";
    static final int PLAYER_COUNT = 1_000_000;
    static final int EMITTER_THREADS = 5;
    static final int TOP_N = 100;
    static final int EMIT_INTERVAL_MS = 500;

    static class MetricEmitter implements Runnable {
        private final int workerId;

        MetricEmitter(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                while (!Thread.currentThread().isInterrupted()) {
                    String playerId = "player-" + random.nextInt(1, PLAYER_COUNT + 1);
                    int metric = random.nextInt(1, 11);
                    double newScore = jedis.zincrby(LEADERBOARD, metric, playerId);

                    System.out.printf(
                            "worker-%d %s emitted +%d, total score = %.0f%n",
                            workerId,
                            playerId,
                            metric,
                            newScore
                    );

                    Thread.sleep(EMIT_INTERVAL_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        startEmitters();
        LeaderboardHttpServer.start(LeaderboardDemo::snapshot);
        System.out.printf(
                "Emitting scores for player-1..player-%d with %d workers. UI shows top %d every 2 seconds.%n",
                PLAYER_COUNT,
                EMITTER_THREADS,
                TOP_N
        );
        new CountDownLatch(1).await();
    }

    static List<LeaderboardEntry> snapshot() {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            List<Tuple> ranked = jedis.zrevrangeWithScores(LEADERBOARD, 0, TOP_N - 1);
            List<LeaderboardEntry> entries = new ArrayList<>(ranked.size());
            int rank = 1;
            for (Tuple entry : ranked) {
                entries.add(new LeaderboardEntry(rank++, entry.getElement(), entry.getScore()));
            }
            return entries;
        }
    }

    private static void startEmitters() {
        ExecutorService pool = Executors.newFixedThreadPool(EMITTER_THREADS, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        for (int i = 1; i <= EMITTER_THREADS; i++) {
            pool.submit(new MetricEmitter(i));
        }
    }
}
