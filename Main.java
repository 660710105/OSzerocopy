
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try{
            switch (args.length) {
                case 1:
                    if ("SERVER".equals(args[0].toUpperCase())) {
                        System.out.print("Enter path file directory (or skip to use .\\send): ");
                        String pathFile = sc.nextLine();
                        System.out.print("Enter port (or skip to use port 9090): ");
                        String p = sc.nextLine();
                        int port = (p.isEmpty())? 0: Integer.parseInt(p);
                        Server server = new Server(pathFile, port);
                        server.run();
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
                sc.nextLine();
                System.out.print("Enter path file directory (or skip to use .\\mailBox): ");
                String pathFile = sc.nextLine();
                Client client = new Client(host, port, pathFile);
                client.run();
                break;
            default:
                System.out.println("Usage: java Main [server]");
                break;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
