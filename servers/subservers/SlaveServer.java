package servers.subservers;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import config.ConfigLoader;

public class SlaveServer {

    private String slaveId;
    private String host; // IP du SlaveServer
    private int port; // Port du SlaveServer
    private ServerSocket serverSocket;
    private String dirPath;
    private Socket socket; // Socket pour se connecter au MasterServer
    private ConfigLoader configLoader;

    // slaveId eg : slave1, slave2
    public SlaveServer(String configPath, String slaveId) {
        this.setSlaveId(slaveId);
        ConfigLoader configLoader = new ConfigLoader(configPath);
        this.setConfigLoader(configLoader);
        this.setHost(configLoader.getSlaveHost(slaveId));
        this.setPort(configLoader.getSlavePort(slaveId));
        this.setDirPath(configLoader.getSlaveDirPath(slaveId));
        updateSaveDir();
    }

    public SlaveServer(Socket socket) {
        this.setSocket(socket);
    }

    public void start() {
        System.out.println("SlaveServer starting...");

        try {
            // Démarrage du ServerSocket
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName(host));
            System.out.println("SlaveServer running at " + host + ":" + port);
            System.out.println("Waiting for MasterServer connections...");

            // Boucle infinie pour gérer les connexions
            while (true) {
                // Accepter une connexion du MasterServer
                socket = serverSocket.accept();
                System.out.println("MasterServer connected: " + socket.getInetAddress());

                // Démarrer un thread pour gérer cette connexion
                Thread masterHandler = new Thread(() -> handleMasterConnection(socket));
                masterHandler.start();
            }

        } catch (IOException e) {
            System.err.println("Error in SlaveServer: " + e.getMessage());
        } finally {
            closeServerSocket();
        }
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing ServerSocket: " + e.getMessage());
        }
    }

    private void handleMasterConnection(Socket masterSocket) {
        try (
                DataInputStream inputStream = new DataInputStream(masterSocket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(masterSocket.getOutputStream())) {
            System.out.println("Handling communication with MasterServer...");

            // Lire et traiter les messages du MasterServer
            String message;
            while ((message = inputStream.readUTF()) != null) {
                System.out.println("Message from MasterServer: " + message);

                // Traiter la commande UPLOAD
                if (message.startsWith("UPLOAD")) {
                    String fileName = inputStream.readUTF(); // Nom du fichier à recevoir
                    long fileSize = inputStream.readLong(); // Taille du fichier à recevoir
                    System.out
                            .println("Commande UPLOAD reçue. Nom du fichier : " + fileName + ", Taille : " + fileSize);

                    // Stocker le fichier dans le répertoire local
                    String saveDir = configLoader.getSlaveDirPath(this.getSlaveId()); 
                                                                                      
                    File file = new File(saveDir, fileName);

                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        long totalRead = 0;
                        int bytesRead;

                        while (totalRead < fileSize && (bytesRead = inputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }

                        if (totalRead == fileSize) {
                            System.out.println("Fichier " + fileName + " reçu avec succès.");
                            outputStream.writeUTF("UPLOAD_SUCCESS " + fileName);
                        } else {
                            System.err.println("Erreur : Fichier " + fileName + " incomplet.");
                            outputStream.writeUTF("UPLOAD_FAILED " + fileName);
                        }
                    } catch (IOException e) {
                        System.err.println("Erreur lors de la réception du fichier : " + e.getMessage());
                        outputStream.writeUTF("UPLOAD_FAILED " + fileName);
                    }
                } else if (message.startsWith("DOWNLOAD_PART")) {
                    // Extraire le nom du fichier demandé
                    String fileName = inputStream.readUTF();
                    System.out.println("Master requested file: " + fileName);

                    // Vérifier si le fichier existe localement
                    File file = new File(configLoader.getSlaveDirPath(this.getSlaveId()), fileName);
                    if (!file.exists()) {
                        System.err.println("File " + fileName + " not found.");
                        outputStream.writeUTF("ERROR: File not found.");
                        outputStream.flush();
                        continue;
                    }

                    // Lire le contenu du fichier
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        byte[] fileData = fileInputStream.readAllBytes();

                        System.out.println("File name: " + fileName);
                        System.out.println("Actual file size: " + file.length());
                        System.out.println("Sending file size: " + fileData.length);

                        // Envoyer la taille et les données
                        outputStream.writeInt(fileData.length);
                        outputStream.flush();

                        System.out.println("Sending file data...");
                        outputStream.write(fileData);
                        outputStream.flush();

                        System.out.println("File data sent successfully.");
                    } catch (IOException e) {
                        System.err.println("Error sending file: " + e.getMessage());
                    }

                } else if ("REMOVE_PART".equalsIgnoreCase(message)) {
                    String fileName = inputStream.readUTF();
                    System.out.println("Deleting part of the file:" + fileName);

                    // Définir l'emplacement du fichier sur le slave
                    File fileToRemove = new File(this.getDirPath(), fileName);

                    // Vérifier si le fichier existe
                    if (fileToRemove.exists()) {
                        // Supprimer le fichier
                        boolean deleted = fileToRemove.delete();

                        // Envoyer une confirmation de la suppression au master
                        if (deleted) {
                            System.out.println("File part successfully deleted: " + fileName);
                            outputStream.writeBoolean(true); 
                        } else {
                            System.out.println("Failed to delete file part: " + fileName);
                            outputStream.writeBoolean(false);
                        }
                    } else {
                        System.out.println("File part not found: " + fileName);
                        outputStream.writeBoolean(false); 
                    }
                    outputStream.flush();

                }
            }

        } catch (IOException e) {
            System.err.println("Connection with MasterServer lost: " + e.getMessage());
        }
    }

    private void updateSaveDir() {
        try {
            File dirCheck = new File(this.getConfigLoader().getSlaveDirPath(this.getSlaveId()));
            if (!dirCheck.exists()) {
                dirCheck.mkdir();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters et Setters
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

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public String getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(String slaveId) {
        this.slaveId = slaveId;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public void setConfigLoader(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }
}
