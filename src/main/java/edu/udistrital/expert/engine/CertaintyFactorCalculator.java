package edu.udistrital.expert.engine;

import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.Premise;
import edu.udistrital.expert.model.Rule;

import java.util.List;

/**
 * Mathematical calculations for Certainty Factors (Factores de Certeza)
 * based on the Shortliffe/Buchanan (MYCIN) probabilistic model.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class CertaintyFactorCalculator {

    /**
     * Calculates the certainty of conjunctive premises:
     * CF(P1 AND P2 AND ... AND Pn) = min(CF(P1), CF(P2), ..., CF(Pn))
     */
    public static double evaluateConjunctivePremises(Rule rule, List<Fact> facts) {
        if (rule == null || rule.getPremises().isEmpty() || facts == null || facts.isEmpty()) {
            return 0.0;
        }

        double minCertainty = 1.0;
        for (Premise premise : rule.getPremises()) {
            double premiseCert = 0.0;
            boolean matched = false;
            for (Fact fact : facts) {
                if (premise.evaluate(fact)) {
                    premiseCert = fact.getCertaintyFactor();
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return 0.0;
            }
            minCertainty = Math.min(minCertainty, premiseCert);
        }
        return minCertainty;
    }

    /**
     * Deduces hypothesis certainty from rule certainty and premise certainty:
     * CF(H) = CF_rule * CF(Premises)
     */
    public static double deduceFactCertainty(Rule rule, double premisesCertainty) {
        if (rule == null) return 0.0;
        return rule.getRuleCertainty() * premisesCertainty;
    }

    /**
     * Combines multiple independent confirmations of the same hypothesis:
     * If CF1 >= 0 and CF2 >= 0:
     *   CF_combined = CF1 + CF2 - (CF1 * CF2)
     * If CF1 < 0 and CF2 < 0:
     *   CF_combined = CF1 + CF2 + (CF1 * CF2)
     * If CF1 * CF2 < 0:
     *   CF_combined = (CF1 + CF2) / (1 - min(|CF1|, |CF2|))
     */
    public static double combineCertainties(double cf1, double cf2) {
        if (cf1 >= 0 && cf2 >= 0) {
            return cf1 + cf2 - (cf1 * cf2);
        } else if (cf1 < 0 && cf2 < 0) {
            return cf1 + cf2 + (cf1 * cf2);
        } else {
            double denom = 1.0 - Math.min(Math.abs(cf1), Math.abs(cf2));
            if (denom == 0) return 0.0;
            return (cf1 + cf2) / denom;
        }
    }
}
