import java.net.*;
import java.io.*;
import java.util.*; 

public class ExchangingValues {

    // File to store benchmark results
    static final String RESULTS_FILE = "results.csv";

    // Base port for socket communication (each process will listen on BASE_PORT + id)
    static final int BASE_PORT = 5000;

    // Shared variables for min and max values
    static int min; 
    static int max;
    static int myValue;

    // Process ID, number of processes, number of rounds, benchmarking flag, and algorithm choice (args)
    static int id;
    static int n;
    static int rounds;
    static boolean benchmark; 
    static String algorithm; 

    //================================= MAIN FUNCTION =================================//
    public static void main (String[] args) throws Exception{

        // Arrays of parameters for benchmarking 
        String[] algorithms = {"centralized", "symmetric", "ring"};
        int[] processCounts = {2, 4, 6, 8};
        int[] roundCounts = {1, 2, 3};
        
        // One single run (specified)
        if(args.length == 0) {
            //launchProcesses(4, 1, "centralized", false);
           //launchProcesses(4, 1, "symmetric", false); 
           launchProcesses(4, 1, "ring", false);
            return;
        }

        // Run all benchmarks
        if(args.length == 1 && args[0].equalsIgnoreCase("test")) {
            runBenchmarks(algorithms, processCounts, roundCounts);
            return;
        }

        // If not all arguments are specified, print usage
        if(args.length != 5) {
            System.out.println("Usage:");
            System.out.println("       java ExchangingValues          (run one single instance of an algorithm)");
            System.out.println("       java ExchangingValues test     (run full benchmarks");
            System.out.println("       java ExchangingValues <id> <n> <rounds> <algorithm> <benchmark>");
            return;
        }

        // Parse command-line arguments
        id = Integer.parseInt(args[0]);
        n = Integer.parseInt(args[1]);
        rounds = Integer.parseInt(args[2]);
        algorithm = args[3]; 
        benchmark = Boolean.parseBoolean(args[4]);  
    
        // Initialize server socket and wait for all processes to be ready
        ServerSocket serverSocket = new ServerSocket(BASE_PORT + id); 
        startBarrier(serverSocket);

        // Initialize local value and start timer
        initializeValue();
        long startTime = System.nanoTime();

        // Execute the chosen algorithm
        if(algorithm.equals("centralized")) {
            centralized(serverSocket);
        } else if(algorithm.equals("symmetric")) {
            symmetric(serverSocket);
        } else if(algorithm.equals("ring")) {
            ring(serverSocket);
        } 

        // End timer, calculate duration, and print final result 
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime; 
        if(!benchmark) System.out.println("P" + id + " final result: Min = " + min + ", Max = " + max);

        // Log benchmark times to file (only if benchmarking is enabled)
        if(benchmark) {
            handleBenchmarkTimes(serverSocket, durationNs);
        }

        // Close the server socket after all communication is done
        serverSocket.close();
    }

    //================================= ALGORITHMS =================================//

    // Centralized algorithm: coordinator & workers
    static void centralized(ServerSocket serverSocket) throws Exception{
        for(int r = 0; r < rounds; r++) { 

            // Reset min and max to myValue at the start of each round
            min = myValue;
            max = myValue;
            
            if(id == 0) { 
                printEvent(id, r, "waiting for " + (n-1) + " values...");

                // P0 is the coordinator: receives values from all other processes
                for(int j = 1; j < n; j++) { 
                    int value = receiveInt(serverSocket);
                    printEvent(id, r, "received value " + value);

                    min = Math.min(min, value);
                    max = Math.max(max, value);
                    printEvent(id, r, "updated min to " + min + " and max to " + max);
                }

                // After max and min have been computed the coordinator sends the result back to all other processes
                printEvent(id, r, "broadcasting result to all processes...");
                for(int j = 1; j < n; j++) {
                    sendInt(j, min);
                    sendInt(j, max);
                }

            // Non-coordinator processes send their value to the coordinator and receive the final result
            } else {
                printEvent(id, r, "sending value " + myValue + " to coordinator...");
                sendInt(0, myValue); 

                min = receiveInt(serverSocket); 
                max = receiveInt(serverSocket); 
                printEvent(id, r, "received min & max from coordinator");
            }
        }

    }

    // Symmetric algorithm: all-to-all 
    static void symmetric(ServerSocket serverSocket) throws Exception{
        for(int r = 0; r < rounds; r++) {
            min = myValue;
            max = myValue;

            // Each process sends its value to all other processes
            printEvent(id, r, "sending value " + myValue + " to all other processes...");
            for(int j = 0; j < n; j++) {
                if(j != id) {
                    sendInt(j, myValue); 
                }
            }

            // Each process receives values from all other processes and updates min and max
            for(int j = 0; j < n - 1; j++) { 
                int value = receiveInt(serverSocket);
                printEvent(id, r, "received value " + value);

                min = Math.min(min, value);
                max = Math.max(max, value);
                printEvent(id, r, "updated min to " + min + " and max to " + max);
            }
        }

    }

