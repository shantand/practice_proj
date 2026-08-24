import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class DistributedCounterDemo {

    // Simulates the shared storage of generated IDs.
    // In a real distributed system this would NOT be a common HashSet.
    private static final Set<Long> generatedIds = new HashSet<>();
    private static final AtomicInteger waitCount = new AtomicInteger();

    // Snowflake-style ID generator
    static class IdGenerator {

        // Number of bits allocated to each component
        private static final int SEQUENCE_BITS = 12;
        private static final int MACHINE_ID_BITS = 10;

        private static final long MAX_SEQUENCE =
                (1L << SEQUENCE_BITS) - 1;

        private static final long MAX_MACHINE_ID =
                (1L << MACHINE_ID_BITS) - 1;

        // Custom epoch - reduces timestamp size
        private static final long EPOCH = 1704067200000L;
        // 2024-01-01 00:00:00 UTC

        private final long machineId;

        private long lastTimestamp = -1L;
        private long sequence = 0L;

        public IdGenerator(long machineId) {

            if (machineId < 0 || machineId > MAX_MACHINE_ID) {
                throw new IllegalArgumentException(
                        "Machine ID must be between 0 and " + MAX_MACHINE_ID
                );
            }

            this.machineId = machineId;
        }

        /**
         * Generate a unique ID.
         *
         * Layout:
         *
         * | timestamp | machineId | sequence |
         *
         * timestamp = 41 bits
         * machineId = 10 bits
         * sequence  = 12 bits
         *
         * Total = 63 bits
         */
        public synchronized long nextId(long requestTimestamp) {

            long timestamp = requestTimestamp - EPOCH;

            if (timestamp < 0) {
                throw new IllegalArgumentException(
                        "Timestamp cannot be before epoch"
                );
            }

            // Same millisecond
            if (timestamp == lastTimestamp) {

                sequence = (sequence + 1) & MAX_SEQUENCE;

                // Sequence exhausted for this millisecond
                if (sequence == 0) {
                    timestamp = waitForNextMillis(lastTimestamp);
                }

            } else {
                // New millisecond
                sequence = 0;
            }

            lastTimestamp = timestamp;

            long id =
                    (timestamp << (MACHINE_ID_BITS + SEQUENCE_BITS))
                    |
                    (machineId << SEQUENCE_BITS)
                    |
                    sequence;

            return id;
        }

        private long waitForNextMillis(long lastTimestamp) {

            waitCount.incrementAndGet();

            long timestamp = System.currentTimeMillis() - EPOCH;

            while (timestamp <= lastTimestamp) {
                timestamp = System.currentTimeMillis() - EPOCH;
            }

            return timestamp;
        }
    }

    /**
     * Simulates one machine sending requests.
     */
    static class Machine implements Runnable {

        private final long machineId;
        private final IdGenerator generator;

        public Machine(long machineId) {
            this.machineId = machineId;
            this.generator = new IdGenerator(machineId);
        }

        @Override
        public void run() {

            for (int i = 0; i < 100_000; i++) {

                // Simulate request timestamp
                long timestamp = System.currentTimeMillis();

                long id = generator.nextId(timestamp);

                synchronized (generatedIds) {

                    if (!generatedIds.add(id)) {

                        System.out.println(
                                "!!! COLLISION !!! " +
                                "Machine=" + machineId +
                                " ID=" + id
                        );

                    }
                }
            }

            System.out.println(
                    "Machine " + machineId +
                    " finished."
            );
        }
    }

    public static void main(String[] args) throws Exception {

        Thread machine1 = new Thread(new Machine(1));
        Thread machine2 = new Thread(new Machine(2));
        Thread machine3 = new Thread(new Machine(3));

        machine1.start();
        machine2.start();
        machine3.start();

        machine1.join();
        machine2.join();
        machine3.join();

        System.out.println();
        System.out.println(
                "Total unique IDs generated = "
                + generatedIds.size()
        );

        System.out.println(
                "Expected = " + (3 * 100_000)
        );

        System.out.println(
                "Times waited for next millisecond = "
                + waitCount.get()
        );

        if (generatedIds.size() == 300_000) {
            System.out.println("SUCCESS: No collisions!");
        } else {
            System.out.println("ERROR: Collision detected!");
        }
    }
}