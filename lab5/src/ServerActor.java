import akka.actor.*;

import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.Duration;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerActor extends AbstractActor {

    // for logging
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    // must be implemented -> creates initial behaviour
    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    if (s.startsWith("find")) {
                        getContext().actorOf(Props.create(BookFinderWorker.class)).forward(("1" + s.split(" ")[1]), getContext()); // send task to child
                        getContext().actorOf(Props.create(BookFinderWorker.class)).forward(("2" + s.split(" ")[1]), getContext()); // send task to child
                    } else if (s.startsWith("stream")) {
                        getContext().actorOf(Props.create(BookStreamerWorker.class)).forward(s, getContext()); // send task to child
                    } else if (s.startsWith("order")) {
                        getContext().actorOf(Props.create(BookOrderWorker.class)).forward(s, getContext()); // send task to child
                    }
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private static SupervisorStrategy strategy
            = new OneForOneStrategy(10, Duration.create("1 minute"), DeciderBuilder.
                    matchAny(o -> {
                        if(o instanceof IOException)
                        {
                            System.out.println("There is no such book.");
                            return resume();
                        }
                        else return restart();
                    }).
                    build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public static void main(String[] args) throws Exception {
        File configFile = new File("server.conf");
        Config config = ConfigFactory.parseFile(configFile);
        final ActorSystem system = ActorSystem.create("library", config);
        system.actorOf(Props.create(ServerActor.class), "ServerActor");
        System.out.println("Server: Started. 'q' to kill.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String line = br.readLine();
            if (line.equals("q")) {
                system.terminate();
                break;
            }
        }
    }



}
