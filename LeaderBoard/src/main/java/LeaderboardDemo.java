import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class LeaderboardDemo {

    static final String REDIS_HOST = "localhost";
    static final int REDIS_PORT = 6379;
    static final String LEADERBOARD = "leaderboard";
    static final String[] PLAYER_IDS = {
            "player-1", "player-2", "player-3", "player-4", "player-5"
    };

    static class Player implements Runnable {
        private final String playerId;
        private final Random random = new Random();

        Player(String playerId) {
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                while (true) {
                    int metric = random.nextInt(10) + 1;
                    double newScore = jedis.zincrby(LEADERBOARD, metric, playerId);

                    System.out.printf(
                            "%s emitted +%d, total score = %.0f%n",
                            playerId,
                            metric,
                            newScore
                    );

                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        startPlayers();
        LeaderboardHttpServer.start(LeaderboardDemo::snapshot);
        System.out.println("Open the React app and it will refresh every 2 seconds.");
        new CountDownLatch(1).await();
    }

    static List<LeaderboardEntry> snapshot() {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            List<Tuple> ranked = jedis.zrevrangeWithScores(LEADERBOARD, 0, -1);
            List<LeaderboardEntry> entries = new ArrayList<>(ranked.size());
            int rank = 1;
            for (Tuple entry : ranked) {
                entries.add(new LeaderboardEntry(rank++, entry.getElement(), entry.getScore()));
            }
            return entries;
        }
    }

    private static void startPlayers() {
        for (String playerId : PLAYER_IDS) {
            Thread thread = new Thread(new Player(playerId), playerId);
            thread.setDaemon(true);
            thread.start();
        }
    }
}
