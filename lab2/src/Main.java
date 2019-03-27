import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.UDP;

public class Main extends ReceiverAdapter{

    public static void main(String[] args) throws Exception {
        new UDP().setValue("mcast_group_addr", InetAddress.getByName("230.100.200.23"));
        eventsLoop();
    }

    private static void eventsLoop() throws Exception{
        DistributedMap distributedMap = new DistributedMap();
        distributedMap.start("ChatCluster");
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                String line=in.readLine();
                if(line.startsWith("quit")) {
                    distributedMap.end();
                }
                if(line.startsWith("start")){
                    distributedMap.start(line.split(" ")[1]);
                }
                String arg = line.split(" ")[0];
                switch (arg) {
                    case "put":
                        Integer value = Integer.valueOf(line.split(" ")[2]);
                        distributedMap.put(line.split(" ")[1], value);
                        distributedMap.send(line);
                        break;
                    case "remove":
                        distributedMap.remove(line.split(" ")[1]);
                        distributedMap.send(line);
                        break;
                    case "get":
                        System.out.println(distributedMap.get(line.split(" ")[1]));
                        break;
                    case "contains":
                        System.out.println(distributedMap.containsKey(line.split(" ")[1]));
                        break;
                    case "print":
                        distributedMap.printAll();
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}