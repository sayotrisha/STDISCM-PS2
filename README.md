# LFG (Looking for Group) Dungeon Queue Manager

A Java-based simulation that manages dungeon queuing for an MMORPG, handling party formation and dungeon instance allocation with concurrency control.

## Overview

This program simulates an MMORPG's Looking for Group (LFG) system where players queue for dungeons as different roles (Tank, Healer, DPS) and are grouped into parties that enter available dungeon instances.

## Features

- **Concurrent Dungeon Management**: Supports up to `n` concurrent dungeon instances
- **Standard Party Composition**: Forms parties with 1 Tank, 1 Healer, and 3 DPS
- **Deadlock Prevention**: Uses ReentrantLock and Condition variables for safe concurrency
- **Starvation Prevention**: Fair allocation of dungeon instances
- **Random Dungeon Duration**: Dungeon clear times vary between user-defined bounds
- **Real-time Status Tracking**: Monitors active/empty instances and group progress

## Requirements

- Java 8 or higher
- No external dependencies required

## How to Use

### Input Parameters

When running the program, you will be prompted to enter:

1. **n** - Maximum number of concurrent dungeon instances
2. **t** - Number of tank players in the queue
3. **h** - Number of healer players in the queue
4. **d** - Number of DPS players in the queue
5. **t1** - Minimum time before a dungeon instance is finished (seconds)
6. **t2** - Maximum time before a dungeon instance is finished (seconds)

**Note**: For testing purposes, t2 should be ≤ 15 seconds.

### Example Usage

```
Enter maximum concurrent dungeon instances: 3
Enter number of available tanks: 5
Enter number of available healers: 5
Enter number of available DPS: 15
Enter minimum clear time (seconds): 5
Enter maximum clear time (seconds): 10
```

## Program Output

The program provides:

1. **Real-time Dungeon Status**: Shows which dungeons are ACTIVE or EMPTY
2. **Group Formation Messages**: Tracks when groups enter and clear dungeons
3. **Final Simulation Report**: Summary including:
   - Number of groups processed per dungeon
   - Total service time per dungeon
   - Remaining unused players

## Concurrency Design

The solution ensures thread safety through:

- **ReentrantLock**: Controls access to shared resources
- **Condition Variables**: Manages waiting/notification for dungeon availability
- **Atomic Operations**: Prevents race conditions during party formation
- **Proper Lock Scoping**: Minimal critical sections to maximize throughput

## Key Components

### DungeonRun Class
- Tracks individual dungeon instance state
- Monitors groups completed and total active time

### Main Functions
- `manageQueue()`: Core simulation loop that forms parties and assigns dungeons
- `processDungeonRun()`: Handles individual dungeon runs in separate threads
- `checkPartyComposition()`: Validates if enough players are available for a party

## Limitations

- Fixed party composition (1-1-3)
- First-come-first-served queue processing
- No role priority or flexible party sizes
- Input values are processed as arriving simultaneously

## Error Handling

- Input validation for dungeon instances and time ranges
- Thread interruption handling
- Resource cleanup on simulation completion

## Compilation and Execution

```bash
javac lfg.java
java lfg
```

Follow the on-screen prompts to configure and run the simulation.
