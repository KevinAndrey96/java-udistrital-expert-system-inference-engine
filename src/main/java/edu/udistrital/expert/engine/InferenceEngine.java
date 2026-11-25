package edu.udistrital.expert.engine;

import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.KnowledgeBase;
import edu.udistrital.expert.model.Premise;
import edu.udistrital.expert.model.Rule;

import java.io.Serializable;
import java.util.*;

/**
 * Core Inference Engine supporting Forward Chaining, Backward Chaining,
 * Saturation, and Hybrid Chaining with Shortliffe Certainty Factors.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class InferenceEngine implements Serializable {

    private static final long serialVersionUID = 1L;

    public interface FactProvider {
        Fact promptFact(Premise premise);
    }

    private final KnowledgeBase knowledgeBase;
    private FactProvider factProvider;

    public InferenceEngine(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase != null ? knowledgeBase : new KnowledgeBase();
    }

    public InferenceEngine(KnowledgeBase knowledgeBase, FactProvider factProvider) {
        this.knowledgeBase = knowledgeBase != null ? knowledgeBase : new KnowledgeBase();
        this.factProvider = factProvider;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public FactProvider getFactProvider() {
        return factProvider;
    }

    public void setFactProvider(FactProvider factProvider) {
        this.factProvider = factProvider;
    }

    /**
     * Executes Forward Chaining (Encadenamiento hacia Adelante) driven by initial facts.
     * Stops when the goal is reached or when no more rules can fire.
     *
     * @param goalAttribute Target fact name to infer (null or blank for full saturation)
     * @param strategy Conflict resolution strategy
     * @return InferenceResult with full trace and newly inferred facts
     */
    public InferenceResult inferForward(String goalAttribute, ConflictResolutionStrategy strategy) {
        long startTime = System.currentTimeMillis();
        InferenceTrace trace = new InferenceTrace();
        trace.logInfo("Inicio de Encadenamiento hacia Adelante. Estrategia: " + strategy.getDescription());

        List<Fact> workingFacts = new ArrayList<>(knowledgeBase.getFactBase());
        List<Fact> newlyInferred = new ArrayList<>();
        List<Rule> firedRules = new ArrayList<>();
        Set<Integer> firedRuleNumbers = new HashSet<>();

        trace.logInfo("Base de Hechos inicial (" + workingFacts.size() + " hechos): " + workingFacts);

        // Check if goal is already present
        if (goalAttribute != null && !goalAttribute.trim().isEmpty()) {
            Fact existingGoal = findFact(workingFacts, goalAttribute.trim());
            if (existingGoal != null) {
                trace.logGoalReached(goalAttribute, existingGoal);
                long duration = System.currentTimeMillis() - startTime;
                return new InferenceResult(goalAttribute, true, existingGoal, firedRules, newlyInferred,
                        trace.getFormattedTrace(), duration);
            }
        }

        int cycle = 1;
        boolean progress = true;

        while (progress) {
            // Find applicable candidate rules that haven't fired yet
            List<Rule> conflictSet = new ArrayList<>();
            for (Rule rule : knowledgeBase.getRuleBase()) {
                if (!firedRuleNumbers.contains(rule.getRuleNumber()) && rule.isApplicable(workingFacts)) {
                    conflictSet.add(rule);
                }
            }

            if (conflictSet.isEmpty()) {
                trace.log(cycle, "HALT", "Conjunto de conflicto vacio. No hay mas reglas aplicables.");
                break;
            }

            // Select rule using the chosen strategy
            Rule selectedRule = strategy.select(conflictSet);
            trace.logCycle(cycle, conflictSet, selectedRule);

            if (selectedRule == null) {
                break;
            }

            // Fire rule
            firedRuleNumbers.add(selectedRule.getRuleNumber());
            firedRules.add(selectedRule);

            // Calculate certainty factor
            double premiseCert = CertaintyFactorCalculator.evaluateConjunctivePremises(selectedRule, workingFacts);
            double deducedCert = CertaintyFactorCalculator.deduceFactCertainty(selectedRule, premiseCert);

            Fact rawConsequent = selectedRule.getConsequent();
            Fact existingFact = findFact(workingFacts, rawConsequent.getName());

            Fact resultingFact;
            if (existingFact != null) {
                double combinedCert = CertaintyFactorCalculator.combineCertainties(
                        existingFact.getCertaintyFactor(), deducedCert);
                resultingFact = new Fact(existingFact.getName(), rawConsequent.getValue(),
                        rawConsequent.getDescription(), combinedCert);
                workingFacts.remove(existingFact);
                workingFacts.add(resultingFact);
                newlyInferred.remove(existingFact);
                newlyInferred.add(resultingFact);
                trace.log(cycle, "FACT_UPDATED", String.format("Hecho '%s' reconfirmado con CF combinado: %.3f (previo: %.3f, nuevo aporte: %.3f)",
                        resultingFact.getName(), combinedCert, existingFact.getCertaintyFactor(), deducedCert));
            } else {
                resultingFact = new Fact(rawConsequent.getName(), rawConsequent.getValue(),
                        rawConsequent.getDescription(), deducedCert);
                workingFacts.add(resultingFact);
                newlyInferred.add(resultingFact);
                trace.logRuleFired(cycle, selectedRule, resultingFact);
            }

            // Check if goal reached
            if (goalAttribute != null && !goalAttribute.trim().isEmpty() &&
                    resultingFact.getName().equalsIgnoreCase(goalAttribute.trim())) {
                trace.logGoalReached(goalAttribute, resultingFact);
                long duration = System.currentTimeMillis() - startTime;
                return new InferenceResult(goalAttribute, true, resultingFact, firedRules, newlyInferred,
                        trace.getFormattedTrace(), duration);
            }

            cycle++;
        }

        long duration = System.currentTimeMillis() - startTime;
        Fact finalGoalFact = (goalAttribute != null && !goalAttribute.trim().isEmpty()) ?
                findFact(workingFacts, goalAttribute.trim()) : null;

        boolean success = finalGoalFact != null;
        if (!success && goalAttribute != null && !goalAttribute.trim().isEmpty()) {
            trace.logInfo("Meta '" + goalAttribute + "' no pudo ser alcanzada por encadenamiento hacia adelante.");
        } else if (goalAttribute == null || goalAttribute.trim().isEmpty()) {
            trace.logInfo("Saturacion completa finalizada. Hechos inferidos: " + newlyInferred.size());
            success = true;
        }

        return new InferenceResult(goalAttribute != null ? goalAttribute : "SATURATION",
                success, finalGoalFact, firedRules, newlyInferred, trace.getFormattedTrace(), duration);
    }

    /**
     * Executes Full Forward Saturation (Saturacion de la Base de Hechos).
     */
    public InferenceResult saturate(ConflictResolutionStrategy strategy) {
        return inferForward(null, strategy);
    }

    /**
     * Executes Backward Chaining (Encadenamiento hacia Atras) driven by a goal hypothesis.
     * Recursively verifies sub-goals with cycle detection and certainty combination.
     *
     * @param goalAttribute Target proposition/attribute to prove
     * @return InferenceResult with resolution path and proof status
     */
    public InferenceResult inferBackward(String goalAttribute) {
        long startTime = System.currentTimeMillis();
        InferenceTrace trace = new InferenceTrace();
        trace.logInfo("Inicio de Encadenamiento hacia Atras para meta: '" + goalAttribute + "'");

        List<Fact> workingFacts = new ArrayList<>(knowledgeBase.getFactBase());
        List<Fact> newlyInferred = new ArrayList<>();
        List<Rule> firedRules = new ArrayList<>();
        Set<String> activeGoals = new HashSet<>();

        Fact provenFact = resolveGoal(goalAttribute, workingFacts, newlyInferred, firedRules, activeGoals, trace, 0);

        long duration = System.currentTimeMillis() - startTime;
        boolean success = provenFact != null;

        if (success) {
            trace.logGoalReached(goalAttribute, provenFact);
        } else {
            trace.logInfo("Meta '" + goalAttribute + "' no pudo ser demostrada.");
        }

        return new InferenceResult(goalAttribute, success, provenFact, firedRules, newlyInferred,
                trace.getFormattedTrace(), duration);
    }

    private Fact resolveGoal(String goalAttribute, List<Fact> workingFacts, List<Fact> newlyInferred,
                            List<Rule> firedRules, Set<String> activeGoals, InferenceTrace trace, int depth) {
        if (goalAttribute == null || goalAttribute.trim().isEmpty()) {
            return null;
        }

        String normalizedGoal = goalAttribute.trim();
        trace.logBackwardStep(depth, "Evaluando objetivo: " + normalizedGoal);

        // 1. Direct fact in working facts
        Fact existing = findFact(workingFacts, normalizedGoal);
        if (existing != null) {
            trace.logBackwardStep(depth, "-> Hecho ya conocido en BH: " + existing);
            return existing;
        }

        // 2. Cycle detection
        if (activeGoals.contains(normalizedGoal.toLowerCase())) {
            trace.logBackwardStep(depth, "-> [CICLO DETECTADO] El objetivo '" + normalizedGoal + "' ya esta en la pila. Poda activada.");
            return null;
        }

        activeGoals.add(normalizedGoal.toLowerCase());

        // 3. Find candidate rules concluding this goal
        List<Rule> candidateRules = knowledgeBase.findRulesConcluding(normalizedGoal);
        trace.logBackwardStep(depth, "Reglas que concluyen '" + normalizedGoal + "': " + candidateRules.size());

        Fact accumulatedFact = null;

        for (Rule rule : candidateRules) {
            trace.logBackwardStep(depth, "Intentando validar regla " + rule.getName() + ": " + rule);
            boolean allPremisesSatisfied = true;
            double minPremiseCert = 1.0;

            for (Premise premise : rule.getPremises()) {
                trace.logBackwardStep(depth + 1, "Verificando premisa: " + premise);
                Fact premiseFact = findFact(workingFacts, premise.getAttribute());

                // If premise is not in facts, try to deduce it recursively
                if (premiseFact == null) {
                    premiseFact = resolveGoal(premise.getAttribute(), workingFacts, newlyInferred,
                            firedRules, activeGoals, trace, depth + 2);
                }

                // If still null, check interactive fact provider
                if (premiseFact == null && factProvider != null) {
                    premiseFact = factProvider.promptFact(premise);
                    if (premiseFact != null) {
                        workingFacts.add(premiseFact);
                        newlyInferred.add(premiseFact);
                        trace.logBackwardStep(depth + 1, "-> Hecho provisto por usuario: " + premiseFact);
                    }
                }

                if (premiseFact != null && premise.evaluate(premiseFact)) {
                    minPremiseCert = Math.min(minPremiseCert, premiseFact.getCertaintyFactor());
                    trace.logBackwardStep(depth + 1, "-> Premisa satisfecha por " + premiseFact + " (CF min: " + minPremiseCert + ")");
                } else {
                    trace.logBackwardStep(depth + 1, "-> Premisa NO satisfecha para " + premise);
                    allPremisesSatisfied = false;
                    break;
                }
            }

            if (allPremisesSatisfied) {
                double deducedCert = CertaintyFactorCalculator.deduceFactCertainty(rule, minPremiseCert);
                Fact concl = rule.getConsequent();

                if (accumulatedFact == null) {
                    accumulatedFact = new Fact(concl.getName(), concl.getValue(), concl.getDescription(), deducedCert);
                } else {
                    double combinedCert = CertaintyFactorCalculator.combineCertainties(
                            accumulatedFact.getCertaintyFactor(), deducedCert);
                    accumulatedFact.setCertaintyFactor(combinedCert);
                }

                if (!firedRules.contains(rule)) {
                    firedRules.add(rule);
                }

                trace.logBackwardStep(depth, "-> Regla " + rule.getName() + " exitosa. Hipotesis '" +
                        concl.getName() + "=" + concl.getValue() + "' con CF=" + accumulatedFact.getCertaintyFactor());
            }
        }

        activeGoals.remove(normalizedGoal.toLowerCase());

        if (accumulatedFact != null) {
            workingFacts.add(accumulatedFact);
            newlyInferred.add(accumulatedFact);
        }

        return accumulatedFact;
    }

    /**
     * Executes Hybrid Chaining: First forward propagates immediate deductive consequences,
     * then executes backward chaining targeting the goal with the enriched knowledge.
     */
    public InferenceResult inferHybrid(String goalAttribute) {
        long startTime = System.currentTimeMillis();
        InferenceTrace trace = new InferenceTrace();
        trace.logInfo("Inicio de Encadenamiento Mixto (Hibrido) para meta: '" + goalAttribute + "'");

        // 1. Forward pass with highest specificity
        InferenceResult forwardPass = inferForward(goalAttribute, ConflictResolutionStrategy.MOST_PREMISES);
        if (forwardPass.isSuccess()) {
            trace.logInfo("Meta alcanzada directamente en fase hacia adelante.");
            long duration = System.currentTimeMillis() - startTime;
            return new InferenceResult(goalAttribute, true, forwardPass.getResultingFact(),
                    forwardPass.getFiredRules(), forwardPass.getNewlyInferredFacts(),
                    forwardPass.getTraceLog(), duration);
        }

        // 2. Backward pass targeting goal
        trace.logInfo("Fase hacia adelante no alcanzo la meta. Iniciando fase hacia atras...");
        InferenceResult backwardPass = inferBackward(goalAttribute);

        List<Rule> combinedRules = new ArrayList<>(forwardPass.getFiredRules());
        for (Rule r : backwardPass.getFiredRules()) {
            if (!combinedRules.contains(r)) {
                combinedRules.add(r);
            }
        }

        List<Fact> combinedFacts = new ArrayList<>(forwardPass.getNewlyInferredFacts());
        for (Fact f : backwardPass.getNewlyInferredFacts()) {
            if (!combinedFacts.contains(f)) {
                combinedFacts.add(f);
            }
        }

        String fullTrace = forwardPass.getTraceLog() + "\n" + backwardPass.getTraceLog();
        long duration = System.currentTimeMillis() - startTime;

        return new InferenceResult(goalAttribute, backwardPass.isSuccess(), backwardPass.getResultingFact(),
                combinedRules, combinedFacts, fullTrace, duration);
    }

    private Fact findFact(List<Fact> facts, String name) {
        if (facts == null || name == null) return null;
        for (Fact f : facts) {
            if (f.getName().equalsIgnoreCase(name.trim())) {
                return f;
            }
        }
        return null;
    }
}
