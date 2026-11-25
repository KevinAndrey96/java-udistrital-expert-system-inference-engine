package edu.udistrital.expert.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Antecedent condition of an inference rule.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class Premise implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Operator {
        EQUALS("="),
        NOT_EQUALS("!="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_OR_EQUAL(">="),
        LESS_OR_EQUAL("<=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Operator fromSymbol(String symbol) {
            if (symbol == null) return EQUALS;
            for (Operator op : values()) {
                if (op.symbol.equals(symbol.trim())) {
                    return op;
                }
            }
            return EQUALS;
        }
    }

    private String attribute;
    private Operator operator;
    private String targetValue;
    private String promptQuestion;

    public Premise() {
        this("", Operator.EQUALS, "true", "");
    }

    public Premise(String attribute) {
        this(attribute, Operator.EQUALS, "true", attribute + "?");
    }

    public Premise(String attribute, String targetValue) {
        this(attribute, Operator.EQUALS, targetValue, attribute + " = " + targetValue + "?");
    }

    public Premise(String attribute, Operator operator, String targetValue, String promptQuestion) {
        this.attribute = attribute != null ? attribute.trim() : "";
        this.operator = operator != null ? operator : Operator.EQUALS;
        this.targetValue = targetValue != null ? targetValue.trim() : "true";
        this.promptQuestion = promptQuestion != null ? promptQuestion.trim() : "";
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute != null ? attribute.trim() : "";
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator != null ? operator : Operator.EQUALS;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue != null ? targetValue.trim() : "true";
    }

    public String getPromptQuestion() {
        return promptQuestion;
    }

    public void setPromptQuestion(String promptQuestion) {
        this.promptQuestion = promptQuestion != null ? promptQuestion.trim() : "";
    }

    /**
     * Evaluates if a given Fact satisfies this premise.
     */
    public boolean evaluate(Fact fact) {
        if (fact == null || !this.attribute.equalsIgnoreCase(fact.getName())) {
            return false;
        }

        String factVal = fact.getValue();

        switch (operator) {
            case EQUALS:
                return factVal.equalsIgnoreCase(targetValue);
            case NOT_EQUALS:
                return !factVal.equalsIgnoreCase(targetValue);
            case GREATER_THAN:
            case LESS_THAN:
            case GREATER_OR_EQUAL:
            case LESS_OR_EQUAL:
                try {
                    double numFact = Double.parseDouble(factVal);
                    double numTarget = Double.parseDouble(targetValue);
                    if (operator == Operator.GREATER_THAN) return numFact > numTarget;
                    if (operator == Operator.LESS_THAN) return numFact < numTarget;
                    if (operator == Operator.GREATER_OR_EQUAL) return numFact >= numTarget;
                    if (operator == Operator.LESS_OR_EQUAL) return numFact <= numTarget;
                } catch (NumberFormatException e) {
                    return false;
                }
            default:
                return factVal.equalsIgnoreCase(targetValue);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Premise premise = (Premise) o;
        return attribute.equalsIgnoreCase(premise.attribute) &&
                operator == premise.operator &&
                targetValue.equalsIgnoreCase(premise.targetValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute.toLowerCase(), operator, targetValue.toLowerCase());
    }

    @Override
    public String toString() {
        return attribute + " " + operator.getSymbol() + " " + targetValue;
    }
}
