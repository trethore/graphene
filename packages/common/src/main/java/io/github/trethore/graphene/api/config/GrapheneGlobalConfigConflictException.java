package io.github.trethore.graphene.api.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Thrown when consumers contribute incompatible process-wide Graphene configuration. */
public final class GrapheneGlobalConfigConflictException extends IllegalStateException {
  @Serial private static final long serialVersionUID = 1L;

  private final Setting setting;
  private final Contribution[] contributions;

  public GrapheneGlobalConfigConflictException(Setting setting, List<Contribution> contributions) {
    super(message(setting, contributions));
    this.setting = setting;
    this.contributions = contributions.toArray(Contribution[]::new);
  }

  public Setting setting() {
    return setting;
  }

  public List<Contribution> contributions() {
    return List.of(contributions);
  }

  private static String message(Setting setting, List<Contribution> contributions) {
    Setting validatedSetting = Objects.requireNonNull(setting, "setting");
    List<Contribution> validatedContributions =
        Objects.requireNonNull(contributions, "contributions");
    if (validatedContributions.size() < 2) {
      throw new IllegalArgumentException("contributions must contain at least two entries");
    }
    String contributionSummary =
        validatedContributions.stream()
            .map(contribution -> contribution.consumerId() + " -> " + contribution.value())
            .collect(Collectors.joining("; "));
    return "Conflicting process-wide Graphene setting "
        + validatedSetting
        + ": "
        + contributionSummary;
  }

  private static void requireText(String value, String name) {
    String validatedValue = Objects.requireNonNull(value, name);
    if (validatedValue.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  /** A consumer and the value it contributed to a conflicting setting. */
  public record Contribution(String consumerId, String value) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public Contribution {
      requireText(consumerId, "consumerId");
      requireText(value, "value");
    }
  }

  /** Process-wide setting that may conflict between consumers. */
  public enum Setting {
    BROWSER_RUNTIME_PATH,
    REMOTE_DEBUGGING,
    BROWSER_FILE_ACCESS_POLICY
  }
}
