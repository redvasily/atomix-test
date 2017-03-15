package atomixtest;

import io.atomix.AtomixReplica;
import io.atomix.catalyst.concurrent.Listener;
import io.atomix.catalyst.transport.Address;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import io.atomix.variables.AbstractDistributedValue;
import io.atomix.variables.DistributedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.toList;

@Service
public class Main {

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private final AtomicBoolean keepGoing = new AtomicBoolean(true);
  private final AtomixConfig atomixConfig;
  private Thread thread;
  private final Netloc netloc;

  private static class Netloc {
    final String host;
    final int port;

    public Netloc(String netlocStr) {
      final String[] parts = netlocStr.split(":");
      host = parts[0];
      port = Integer.valueOf(parts[1]);
    }

    @Override
    public String toString() {
      return "Netloc{" +
          "host='" + host + '\'' +
          ", port=" + port +
          '}';
    }
  }

  @Inject
  public Main(AtomixConfig atomixConfig) {
    this.atomixConfig = atomixConfig;
    this.netloc = new Netloc(atomixConfig.getReplicas().get(atomixConfig.getReplicaNumber()));
  }

  @PostConstruct
  public void start() {
    log.info("Started");
    thread = new Thread(this::main, "main");
    thread.start();
  }

  @PreDestroy
  public void stop() throws InterruptedException {
    log.info("Stopping");
    keepGoing.set(false);
    thread.join(5000L);
    log.info("Stopped");
  }

  private void main() {
    log.info("Atomix config: {}", atomixConfig);
    log.info("Netloc: {}", netloc);

    final Storage storage = Storage.builder()
        .withDirectory(new File(
            atomixConfig.getStorageLocation(),
            Integer.toString(atomixConfig.getReplicaNumber())))
        .withStorageLevel(StorageLevel.DISK)
        .build();

    final AtomixReplica replica = AtomixReplica.builder(new Address(netloc.host, netloc.port))
        .withStorage(storage)
        .build();

    final List<Address> cluster = atomixConfig.getReplicas().stream()
        .map(netlocStr -> {
          Netloc netloc = new Netloc(netlocStr);
          return new Address(netloc.host, netloc.port);
        })
        .collect(toList());

    log.info("Bootstrapping: {}", cluster);
    final CompletableFuture<AtomixReplica> bootstrapFuture = replica.bootstrap(cluster);
    bootstrapFuture.join();
    log.info("Bootstrapped");

    final DistributedLong distributedLong = replica.getLong("long-value").join();
    final ThreadLocalRandom random = ThreadLocalRandom.current();

    if (!atomixConfig.isIncrement()) {
      log.info("Setting up an onChange listener");
      distributedLong.onChange(change ->
          log.info("onChange: {} -> {}", change.oldValue(), change.newValue()))
          .join();
      log.info("Set up an onChange listener");
    }

    while (keepGoing.get()) {
      if (atomixConfig.isIncrement()) {
        log.info("Incrementing");
        try {
          final Long newValue = distributedLong.incrementAndGet()
              .get(atomixConfig.getIncrementTimeoutSec(), TimeUnit.SECONDS);
          log.info("Incremented. New value: {}", newValue);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          log.error("Incrementing error", e);
        }
      }
      try {
        Thread.sleep(random.nextLong(1000, 3000));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    log.info("Thread is done");
  }
}
