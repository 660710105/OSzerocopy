package zerocopy;

import java.util.Scanner;
import zerocopy.client.*;
import zerocopy.server.*;

public class Main {
        static int port = 9090;
        static String directory = "./mailbox";
        static String sendDirectory = "./send";
        static String remoteHost = "localhost";
        static boolean runServer = false;

        public static void printUsages() {
                System.out.print("examples: \n" +
                                 " <app_name> [ -s {-d, -D} <directory> -p <port> -h ]\n" +
                                 "usages: \n" +
                                 " --server, -s         run this as a server.\n" +
                                 " --directory, -d      set mailbox directory.\n" +
                                 " --send-directory, -D set send directory.\n" +
                                 " --port, -p           set port to connect or listening.\n" +
                                 " --host, -h           set remote host.\n"
                                 );
        }
        
        public static void main(String[] args) {
                Scanner sc = new Scanner(System.in);
                int argsIdx = 0;
                while(argsIdx < args.length) {
                        switch (args[argsIdx]) {
                        case "--port":
                        case "-p":
                                if (argsIdx + 1 >= args.length) {
                                        printUsages();
                                        return;
                                }
                                port = Integer.parseInt(args[argsIdx + 1]);
                                argsIdx += 2;
                                break;
                        case "--directory":
                        case "-d":
                                if (argsIdx + 1 >= args.length) {
                                        printUsages();
                                        return;
                                }
                                directory = args[argsIdx + 1];
                                argsIdx += 2;
                                break;
                        case "--send-directory":
                        case "-D":
                                if (argsIdx + 1 >= args.length) {
                                        printUsages();
                                        return;
                                }
                                sendDirectory = args[argsIdx + 1];
                                argsIdx += 2;
                                break;
                        case "--host":
                        case "-h":
                                if (argsIdx + 1 >= args.length) {
                                        printUsages();
                                        return;
                                }
                                remoteHost = args[argsIdx + 1];
                                argsIdx += 2;
                                break;
                        case "--server":
                        case "-s":
                                runServer = true;
                                argsIdx++;
                                break;
                        default:
                                printUsages();
                                return;
                        } 
                }

                System.out.println("server: " + runServer + ", host: " + remoteHost + ", port: " + port + ", dir: " + directory + ", send dir: " + sendDirectory);
                
                try {
                        if (runServer) {
                                Thread server = new Thread(new Server(sendDirectory, port));
                                server.run();
                        } else {
                                Thread client = new Thread(new Client(remoteHost, port, directory));
                                client.run();
                        }
                } catch (Exception e) {
                        
                }
                
                sc.close();
        }
}