    // Ring algorithm: recieve from previous, send to next (2 phases, 1 initiator)
    static void ring(ServerSocket serverSocket) throws Exception{
        int nextId = (id + 1) % n; 

        //Phase 1: find the global min and max
        for(int r = 0; r < rounds; r++) {
            min = myValue;
            max = myValue;

            if(id == 0) { 
                // P0 initiates ring communication
                printEvent(id, r, "Phase 1: initiating ring communication by sending value " + myValue + " to P" + nextId);
                sendInt(nextId, min); 
                sendInt(nextId, max);

                // P0 receives the final min and max after one full round
                min = receiveInt(serverSocket); 
                max = receiveInt(serverSocket);
                printEvent(id, r, "received final min & max from P" + ((id - 1 + n) % n));

            } else { 
                // Non-initiator processes receive min and max, updates them, and passes them on
                int currentMin = receiveInt(serverSocket); 
                int currentMax = receiveInt(serverSocket);
                printEvent(id, r, "received min " + currentMin + " and max " + currentMax + " from P" + ((id - 1 + n) % n));

                currentMin = Math.min(currentMin, myValue);
                currentMax = Math.max(currentMax, myValue);

                min = currentMin;
                max = currentMax;
                printEvent(id, r, "updated min to " + min + " and max to " + max);

                sendInt(nextId, min); 
                sendInt(nextId, max); 
            }

            //Phase 2: send the global min and max to all processes
            if(id == 0) { 
                // P0 initiates ring communication to distribute final result
                printEvent(id, r, "Phase 2: initiating ring communication to distribute global min and max...");
                sendInt(nextId, min);
                sendInt(nextId, max);

                receiveInt(serverSocket);
                receiveInt(serverSocket);
                printEvent(id, r, "received result back (Phase 2 done)");

            } else { 
                // Non-initiator processes receives the final result and passes it on
                min =receiveInt(serverSocket); 
                max = receiveInt(serverSocket);
                printEvent(id, r, "received final min " + min + " & max " + max + ", forwarding result...");

                sendInt(nextId, min); 
                sendInt(nextId, max); 
            }
           
        }

    }

    //================================= HELPER METHODS =================================//

    // Initialize myValue to a random integer and assume that value is both min and max at the start
    static void initializeValue() {
        myValue = (int)(Math.random() * 100); 
        min = myValue;
        max = myValue;
        if(!benchmark) System.out.println("P" + id + " = " + myValue); 
    }

    // For sending integers over sockets
    static void sendInt(int targetId, int value) throws Exception {
        Socket socket = new Socket ("localhost", BASE_PORT + targetId); 
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        output.writeInt(value); 
        output.flush();
        socket.close();
    }

    // For receiving integers over sockets
    static int receiveInt(ServerSocket serverSocket) throws Exception {
        Socket socket = serverSocket.accept(); 
        DataInputStream input = new DataInputStream(socket.getInputStream());
        int receivedValue = input.readInt(); 
        socket.close();
        return receivedValue;
    }

    // For creating a barrier to synchronize processes everytime it starts 
    static void startBarrier(ServerSocket serverSocket) throws Exception {
        // P0 waits for "ready" and sends "go"
        if(id == 0) {
            for(int i = 1; i < n; i++) {
                receiveInt(serverSocket); 
            }
            for(int i = 1; i < n; i++) {
                sendInt(i, 1); 
            }
        }
        // All others send "ready" and wait for "go"
        else {
            sendInt(0, 1); 
            receiveInt(serverSocket); 
        }

    }

    // For launching multiple processes
    static void launchProcesses(int numProcesses, int rounds, String algorithm, boolean benchmark) throws Exception {
        List<Process> processes = new ArrayList<>();

        for(int i = 0; i < numProcesses; i++) {  
            Process p = new ProcessBuilder(
                "java", 
                "ExchangingValues", 
                String.valueOf(i), 
                String.valueOf(numProcesses), 
                String.valueOf(rounds), 
                algorithm, 
                String.valueOf(benchmark)
            ).inheritIO().start();
            
            processes.add(p); 
            Thread.sleep(200); // Small delay to avoid port conflicts
        }

        // Wait for all processes to finish before starting the next benchmark
        for(Process p : processes) {
            p.waitFor();  
        }
    }

    // For printing events
    static void printEvent(int id, int r, String message) {
        if(!benchmark) System.out.println("P" + id + ", " + " r = " + r + ": " + message);
    }

    // For writing benchmark results to a file
    static void writeResultsToFile(String algorithm, int numProcesses, int rounds, int timeMs) throws IOException {
        try(FileWriter file = new FileWriter(RESULTS_FILE, true)) {
            file.write(algorithm + "," + numProcesses + "," + rounds + "," + timeMs + "\n");
        }
    }

    // For handling benchmark times (total execution time across all processes) neatly
    static void handleBenchmarkTimes(ServerSocket serverSocket, long durationNs) throws Exception {
        int durationMs = (int)(durationNs / 1000000);
        if(id == 0) {
            for(int i = 1; i < n; i++) {
                int other = receiveInt(serverSocket); 
                durationMs = Math.max(durationMs, other);
            }

            writeResultsToFile(algorithm, n, rounds, durationMs);
        }
        else {
            sendInt(0, durationMs);
        }
    }

    // For running all benchmark combinations 
    static void runBenchmarks(String[] algorithms, int[] processCounts, int[] roundCounts) throws Exception {
        try(FileWriter file = new FileWriter(RESULTS_FILE, false)) {
            file.write("algorithm, number of processes, number of rounds, time (ms)" + "\n");
        }

        for(String alg : algorithms) {
            for(int p : processCounts) {
                for(int r : roundCounts) {
                    launchProcesses(p, r, alg, true);
                }
            }
        }
        
        System.out.println("All benchmarks are done, check the CSV file!");
    }
}
