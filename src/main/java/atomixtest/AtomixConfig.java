package atomixtest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "atomix")
public class AtomixConfig {
  private List<String> replicas;
  private String storageLocation;
  private int replicaNumber;
  private boolean increment;
  private int incrementTimeoutSec;

  @Override
  public String toString() {
    return "AtomixConfig{" +
        "replicas=" + replicas +
        ", storageLocation='" + storageLocation + '\'' +
        ", replicaNumber=" + replicaNumber +
        ", increment=" + increment +
        ", incrementTimeoutSec=" + incrementTimeoutSec +
        '}';
  }

  public int getIncrementTimeoutSec() {
    return incrementTimeoutSec;
  }

  public void setIncrementTimeoutSec(int incrementTimeoutSec) {
    this.incrementTimeoutSec = incrementTimeoutSec;
  }

  public boolean isIncrement() {
    return increment;
  }

  public void setIncrement(boolean increment) {
    this.increment = increment;
  }

  public List<String> getReplicas() {
    return replicas;
  }

  public void setReplicas(List<String> replicas) {
    this.replicas = replicas;
  }

  public String getStorageLocation() {
    return storageLocation;
  }

  public void setStorageLocation(String storageLocation) {
    this.storageLocation = storageLocation;
  }

  public int getReplicaNumber() {
    return replicaNumber;
  }

  public void setReplicaNumber(int replicaNumber) {
    this.replicaNumber = replicaNumber;
  }
}
