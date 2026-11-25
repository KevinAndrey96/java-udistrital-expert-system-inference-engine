package edu.udistrital.expert.engine;

import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.Rule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Audit and explanation facility for reasoning steps in the inference engine.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class InferenceTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class TraceEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        private final int cycle;
        private final String eventType;
        private final String message;

        public TraceEvent(int cycle, String eventType, String message) {
            this.cycle = cycle;
            this.eventType = eventType;
            this.message = message;
        }

        public int getCycle() {
            return cycle;
        }

        public String getEventType() {
            return eventType;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            if (cycle >= 0) {
                return String.format("[Ciclo %02d][%s] %s", cycle, eventType, message);
            }
            return String.format("[%s] %s", eventType, message);
        }
    }

    private final List<TraceEvent> events = new ArrayList<>();
    private final StringBuilder rawLog = new StringBuilder();

    public void log(int cycle, String eventType, String message) {
        TraceEvent event = new TraceEvent(cycle, eventType, message);
        events.add(event);
        rawLog.append(event.toString()).append("\n");
    }

    public void log(String eventType, String message) {
        log(-1, eventType, message);
    }

    public void logInfo(String message) {
        log("INFO", message);
    }

    public void logCycle(int cycle, List<Rule> candidates, Rule selected) {
        StringBuilder sb = new StringBuilder();
        sb.append(candidates.size()).append(" reglas candidatas: [");
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(candidates.get(i).getName());
        }
        sb.append("] -> Seleccionada: ").append(selected != null ? selected.getName() : "Ninguna");
        log(cycle, "CONFLICT_RESOLUTION", sb.toString());
    }

    public void logRuleFired(int cycle, Rule rule, Fact deducedFact) {
        String msg = String.format("Disparada regla %s -> Hecho deducido: %s (CF: %.3f)",
                rule.getName(), deducedFact.toString(), deducedFact.getCertaintyFactor());
        log(cycle, "RULE_FIRED", msg);
    }

    public void logGoalReached(String goalName, Fact fact) {
        log("GOAL_REACHED", String.format("Meta lograda: %s = %s con CF=%.3f",
                goalName, fact.getValue(), fact.getCertaintyFactor()));
    }

    public void logBackwardStep(int depth, String message) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }
        log("BACKWARD", indent.toString() + message);
    }

    public List<TraceEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public String getFormattedTrace() {
        return rawLog.toString();
    }

    public void clear() {
        events.clear();
        rawLog.setLength(0);
    }

    @Override
    public String toString() {
        return getFormattedTrace();
    }
}
