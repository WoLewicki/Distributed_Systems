import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

public class BookOrderWorker extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);


    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    orderBook(s);
                    getSender().tell(new OrderWrapper("done"), getSelf());
                    getContext().stop(getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void orderBook(String s) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("orders.txt", true));
        writer.append(s);
        writer.append("\n");
        writer.close();
    }

}