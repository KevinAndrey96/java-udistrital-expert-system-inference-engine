package edu.udistrital.expert;

import edu.udistrital.expert.engine.ConflictResolutionStrategy;
import edu.udistrital.expert.engine.InferenceEngine;
import edu.udistrital.expert.engine.InferenceResult;
import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.KnowledgeBase;
import edu.udistrital.expert.model.Premise;
import edu.udistrital.expert.model.Rule;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Forward Chaining (Encadenamiento hacia Adelante) and Saturation.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class ForwardChainingTest {

    private KnowledgeBase kb;

    @Before
    public void setUp() {
        kb = new KnowledgeBase("Test KB", "Test Knowledge Base for Forward Chaining");

        // Facts: A=1, B=1
        kb.addFact("A", "1", 1.0);
        kb.addFact("B", "1", 0.9);

        // Rules:
        // R1: IF A=1 THEN C=1 [CF=0.8] [PRIORITY=1]
        Rule r1 = new Rule(1, "R1", null, new Fact("C", "1", "", 0.8), 0.8, 1);
        r1.addPremise(new Premise("A", Premise.Operator.EQUALS, "1", "A?"));
        kb.addRule(r1);

        // R2: IF A=1 AND B=1 THEN D=1 [CF=0.95] [PRIORITY=10] (2 premises, higher specificity)
        Rule r2 = new Rule(2, "R2", null, new Fact("D", "1", "", 0.95), 0.95, 10);
        r2.addPremise(new Premise("A", Premise.Operator.EQUALS, "1", "A?"));
        r2.addPremise(new Premise("B", Premise.Operator.EQUALS, "1", "B?"));
        kb.addRule(r2);

        // R3: IF D=1 THEN Goal=Success [CF=1.0] [PRIORITY=5]
        Rule r3 = new Rule(3, "R3", null, new Fact("Goal", "Success", "", 1.0), 1.0, 5);
        r3.addPremise(new Premise("D", Premise.Operator.EQUALS, "1", "D?"));
        kb.addRule(r3);
    }

    @Test
    public void testForwardChainingReachesTargetGoal() {
        InferenceEngine engine = new InferenceEngine(kb);
        InferenceResult result = engine.inferForward("Goal", ConflictResolutionStrategy.MOST_PREMISES);

        assertTrue("Goal should be successfully deduced", result.isSuccess());
        assertNotNull("Resulting fact should not be null", result.getResultingFact());
        assertEquals("Success", result.getResultingFact().getValue());
        assertTrue("At least 2 rules should fire to reach Goal (R2 and R3)",
                result.getFiredRules().size() >= 2);
    }

    @Test
    public void testForwardSaturationFiresAllPossibleRules() {
        InferenceEngine engine = new InferenceEngine(kb);
        InferenceResult result = engine.saturate(ConflictResolutionStrategy.FIRST_RULE);

        assertTrue("Saturation should report success", result.isSuccess());
        assertEquals("All 3 rules should have fired in saturation", 3, result.getFiredRules().size());
        assertEquals("3 new facts should be inferred (C, D, Goal)", 3, result.getNewlyInferredFacts().size());
    }

    @Test
    public void testConflictResolutionStrategyPrioritizesMostPremises() {
        // R1 has 1 premise, R2 has 2 premises
        Rule selected = ConflictResolutionStrategy.MOST_PREMISES.select(kb.getRuleBase());
        assertNotNull(selected);
        assertEquals("R2 should be selected first due to higher specificity", "R2", selected.getName());
    }

    @Test
    public void testConflictResolutionStrategyPrioritizesHighestCertainty() {
        Rule selected = ConflictResolutionStrategy.HIGHEST_CERTAINTY.select(kb.getRuleBase());
        assertNotNull(selected);
        assertEquals("R3 has highest certainty 1.0", "R3", selected.getName());
    }

    @Test
    public void testConflictResolutionStrategyPrioritizesHighestPriority() {
        Rule selected = ConflictResolutionStrategy.HIGHEST_PRIORITY.select(kb.getRuleBase());
        assertNotNull(selected);
        assertEquals("R2 has highest priority 10", "R2", selected.getName());
    }

    @Test
    public void testUnreachableGoalReturnsFailureGracefully() {
        InferenceEngine engine = new InferenceEngine(kb);
        InferenceResult result = engine.inferForward("InexistentGoal", ConflictResolutionStrategy.FIRST_RULE);

        assertFalse("Inexistent goal should fail", result.isSuccess());
        assertNull(result.getResultingFact());
    }
}
