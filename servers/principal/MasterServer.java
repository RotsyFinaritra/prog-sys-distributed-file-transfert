package servers.principal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import config.ConfigLoader;
import servers.subservers.SlaveServer;

public class MasterServer {

    private String host;
    private int port;
    private ServerSocket serverSocket;
    private List<SlaveServer> activeSubs;
    private String configPath;
    private ConfigLoader configLoader;

    public MasterServer() {
    }

    public MasterServer(String configPath) {
        this.setConfigPath(configPath);
        ConfigLoader configLoader = new ConfigLoader(configPath);
        this.setConfigLoader(configLoader);
        this.setHost(configLoader.getMasterHost());
        this.setPort(configLoader.getMasterPort());
        this.setActiveSubs(new ArrayList<>());
    }

    public void start() {
        System.out.println("MasterServer starting...");

        // Charger la liste des sous-serveurs depuis le fichier de configuration
        int numOfSlaves = configLoader.getNumOfSlaves();
        boolean hasActiveSlaves = false;

        for (int i = 1; i <= numOfSlaves; i++) {
            String slaveId = "slave" + i;
            String slaveHost = configLoader.getSlaveHost(slaveId);
            int slavePort = configLoader.getSlavePort(slaveId);
            try {
                System.out.println("Checking connection to " + slaveId + " (" + slaveHost + ":" + slavePort + ")...");
                Socket socket = new Socket(slaveHost, slavePort); // Test de connexion
                socket.close(); // Fermer immédiatement le socket de test
                System.out.println("Slave " + slaveId + " is active.");

                // Ajouter ce sous-serveur à la liste des slaves actifs
                activeSubs.add(new SlaveServer(this.getConfigPath(), slaveId));
                hasActiveSlaves = true;

            } catch (IOException e) {
                System.err.println("Slave " + slaveId + " is unavailable.");
            }
        }

        if (!hasActiveSlaves) {
            System.out.println("No active slaves found. Shutting down MasterServer.");
            return; // Quitte le programme si aucun sous-serveur n'est disponible
        }

        System.out.println("Active slaves detected. MasterServer is running...");

        // Écoute des connexions entrantes pour un client
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;

            while (true) {
                System.out
                        .println("Waiting for a client connection at " + this.getHost() + ":" + this.getPort() + "...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Gestion de la connexion client (implémentez votre logique ici)
                new Thread(() -> handleClientRequests(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error with the MasterServer socket: " + e.getMessage());
        }
    }

    private void updateActiveSlaves() {
        System.out.println("Updating active slaves...");

        // Nettoyer la liste des slaves actifs
        activeSubs.clear();
        int numOfSlaves = configLoader.getNumOfSlaves();

        for (int i = 1; i <= numOfSlaves; i++) {
            String slaveId = "slave" + i;
            String slaveHost = configLoader.getSlaveHost(slaveId);
            int slavePort = configLoader.getSlavePort(slaveId);

            try {
                System.out.println("Checking connection to " + slaveId + " (" + slaveHost + ":" + slavePort + ")...");
                Socket socket = new Socket(slaveHost, slavePort); // Test de connexion
                socket.close(); // Fermer immédiatement le socket de test
                System.out.println("Slave " + slaveId + " is active.");

                // Ajouter ce sous-serveur à la liste des slaves actifs
                activeSubs.add(new SlaveServer(this.getConfigPath(), slaveId));
            } catch (IOException e) {
                System.err.println("Slave " + slaveId + " is unavailable.");
            }
        }

        System.out.println("Active slaves updated: " + activeSubs.size() + " active slave(s) found.");
    }

    public void handleClientRequests(Socket clientSocket) {
        Socket socketClient = clientSocket;
        try (DataInputStream in = new DataInputStream(socketClient.getInputStream());
                DataOutputStream out = new DataOutputStream(socketClient.getOutputStream())) {

            // Envoyer un message de bienvenue
            out.writeUTF("Tongasoa eto amin'ny Livai's Server!");
            System.out.println("Message de bienvenue envoyé à " + socketClient.getRemoteSocketAddress());

            String command;
            while ((command = in.readUTF()) != null && !socketClient.isClosed()) {
                System.out.println("Commande choisi: " + command);
                switch (command) {
                    case "LISTING":
                        handleListing(out);
                        break;

                    case "UPLOAD":
                        handleFileUpload(in, out);
                        break;

                    case "DOWNLOAD":
                        handleFileDownload(in, out);
                        break;

                    case "REMOVE":
                        handleFileRemove(in, out);
                        break;

                    case "EXIT":
                        System.out.println("Client disconnected: " + socketClient.getRemoteSocketAddress());
                        return;

                    default:
                        System.out.println("Commande inconnue reçue : " + command);
                        out.writeUTF("Commande inconnue : " + command);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur pendant le traitement des requêtes client : " + e.getMessage());
        }
    }

    private void handleSimpleMessage(DataInputStream in, DataOutputStream out) throws IOException {
        String message = in.readUTF();
        System.out.println("Message reçu du client : " + message);
        out.writeUTF("Message reçu : " + message);
    }

    private void handleListing(DataOutputStream out) throws IOException {
        File dir = new File(this.getConfigLoader().getSavePath()); // Répertoire où les fichiers sont stockés
        if (!dir.exists() || !dir.isDirectory()) {
            out.writeUTF("Répertoire introuvable ou invalide.");
            return;
        }

        String[] files = dir.list();
        if (files == null || files.length == 0) {
            out.writeUTF("Aucun fichier disponible.");
        } else {
            out.writeUTF(String.join(", ", files));
        }
    }

    private void handleFileUpload(DataInputStream in, DataOutputStream out) throws IOException {
        // Mettre à jour la liste des slaves actifs
        updateActiveSlaves();

        if (activeSubs.isEmpty()) {
            out.writeUTF("Aucun slave actif. Impossible de traiter le fichier.");
            return;
        }

        // Lire les informations sur le fichier depuis le client
        String fileName = in.readUTF();
        long fileSize = in.readLong();

        System.out.println("Réception du fichier " + fileName + " de taille " + fileSize + " octets.");

        // Diviser le fichier en parties pour chaque slave
        int numSlaves = activeSubs.size();
        long partSize = fileSize / numSlaves;
        long remainingBytes = fileSize % numSlaves;

        System.out.println("Fichier divisé en " + numSlaves + " parties, chacune de taille " + partSize + " octets.");

        // Transférer les parties du fichier aux slaves
        byte[] buffer = new byte[4096]; // Tampon pour les transferts
        long totalRead = 0;

        for (int i = 0; i < numSlaves; i++) {
            SlaveServer slave = activeSubs.get(i);

            long bytesToSend = partSize + (i == numSlaves - 1 ? remainingBytes : 0); // Ajouter les octets restants à la
                                                                                     // dernière partie
            System.out.println("Envoi de " + bytesToSend + " octets à " + slave.getSlaveId());

            try (Socket slaveSocket = new Socket(slave.getHost(), slave.getPort());
                    DataOutputStream slaveOut = new DataOutputStream(slaveSocket.getOutputStream())) {

                // Envoyer les métadonnées du fichier au slave
                slaveOut.writeUTF("UPLOAD");
                slaveOut.writeUTF(fileName);
                slaveOut.writeLong(bytesToSend);

                // Lire les données depuis le client et les envoyer au slave
                long sentBytes = 0;
                while (sentBytes < bytesToSend) {
                    int bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, bytesToSend - sentBytes));
                    if (bytesRead == -1) {
                        throw new IOException("Connexion interrompue par le client.");
                    }

                    slaveOut.write(buffer, 0, bytesRead);
                    sentBytes += bytesRead;
                    totalRead += bytesRead;
                }

                System.out.println("Partie envoyée avec succès à " + slave.getSlaveId());
            } catch (IOException e) {
                System.err.println("Erreur lors de l'envoi à " + slave.getSlaveId() + ": " + e.getMessage());
                out.writeUTF("Erreur lors de l'envoi d'une partie à " + slave.getSlaveId() + ".");
            }
        }

        // Confirmer la réception au client
        if (totalRead == fileSize) {
            out.writeUTF("Fichier " + fileName + " distribué avec succès aux slaves.");
        } else {
            out.writeUTF("Erreur : tous les octets du fichier n'ont pas été transmis.");
        }

        System.out.println("Fichier " + fileName + " distribué avec succès.");
    }

    private void handleFileDownload(DataInputStream in, DataOutputStream out) throws IOException {
        String fileName = in.readUTF();
        File file = new File(configLoader.getSavePath() + fileName);

        if (!file.exists()) {
            out.writeUTF("Fichier introuvable.");
            return;
        }

        out.writeUTF("Fichier trouvé.");
        out.writeLong(file.length());

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
        }

        System.out.println("Fichier " + fileName + " envoyé avec succès.");
    }

    private void handleFileRemove(DataInputStream in, DataOutputStream out) throws IOException {
        String fileName = in.readUTF();
        File file = new File(this.getConfigLoader().getSavePath() + fileName);

        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                out.writeUTF("Fichier supprimé avec succès.");
            } else {
                out.writeUTF("Échec de la suppression du fichier.");
            }
        } else {
            out.writeUTF("Fichier introuvable.");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public List<SlaveServer> getActiveSubs() {
        return activeSubs;
    }

    public void setActiveSubs(List<SlaveServer> activeSubs) {
        this.activeSubs = activeSubs;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public void setConfigLoader(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
