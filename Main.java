package Server;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        switch (args.length) {
            case 1:
                if ("SERVER".equals(args[0].toUpperCase())) {
                    System.out.print("Enter path file directory (or skip to use ./send): ");
                    String pathFile = sc.nextLine();
                    System.out.print("Enter port (or skip to use port 9090): ");
                    int port = sc.nextInt();
                    Server server = new Server(pathFile, port);
                } else {
                    System.err.println("If you want to use server");
                    System.err.println("Usage: java Main server");
                }
                break;
            case 0:
                System.out.print("Enter host (or skip to use localhost): ");
                String host = sc.nextLine();
                System.out.print("Enter port: ");
                int port = sc.nextInt();
                System.out.print("Enter path file directory (or skip to use ./mailBox): ");
                String pathFile = sc.nextLine();
                Client server = new Client(host, port, pathFile);
                break;
            default:
                System.out.println("Usage: java Main");
                break;
        }
    }
}
