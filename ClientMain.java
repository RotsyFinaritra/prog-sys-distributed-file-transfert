import client.Client;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        try {
            Client client = new Client("localhost", 10000);
            client.connectToServer();
        } catch (Exception e) {
            System.err.println("Client terminated: " + e.getMessage());
        }
    }
}
