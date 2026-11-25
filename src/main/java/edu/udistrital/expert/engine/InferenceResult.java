package edu.udistrital.expert.engine;

import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.Rule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of an inference execution.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class InferenceResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String targetGoal;
    private boolean success;
    private Fact resultingFact;
    private List<Rule> firedRules;
    private List<Fact> newlyInferredFacts;
    private String traceLog;
    private long executionTimeMs;

    public InferenceResult() {
        this("", false, null, new ArrayList<>(), new ArrayList<>(), "", 0);
    }

    public InferenceResult(String targetGoal, boolean success, Fact resultingFact,
                           List<Rule> firedRules, List<Fact> newlyInferredFacts,
                           String traceLog, long executionTimeMs) {
        this.targetGoal = targetGoal != null ? targetGoal : "";
        this.success = success;
        this.resultingFact = resultingFact;
        this.firedRules = firedRules != null ? new ArrayList<>(firedRules) : new ArrayList<>();
        this.newlyInferredFacts = newlyInferredFacts != null ? new ArrayList<>(newlyInferredFacts) : new ArrayList<>();
        this.traceLog = traceLog != null ? traceLog : "";
        this.executionTimeMs = executionTimeMs;
    }

    public String getTargetGoal() {
        return targetGoal;
    }

    public boolean isSuccess() {
        return success;
    }

    public Fact getResultingFact() {
        return resultingFact;
    }

    public List<Rule> getFiredRules() {
        return Collections.unmodifiableList(firedRules);
    }

    public List<Fact> getNewlyInferredFacts() {
        return Collections.unmodifiableList(newlyInferredFacts);
    }

    public String getTraceLog() {
        return traceLog;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("InferenceResult [Goal=").append(targetGoal)
          .append(", Success=").append(success);
        if (resultingFact != null) {
            sb.append(", Result=").append(resultingFact);
        }
        sb.append(", FiredRules=").append(firedRules.size())
          .append(", NewFacts=").append(newlyInferredFacts.size())
          .append(", Time=").append(executionTimeMs).append("ms]");
        return sb.toString();
    }
}
