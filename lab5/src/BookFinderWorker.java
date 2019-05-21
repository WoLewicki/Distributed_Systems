import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;

public class BookFinderWorker extends AbstractActor {

   private String dbNumber;
   private String price;
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    findBook(s);
                    getSender().tell(new FinderWrapper(dbNumber, price), getSelf());
                    getContext().stop(getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void findBook(String s){
        dbNumber = s.substring(0,1);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("db"+dbNumber));
            String line = reader.readLine();
            while (line != null) {
                if (line.split(" ")[0].equals(s.substring(1)))
                {
                    price = line.split(" ")[1];
                    return;
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        price = "unavailable";
    }

}