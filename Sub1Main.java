import servers.subservers.SlaveServer;

public class Sub1Main {
    public static void main(String[] args) {
        SlaveServer slaveServer = new SlaveServer("config/config.properties", "slave1");
        slaveServer.start();
    }
}
