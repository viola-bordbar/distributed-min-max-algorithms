# Distributed Min/Max Algorithms

A Java project exploring how multiple processes communicate to determine the global minimum and maximum of their individual values. The project implements three communication strategies and includes a benchmarking mode to compare their execution times.

Developed as part of the Concurrent Programming coursework at KTH Royal Institute of Technology.

## Implemented Algorithms

- **Centralized:** One coordinator collects values from the other processes, determines the global minimum and maximum, and sends the results back to all processes.
- **Symmetric (all-to-all):** Every process sends its value to all other processes and independently computes the global minimum and maximum.
- **Ring:** Processes communicate in a logical ring. The first phase determines the global minimum and maximum, while the second phase distributes the results to all processes.

## Technical Concepts

- Inter-process communication using Java TCP sockets (`Socket` and `ServerSocket`)
- Process coordination using a startup barrier
- Distributed message passing and algorithm comparison
- Execution-time benchmarking with CSV output

## How to Run

**Requirements:** Java Development Kit (JDK) installed, with `javac` and `java` available in your terminal.

### Compile

From the project directory, compile the source file:

```bash
javac ExchangingValues.java
```

### Run the default demonstration

```bash
java ExchangingValues
```

The default configuration runs four processes, one round, using the ring algorithm.

### Run a different algorithm

To run the centralized or symmetric algorithm, change the `launchProcesses()` call in the no-argument section of `main()` to use `"centralized"` or `"symmetric"`, respectively. Recompile and run the program.

### Run benchmarks

```bash
java ExchangingValues test
```

The benchmark tests all three algorithms using 2, 4, 6 and 8 processes, with 1, 2 and 3 rounds.

Results are written to `results.csv`, containing the algorithm, number of processes, number of rounds and execution time in milliseconds.

**Note:** Processes communicate over `localhost` using TCP ports starting at `5000`.

## Project Scope

This repository contains the implementation of a university programming assignment. The focus is on comparing communication patterns and practising inter-process communication, rather than providing a production-ready distributed systems library.

