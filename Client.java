import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException {

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Enter 'publisher' or 'subscriber':");
            String choice = scanner.nextLine();

            if (choice.equalsIgnoreCase("publisher")) {
                System.out.println("Enter publisher name:");
                String publisherName = scanner.nextLine();

                System.out.println("Enter publisher port:");
                int publisherPort = scanner.nextInt();
                scanner.nextLine();

                try (Socket socket = new Socket("localhost", publisherPort)) {
                    OutputStream outputStream = socket.getOutputStream();

                    while (true) {
                        String message = scanner.nextLine();

                        String fullMessage = publisherName + ": " + message;

                        outputStream.write(fullMessage.getBytes());
                        outputStream.flush();
                    }
                }
            } else if (choice.equalsIgnoreCase("subscriber")) {
                System.out.println("Enter subscriber port:");
                int subscriberPort = scanner.nextInt();
                scanner.nextLine();

                System.out.println("Enter publisher name to subscribe to:");
                String publisherName = scanner.nextLine();

                try (Socket socket = new Socket("localhost", subscriberPort)) {
                    OutputStream outputStream = socket.getOutputStream();
                    InputStream inputStream = socket.getInputStream();

                    // Send the publisher name to the server
                    outputStream.write(publisherName.getBytes());
                    outputStream.flush();

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        String message = new String(buffer, 0, bytesRead);
                        System.out.println("Received message from server: " + message);
                    }
                }

            } else {
                System.out.println("Invalid choice");
            }
        }
    }
}
