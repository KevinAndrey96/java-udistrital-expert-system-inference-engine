package edu.udistrital.expert;

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
 * Unit tests for Backward Chaining (Encadenamiento hacia Atras) and Cycle Prevention.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class BackwardChainingTest {

    private KnowledgeBase kb;

    @Before
    public void setUp() {
        kb = new KnowledgeBase("Backward KB", "Knowledge Base for goal-driven backward resolution");

        // Facts: Pelo=si, RayasNegras=si
        kb.addFact("Pelo", "si", 1.0);
        kb.addFact("RayasNegras", "si", 0.95);

        // R1: IF Pelo=si THEN Clase=Mamifero [CF=1.0]
        Rule r1 = new Rule(1, "R1_Mamifero", null, new Fact("Clase", "Mamifero", "", 1.0), 1.0, 5);
        r1.addPremise(new Premise("Pelo", Premise.Operator.EQUALS, "si", "Tiene pelo?"));
        kb.addRule(r1);

        // R2: IF ComeCarne=si THEN Orden=Carnivoro [CF=0.90]
        Rule r2 = new Rule(2, "R2_Carnivoro", null, new Fact("Orden", "Carnivoro", "", 0.90), 0.90, 5);
        r2.addPremise(new Premise("ComeCarne", Premise.Operator.EQUALS, "si", "Come carne?"));
        kb.addRule(r2);

        // R3: IF Clase=Mamifero AND Orden=Carnivoro AND RayasNegras=si THEN Especie=Tigre [CF=0.98]
        Rule r3 = new Rule(3, "R3_Tigre", null, new Fact("Especie", "Tigre", "", 0.98), 0.98, 10);
        r3.addPremise(new Premise("Clase", Premise.Operator.EQUALS, "Mamifero", "Es mamifero?"));
        r3.addPremise(new Premise("Orden", Premise.Operator.EQUALS, "Carnivoro", "Es carnivoro?"));
        r3.addPremise(new Premise("RayasNegras", Premise.Operator.EQUALS, "si", "Tiene rayas negras?"));
        kb.addRule(r3);
    }

    @Test
    public void testBackwardChainingWithInteractiveFactProvider() {
        // ComeCarne is missing from base facts. FactProvider provides it.
        InferenceEngine engine = new InferenceEngine(kb, (premise) -> {
            if ("ComeCarne".equalsIgnoreCase(premise.getAttribute())) {
                return new Fact("ComeCarne", "si", "Provisto por usuario", 1.0);
            }
            return null;
        });

        InferenceResult result = engine.inferBackward("Especie");

        assertTrue("Backward chaining should resolve Especie=Tigre", result.isSuccess());
        assertNotNull(result.getResultingFact());
        assertEquals("Tigre", result.getResultingFact().getValue());
        assertTrue("Certainty factor should be greater than 0.8",
                result.getResultingFact().getCertaintyFactor() > 0.8);
    }

    @Test
    public void testBackwardChainingFailsWhenSubgoalUnprovable() {
        // Without fact provider, ComeCarne cannot be proven
        InferenceEngine engine = new InferenceEngine(kb);
        InferenceResult result = engine.inferBackward("Especie");

        assertFalse("Goal should fail because ComeCarne is neither in facts nor inferred",
                result.isSuccess());
    }

    @Test
    public void testCycleDetectionPreventsInfiniteRecursion() {
        KnowledgeBase cyclicKb = new KnowledgeBase("Cyclic KB", "Circular dependency test");

        // Rule Circular 1: IF Alpha=true THEN Beta=true
        Rule c1 = new Rule(1, "C1", null, new Fact("Beta", "true"), 1.0, 1);
        c1.addPremise(new Premise("Alpha", Premise.Operator.EQUALS, "true", "Alpha?"));
        cyclicKb.addRule(c1);

        // Rule Circular 2: IF Beta=true THEN Alpha=true
        Rule c2 = new Rule(2, "C2", null, new Fact("Alpha", "true"), 1.0, 1);
        c2.addPremise(new Premise("Beta", Premise.Operator.EQUALS, "true", "Beta?"));
        cyclicKb.addRule(c2);

        InferenceEngine engine = new InferenceEngine(cyclicKb);
        InferenceResult result = engine.inferBackward("Alpha");

        assertFalse("Cycle should be detected and pruned, returning false gracefully without StackOverflowError",
                result.isSuccess());
        assertTrue("Trace should mention cycle detection",
                result.getTraceLog().contains("CICLO DETECTADO"));
    }
}
