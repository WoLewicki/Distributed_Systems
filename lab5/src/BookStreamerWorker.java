import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.concurrent.duration.Duration;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BookStreamerWorker extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    @Override
    public AbstractActor.Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> streamBook(s))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void streamBook(String s) throws IOException {
            String filename = s.split(" ")[1];
            Stream<String> stream = Files.lines(Paths.get(filename));
            Source<String, NotUsed> source = Source.from(stream.collect(Collectors.toList()));
            source.throttle(1, Duration.create(1, TimeUnit.SECONDS), 1, ThrottleMode.shaping())
                    .runWith(Sink.actorRef(sender(), "DONE"), ActorMaterializer.create(getContext()));
    }
}