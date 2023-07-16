import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
    private ServerSocket subscriberServerSocket;
    private ServerSocket publisherServerSocket;
    private Map<String, List<Socket>> publishers;
    private List<SubscriberHandler> subscriberHandlers;

    public Server(int subscriberPort, int publisherPort) {
        publishers = new HashMap<>();
        subscriberHandlers = new ArrayList<>();
        try {
            subscriberServerSocket = new ServerSocket(subscriberPort);
            publisherServerSocket = new ServerSocket(publisherPort);
            System.out.println("Server started and listening on ports: Subscriber-" + subscriberPort + ", Publisher-" + publisherPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        Thread subscriberThread = new Thread(new SubscriberConnectionHandler());
        subscriberThread.start();

        Thread publisherThread = new Thread(new PublisherConnectionHandler());
        publisherThread.start();
    }

    public void sendToSubscribers(String publisherName, String message) {
        List<Socket> subscriberSockets = publishers.get(publisherName);
        if (subscriberSockets != null) {
            for (Socket socket : subscriberSockets) {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class SubscriberConnectionHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket clientSocket = subscriberServerSocket.accept();
                    System.out.println("New subscriber connected");

                    Thread t = new Thread(new SubscriberHandler(clientSocket));
                    t.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class PublisherConnectionHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Socket publisherSocket = publisherServerSocket.accept();
                    System.out.println("Publisher connected");

                    Thread t = new Thread(new PublisherHandler(publisherSocket));
                    t.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class SubscriberHandler implements Runnable {
        private Socket clientSocket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private String subscribedPublisher;

        public SubscriberHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                inputStream = clientSocket.getInputStream();
                outputStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;

                // Receive the publisher name to subscribe to
                bytesRead = inputStream.read(buffer);
                subscribedPublisher = new String(buffer, 0, bytesRead);

                synchronized (publishers) {
                    if (!publishers.containsKey(subscribedPublisher)) {
                        publishers.put(subscribedPublisher, new ArrayList<>());
                    }
                    publishers.get(subscribedPublisher).add(clientSocket);
                }

                subscriberHandlers.add(this);

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    System.out.println("Received message from publisher: " + message);
                    sendToSubscribers(subscribedPublisher, message);
                }

                System.out.println("Subscriber disconnected");
                synchronized (publishers) {
                    if (publishers.containsKey(subscribedPublisher)) {
                        publishers.get(subscribedPublisher).remove(clientSocket);
                        if (publishers.get(subscribedPublisher).isEmpty()) {
                            publishers.remove(subscribedPublisher);
                        }
                    }
                }
                subscriberHandlers.remove(this);
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class PublisherHandler implements Runnable {
        private Socket publisherSocket;
        private InputStream inputStream;

        public PublisherHandler(Socket publisherSocket) {
            this.publisherSocket = publisherSocket;
            try {
                inputStream = publisherSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    String message = new String(buffer, 0, bytesRead);
                    System.out.println("Received message from publisher: " + message);
                    for (SubscriberHandler handler : subscriberHandlers) {
                        if (handler.subscribedPublisher.equals(message.split(":")[0].trim())) {
                            handler.outputStream.write(message.getBytes());
                            handler.outputStream.flush();
                        }
                    }
                }
                System.out.println("Publisher disconnected");
                publisherSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server(8888, 9999);
        server.start();
    }
}
