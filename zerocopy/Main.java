package zerocopy;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Iterator;

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
                         " <app_name> [ -s ] [ { -d, -D } <directory> ] [ -p <port>] [ -h ] ]\n" +
                         "usages: \n" +
                         " --server, -s         run this as a server.\n" +
                         " --directory, -d      set mailbox directory.\n" +
                         " --send-directory, -D set send directory.\n" +
                         " --port, -p           set port to connect or listening.\n" +
                         " --host, -h           set remote host.\n"
                         );
    }
        
    public static void main(String[] args) {
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

        StringBuilder infoStringBuilder = new StringBuilder();

        try {
            if (runServer) {
                infoStringBuilder
                    .append("Select hosting directory: " + sendDirectory + "\n")
                    .append("Now Server will listening is this following interfaces: \n");
                
                Enumeration<NetworkInterface> interfaceLookup = NetworkInterface.getNetworkInterfaces();
                while (interfaceLookup.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaceLookup.nextElement();
                    String networkInterfaceName = networkInterface.getDisplayName();
                    Enumeration<InetAddress> addressLookup = networkInterface.getInetAddresses();
                    while (addressLookup.hasMoreElements()) {
                        InetAddress address = addressLookup.nextElement();
                        infoStringBuilder.append(networkInterfaceName + ": " + address.getHostAddress() + ":" + port + "\n");
                    }
                }

                String info = infoStringBuilder.toString();
                System.out.println(info);
                
                Thread server = new Thread(new Server(sendDirectory, port));
                server.start();
            } else {
                String info = infoStringBuilder
                    .append("Selecting remote: " + remoteHost + " ")
                    .append("on port " + port + "\n")
                    .toString();

                System.out.println(info);
                
                Thread client = new Thread(new Client(remoteHost, port, directory));
                client.start();
            }
        } catch (SocketException exception) {
            exception.printStackTrace();
        }
    }
}
