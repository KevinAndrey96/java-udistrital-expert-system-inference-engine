package edu.udistrital.expert;

import edu.udistrital.expert.engine.CertaintyFactorCalculator;
import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.Premise;
import edu.udistrital.expert.model.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for Shortliffe / Buchanan (MYCIN) Certainty Factor calculations.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class CertaintyFactorTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testConjunctivePremisesCalculatesMinimum() {
        Rule rule = new Rule();
        rule.addPremise(new Premise("Fiebre", Premise.Operator.EQUALS, "alta", ""));
        rule.addPremise(new Premise("Tos", Premise.Operator.EQUALS, "seca", ""));
        rule.addPremise(new Premise("Dolor", Premise.Operator.EQUALS, "si", ""));

        Fact f1 = new Fact("Fiebre", "alta", "", 0.90);
        Fact f2 = new Fact("Tos", "seca", "", 0.65);
        Fact f3 = new Fact("Dolor", "si", "", 0.80);

        double minCert = CertaintyFactorCalculator.evaluateConjunctivePremises(
                rule, Arrays.asList(f1, f2, f3));

        assertEquals("Premise certainty should be minimum of conjunctive premises",
                0.65, minCert, EPSILON);
    }

    @Test
    public void testDeduceFactCertaintyMultipliesRuleAndPremiseCertainty() {
        Rule rule = new Rule();
        rule.setRuleCertainty(0.80);

        double premiseCertainty = 0.75;
        double deduced = CertaintyFactorCalculator.deduceFactCertainty(rule, premiseCertainty);

        assertEquals("Deduced certainty should be Rule CF * Premise CF",
                0.60, deduced, EPSILON);
    }

    @Test
    public void testCombineBothPositiveCertainties() {
        // Formula: CF1 + CF2 - (CF1 * CF2)
        // 0.80 + 0.60 - (0.80 * 0.60) = 1.40 - 0.48 = 0.92
        double cf1 = 0.80;
        double cf2 = 0.60;
        double combined = CertaintyFactorCalculator.combineCertainties(cf1, cf2);

        assertEquals(0.92, combined, EPSILON);
    }

    @Test
    public void testCombineBothNegativeCertainties() {
        // Formula: CF1 + CF2 + (CF1 * CF2)
        // -0.50 + -0.40 + (-0.50 * -0.40) = -0.90 + 0.20 = -0.70
        double cf1 = -0.50;
        double cf2 = -0.40;
        double combined = CertaintyFactorCalculator.combineCertainties(cf1, cf2);

        assertEquals(-0.70, combined, EPSILON);
    }

    @Test
    public void testCombineMixedSignsCertainties() {
        // Formula: (CF1 + CF2) / (1 - min(|CF1|, |CF2|))
        // CF1 = 0.80, CF2 = -0.40
        // (0.80 - 0.40) / (1 - 0.40) = 0.40 / 0.60 = 0.666667
        double cf1 = 0.80;
        double cf2 = -0.40;
        double combined = CertaintyFactorCalculator.combineCertainties(cf1, cf2);

        assertEquals(0.6666666, combined, 0.0001);
    }
}
