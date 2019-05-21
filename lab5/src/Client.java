import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Client extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                            System.out.println(s); // stream results
                        }
                )
                .match(OrderWrapper.class, orderWrapper-> {
                    System.out.println(orderWrapper.getOrderDone());
                })
                .match(FinderWrapper.class, finderWrapper -> {
                    String dbnumber = finderWrapper.getDbNumber();
                    String price = finderWrapper.getPrice();
                    if (price.equals("unavailable")) {
                        System.out.println("db" + dbnumber + ": " + price);
                    } else {
                        System.out.println("db" + dbnumber + ": price: " + price);
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    public static void main(String[] args) throws Exception {
        // create actor system & actors
        File configFile = new File("client"+args[0]+".conf");
        Config config = ConfigFactory.parseFile(configFile);
        final ActorSystem system = ActorSystem.create("local_system", config);
        final ActorSelection actor = system.actorSelection("akka.tcp://library@127.0.0.1:2552/user/ServerActor");
        final ActorRef me = system.actorOf(Props.create(Client.class), "client");
        System.out.println("Started. Commands: 'find', 'order', 'stream'. q to quit");
        // read line & send to actor
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = br.readLine();
            if (line.equals("q")) {
                break;
            }
            actor.tell(line, me);
        }
        // finish
        system.terminate();
    }
}