package client;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private String serverHost;
    private int serverPort;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public Client(String host, int port) throws Exception {
        this.setServerHost(host);
        this.setServerPort(port);
    }

    public void connectToServer() throws Exception {
        try {
            this.setSocket(new Socket(this.getServerHost(), this.getServerPort()));
            DataInputStream input = new DataInputStream(this.getSocket().getInputStream());
            DataOutputStream output = new DataOutputStream(this.getSocket().getOutputStream());

            System.out.println("Connected to the server at " + this.getServerHost() + ":" + this.getServerPort());

            // Lire le message de bienvenue
            String serverMessage = input.readUTF();
            System.out.println("\nMessage du serveur: " + serverMessage);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                // Affichage des options et interaction utilisateur
                String command = displayMenuAndGetChoice(scanner);

                switch (command) {
                    case "1":
                        output.writeUTF("LISTING");
                        String listing = input.readUTF();
                        System.out.println("Liste an'ireo fichiers ao amin'ny serveur: " + listing);
                        break;
                    case "2":
                        System.out.println("Sorato ny lalana feno makany amin'ny fichier ho alefa: ");
                        String filePath = scanner.nextLine();
                        File file = new File(filePath);
                        if (file.exists() && file.isFile()) {
                            output.writeUTF("UPLOAD");
                            sendFile(filePath, output, input);
                            String uploadResponse = input.readUTF();
                            System.out.println("Server: " + uploadResponse);
                        } else {
                            System.out.println("Hamarino tsara ny path nosoratanao. Avereno indray.");
                        }
                        break;
                    case "3":
                        System.out.println("Sorato ny anaran'ny fichier tianao ho alaina: ");
                        String fileName = scanner.nextLine();
                        System.out.println("Ampidiro ny lalana feno tianao ametrahana ny fichier: ");
                        String savePath = scanner.nextLine();
                        output.writeUTF("DOWNLOAD");
                        output.writeUTF(fileName);
                        receiveFile(fileName, savePath, input, output);
                        break;
                    case "4":
                        System.out.println("Sorato eto ny anaran'ny fichier tianao hofafaina: ");
                        String fileNameRm = scanner.nextLine();
                        output.writeUTF("REMOVE");
                        output.writeUTF(fileNameRm);
                        break;
                    case "5":
                        output.writeUTF("EXIT");
                        System.out.println("Miala amin'ny fifandraisana...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("\nDiso safidy enao ah. Avereno azafady.");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Connection lost or server is unavailable: " + e.getMessage());
        }
    }

    private String displayMenuAndGetChoice(Scanner scanner) {
        System.out.println("\nSafidio izay tianao (ex : 1): ");
        System.out.println("1. (list) Mijery ny liste an'ireo fichiers ao amin'ny serveur");
        System.out.println("2. (upload) Mandefa fichier makany amin'ny serveur");
        System.out.println("3. (download) Maka fichier ao amin'ny serveur");
        System.out.println("4. (remove) Mamafa fichier ao amin'ny serveur");
        System.out.println("5. (exit) Hiala");
        System.out.print("Sorato eto ny chiffre mifandray amin'izay tianao atao: ");
        return scanner.nextLine();
    }

    // Receive a file from the server
    public void receiveFile(String fileName, String savePath, DataInputStream inputStream, DataOutputStream output)
            throws IOException {
        String serverResponse = inputStream.readUTF();
        if (!serverResponse.equalsIgnoreCase("DOWNLOAD_READY")) {
            System.out.println(serverResponse);
            return;
        }

        File saveFile = new File(savePath, fileName);
        long fileSize = inputStream.readLong();
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {
            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead = 0;
            while (totalRead < fileSize && (bytesRead = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }
            System.out.println("Vita ny download: " + saveFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.out.println("Error: Could not create file for writing.");
        } catch (IOException e) {
            System.out.println("Error: Could not download file. " + e.getMessage());
        }
    }

    // Mandefa fichier makany amin'ny serveur
    public void sendFile(String filePath, DataOutputStream out, DataInput input) {
        try (
                FileInputStream fileInputStream = new FileInputStream(filePath)) {

            File file = new File(filePath);

            // Send file metadata
            out.writeUTF(file.getName());
            out.writeLong(file.length());

            // Send file data
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Lasa soa amantsara ny fichier!");
            // System.out.println("Server response: "+input.readUTF());

        } catch (IOException e) {
            System.err.println("Misy olana eo ampandefasana ny fichier: " + e.getMessage());
        }

    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public DataInputStream getInput() {
        return input;
    }

    public void setInput(DataInputStream input) {
        this.input = input;
    }

    public DataOutputStream getOutput() {
        return output;
    }

    public void setOutput(DataOutputStream output) {
        this.output = output;
    }

}
