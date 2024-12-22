import servers.subservers.SlaveServer;

public class Sub3Main {
    public static void main(String[] args) {
        SlaveServer slaveServer = new SlaveServer("config/config.properties", "slave3");
        slaveServer.start();
    }
}
