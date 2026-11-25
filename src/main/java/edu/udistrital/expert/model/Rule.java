package edu.udistrital.expert.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representation of an inference rule: IF Premise1 AND ... AND PremiseN THEN Consequent.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class Rule implements Serializable, Comparable<Rule> {

    private static final long serialVersionUID = 1L;

    private int ruleNumber;
    private String name;
    private String description;
    private List<Premise> premises;
    private Fact consequent; // The hypothesis or fact deduced if all premises hold
    private double ruleCertainty; // CF of the rule itself [0.0, 1.0]
    private int priority; // Explicit priority for conflict resolution

    public Rule() {
        this(0, "R0", new ArrayList<>(), new Fact(), 1.0, 0);
    }

    public Rule(int ruleNumber, String name, List<Premise> premises, Fact consequent) {
        this(ruleNumber, name, premises, consequent, 1.0, 0);
    }

    public Rule(int ruleNumber, String name, List<Premise> premises, Fact consequent, double ruleCertainty, int priority) {
        this.ruleNumber = ruleNumber;
        this.name = name != null ? name.trim() : "R" + ruleNumber;
        this.description = "";
        this.premises = premises != null ? new ArrayList<>(premises) : new ArrayList<>();
        this.consequent = consequent != null ? consequent : new Fact();
        this.ruleCertainty = Math.max(-1.0, Math.min(1.0, ruleCertainty));
        this.priority = priority;
    }

    public int getRuleNumber() {
        return ruleNumber;
    }

    public void setRuleNumber(int ruleNumber) {
        this.ruleNumber = ruleNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public List<Premise> getPremises() {
        return Collections.unmodifiableList(premises);
    }

    public void addPremise(Premise premise) {
        if (premise != null && !premises.contains(premise)) {
            this.premises.add(premise);
        }
    }

    public void setPremises(List<Premise> premises) {
        this.premises = premises != null ? new ArrayList<>(premises) : new ArrayList<>();
    }

    public Fact getConsequent() {
        return consequent;
    }

    public void setConsequent(Fact consequent) {
        this.consequent = consequent != null ? consequent : new Fact();
    }

    public double getRuleCertainty() {
        return ruleCertainty;
    }

    public void setRuleCertainty(double ruleCertainty) {
        this.ruleCertainty = Math.max(-1.0, Math.min(1.0, ruleCertainty));
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPremiseCount() {
        return premises.size();
    }

    /**
     * Verifies if all premises are satisfied by the current Fact Base.
     */
    public boolean isApplicable(List<Fact> facts) {
        if (premises.isEmpty() || facts == null || facts.isEmpty()) {
            return false;
        }

        for (Premise premise : premises) {
            boolean satisfied = false;
            for (Fact fact : facts) {
                if (premise.evaluate(fact)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if this rule concludes the specified hypothesis / fact.
     */
    public boolean concludes(String factName) {
        if (consequent == null || factName == null) return false;
        return consequent.getName().equalsIgnoreCase(factName.trim());
    }

    public boolean concludes(String factName, String factValue) {
        if (consequent == null || factName == null) return false;
        return consequent.getName().equalsIgnoreCase(factName.trim()) &&
               consequent.getValue().equalsIgnoreCase(factValue != null ? factValue.trim() : "true");
    }

    @Override
    public int compareTo(Rule o) {
        // Natural ordering: highest priority first, then most premises first
        if (this.priority != o.priority) {
            return Integer.compare(o.priority, this.priority);
        }
        if (this.premises.size() != o.premises.size()) {
            return Integer.compare(o.premises.size(), this.premises.size());
        }
        return Integer.compare(this.ruleNumber, o.ruleNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return ruleNumber == rule.ruleNumber && name.equalsIgnoreCase(rule.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleNumber, name.toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": IF ");
        for (int i = 0; i < premises.size(); i++) {
            sb.append(premises.get(i));
            if (i < premises.size() - 1) {
                sb.append(" AND ");
            }
        }
        sb.append(" THEN ").append(consequent);
        if (ruleCertainty < 1.0) {
            sb.append(" (CF=").append(String.format("%.2f", ruleCertainty)).append(")");
        }
        return sb.toString();
    }
}
