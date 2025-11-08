import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class lfg {

    // Configuration variables
    private static int maxConcurrentDungeons;
    private static int availableTanks;
    private static int availableHealers;
    private static int availableDps;
    private static int minDuration;
    private static int maxDuration;

    // Concurrency and state management
    private static final ReentrantLock groupLock = new ReentrantLock();
    private static final Condition canStartDungeon = groupLock.newCondition();
    private static final List<DungeonRun> activeDungeons = new ArrayList<>();
    private static int currentActiveDungeons = 0;

    /**
     * Represents a single dungeon instance that can be active or empty.
     */
    static class DungeonRun {
        int dungeonId;
        boolean isRunning = false;
        int groupsCompleted = 0;
        long totalActiveTimeSeconds = 0;

        DungeonRun(int id) {
            this.dungeonId = id;
        }
    }

    /**
     * Generates a random integer within the specified inclusive range.
     */
    private static int getRandomDuration(int min, int max) {
        // nextInt's upper bound is exclusive, so add 1
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Checks if there are enough players in the queue to form a standard party.
     */
    private static boolean checkPartyComposition() {
        // A party requires 1 Tank, 1 Healer, and 3 DPS
        return (availableTanks >= 1 && availableHealers >= 1 && availableDps >= 3);
    }

    
    public static void processDungeonRun(int dungeonIndex, int duration, int groupNumber) {
        DungeonRun run;

        try {
            groupLock.lock();
            try {
                run = activeDungeons.get(dungeonIndex);
                System.out.printf("\n[Group #%d] is entering Dungeon %d. Estimated time: %ds.\n",
                        groupNumber, run.dungeonId, duration);

                printCurrentDungeonStatus();

            } finally {
                groupLock.unlock();
            }

            // --- Simulate the Dungeon Run (Thread sleeps) ---
            TimeUnit.SECONDS.sleep(duration);

            groupLock.lock();
            try {
                run = activeDungeons.get(dungeonIndex); 
                run.isRunning = false;
                run.totalActiveTimeSeconds += duration;
                currentActiveDungeons--;

                System.out.printf("\n[Group #%d] has cleared Dungeon %d. (Run time: %ds)\n",
                        groupNumber, run.dungeonId, duration);
                System.out.printf("  > This dungeon has now served %d groups.\n", run.groupsCompleted);

                printCurrentDungeonStatus();

                canStartDungeon.signalAll();
            } finally {
                groupLock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Dungeon run was interrupted.");
        }
    }

    /**
     * Helper to print the status of all available dungeons.
     * Must be called while holding the 'groupLock'.
     */
    private static void printCurrentDungeonStatus() {
        System.out.println("===== Dungeon Status =====");
        for (DungeonRun dr : activeDungeons) {
            System.out.printf("  Dungeon %-2d: [%s]\n",
                    dr.dungeonId, (dr.isRunning ? "ACTIVE" : "EMPTY"));
        }
        System.out.println("==========================\n");
    }

    
    //The main simulation loop that manages the queue and dispatches dungeon runs.
    public static void manageQueue() {
        try {      
            while (true) {
                groupLock.lock(); 
                try {
                    
                    if (!checkPartyComposition()) {
                        break; // Exit the while(true) loop
                    }

                    while (currentActiveDungeons >= maxConcurrentDungeons) {
                        canStartDungeon.await(); // Atomically releases lock and waits
                        
                        if (!checkPartyComposition()) {
                             break;
                        }
                    }
                    
                    if (!checkPartyComposition()) {
                        break; 
                    }
                    int dungeonIndex = -1;
                    for (int i = 0; i < maxConcurrentDungeons; i++) {
                        if (!activeDungeons.get(i).isRunning) {
                            dungeonIndex = i;
                            break;
                        }
                    }
                    
                    availableTanks--;
                    availableHealers--;
                    availableDps -= 3;

                    currentActiveDungeons++;
                    DungeonRun selectedRun = activeDungeons.get(dungeonIndex);
                    selectedRun.isRunning = true;
                    selectedRun.groupsCompleted++;
                    
                    int assignedGroupNumber = selectedRun.groupsCompleted;
                    int duration = getRandomDuration(minDuration, maxDuration);

                    final int finalDungeonIndex = dungeonIndex;
                    Thread dungeonThread = new Thread(() -> {
                        processDungeonRun(finalDungeonIndex, duration, assignedGroupNumber);
                    });
                    dungeonThread.start(); 

                } finally {
                    groupLock.unlock(); // Always unlock 
                }
            } 

            // --- Simulation Shutdown ---
            groupLock.lock();
            try {
                while (currentActiveDungeons > 0) {
                    canStartDungeon.await();
                }
            } finally {
                groupLock.unlock();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Simulation manager was interrupted.");
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter maximum concurrent dungeon instances: ");
            maxConcurrentDungeons = scanner.nextInt();
            System.out.print("Enter number of available tanks: ");
            availableTanks = scanner.nextInt();
            System.out.print("Enter number of available healers: ");
            availableHealers = scanner.nextInt();
            System.out.print("Enter number of available DPS: ");
            availableDps = scanner.nextInt();
            System.out.print("Enter minimum clear time (seconds): ");
            minDuration = scanner.nextInt();
            System.out.print("Enter maximum clear time (seconds): ");
            maxDuration = scanner.nextInt();

            // Input Validation
            if (maxConcurrentDungeons <= 0) {
                System.out.println("Max instances must be > 0.");
                return;
            }
            if (minDuration > maxDuration) {
                System.out.println("Minimum time cannot be greater than maximum time.");
                return;
            }

            // Initialize dungeon instances
            for (int i = 0; i < maxConcurrentDungeons; i++) {
                activeDungeons.add(new DungeonRun(i + 1));
            }

            System.out.println("\nStarting LFG Simulation...\n");
            
            // Run the simulation
            manageQueue();

            // --- Print Final Report ---
            System.out.println("========= FINAL SIMULATION REPORT =========");
            for (DungeonRun run : activeDungeons) {
                System.out.printf(">>> Dungeon %-2d Report: %-3d groups processed | Total service time: %ds\n",
                        run.dungeonId, run.groupsCompleted, run.totalActiveTimeSeconds);
            }
            
            System.out.printf("\n>>> Unused Players: [Tanks: %d, Healers: %d, DPS: %d]\n",
                    availableTanks, availableHealers, availableDps);
            System.out.println("===========================================");
            
        } catch (Exception e) {
            System.err.println("An error occurred during input: " + e.getMessage());
        }
    }
}