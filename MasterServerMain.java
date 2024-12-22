import servers.principal.MasterServer;

public class MasterServerMain {
    public static void main(String[] args) {
        MasterServer masterServer = new MasterServer("config/config.properties");
        masterServer.start();
    }
}
