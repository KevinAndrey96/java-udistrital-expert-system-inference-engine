package edu.udistrital.expert.engine;

import edu.udistrital.expert.model.Rule;

import java.util.Comparator;
import java.util.List;

/**
 * Conflict resolution strategy to select the next rule when multiple rules are applicable.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public enum ConflictResolutionStrategy {

    FIRST_RULE("Primera Regla Declarada") {
        @Override
        public Rule select(List<Rule> conflictSet) {
            if (conflictSet == null || conflictSet.isEmpty()) return null;
            return conflictSet.get(0);
        }
    },

    MOST_PREMISES("Mayor Especificidad (Mas Premisas)") {
        @Override
        public Rule select(List<Rule> conflictSet) {
            if (conflictSet == null || conflictSet.isEmpty()) return null;
            return conflictSet.stream()
                    .max(Comparator.comparingInt(Rule::getPremiseCount).thenComparingInt(r -> -r.getRuleNumber()))
                    .orElse(conflictSet.get(0));
        }
    },

    HIGHEST_CERTAINTY("Mayor Factor de Certeza") {
        @Override
        public Rule select(List<Rule> conflictSet) {
            if (conflictSet == null || conflictSet.isEmpty()) return null;
            return conflictSet.stream()
                    .max(Comparator.comparingDouble(Rule::getRuleCertainty).thenComparingInt(Rule::getPremiseCount))
                    .orElse(conflictSet.get(0));
        }
    },

    HIGHEST_PRIORITY("Mayor Prioridad Explicita") {
        @Override
        public Rule select(List<Rule> conflictSet) {
            if (conflictSet == null || conflictSet.isEmpty()) return null;
            return conflictSet.stream()
                    .max(Comparator.comparingInt(Rule::getPriority).thenComparingInt(Rule::getPremiseCount))
                    .orElse(conflictSet.get(0));
        }
    };

    private final String description;

    ConflictResolutionStrategy(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public abstract Rule select(List<Rule> conflictSet);
}
