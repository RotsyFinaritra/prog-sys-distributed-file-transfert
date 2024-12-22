import servers.subservers.SlaveServer;

public class Sub2Main {
    public static void main(String[] args) {
        SlaveServer slaveServer = new SlaveServer("config/config.properties", "slave2");
        slaveServer.start();
    }
}
