# Distributed Min/Max Algorithms

A Java project exploring how multiple processes can communicate to determine the global minimum and maximum of their individual values. It implements three communication strategies and includes a simple benchmarking mode to compare their execution times.

Developed as part of my Concurrent Programming coursework at KTH Royal Institute of Technology.

## Implemented algorithms

- **Centralized:** One coordinator collects values from the other processes, determines the minimum and maximum, and sends the result back.
- **Symmetric (all-to-all):** Every process sends its value to all other processes and computes the minimum and maximum locally.
- **Ring:** Processes communicate in a logical ring. The first pass finds the global minimum and maximum; the second pass distributes the final result.

## Technical concepts

- Java processes and TCP socket communication (`Socket` and `ServerSocket`)
- Coordination using a startup barrier
- Distributed message passing and algorithm comparison
- Execution-time benchmarking with CSV output

## How to run

**Requirements:** A Java Development Kit (JDK) installed, with `javac` and `java` available in your terminal.

Compile the program:

```bash
javac ExchangingValues.java
```

Run the default demonstration (four processes, one round, ring algorithm):

```bash
java ExchangingValues
```

To try the centralized or symmetric algorithm, change the `launchProcesses(...)` call in the no-argument section of `main` to use `"centralized"` or `"symmetric"`, then recompile and run.

Run all built-in benchmark configurations:

```bash
java ExchangingValues test
```

Benchmark results are written to `results.csv`, with columns for algorithm, number of processes, number of rounds, and execution time in milliseconds. The built-in test covers 2, 4, 6, and 8 processes, with 1, 2, and 3 rounds.

**Note:** The processes communicate over `localhost` on TCP ports starting at `5000`.

## Project scope

This repository contains the implementation of a university programming assignment. The focus is on comparing communication patterns and practicing inter-process communication, rather than providing a production-ready distributed systems library.
