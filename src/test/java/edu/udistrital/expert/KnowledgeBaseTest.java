package edu.udistrital.expert;

import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.KnowledgeBase;
import edu.udistrital.expert.model.Premise;
import edu.udistrital.expert.model.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for KnowledgeBase parsing, rule indexing, and numeric premises.
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class KnowledgeBaseTest {

    @Test
    public void testParseKnowledgeBaseFromStream() throws IOException {
        String kbContent =
                "# Test Knowledge Base\n" +
                "FACT SolHoy=1 [CF=1.00]\n" +
                "FACT SolAyer=1 [CF=0.90]\n" +
                "RULE R1: IF SolHoy=1 AND SolAyer=1 THEN SolManana=1 [CF=0.95] [PRIORITY=10]\n";

        KnowledgeBase kb = new KnowledgeBase();
        try (InputStream is = new ByteArrayInputStream(kbContent.getBytes(StandardCharsets.UTF_8))) {
            kb.loadFromStream(is);
        }

        assertEquals("Should parse 2 facts", 2, kb.getFactBase().size());
        assertTrue("Should have fact SolHoy", kb.hasFact("SolHoy", "1"));
        assertEquals(0.90, kb.getFact("SolAyer").getCertaintyFactor(), 0.001);

        assertEquals("Should parse 1 rule", 1, kb.getRuleBase().size());
        Rule r = kb.getRuleBase().get(0);
        assertEquals("R1", r.getName());
        assertEquals(2, r.getPremises().size());
        assertEquals(10, r.getPriority());
        assertEquals(0.95, r.getRuleCertainty(), 0.001);
        assertEquals("SolManana", r.getConsequent().getName());
        assertEquals("1", r.getConsequent().getValue());
    }

    @Test
    public void testNumericPremisesOperators() {
        Premise pGreater = new Premise("Temperatura", Premise.Operator.GREATER_THAN, "37.5", "Fiebre?");
        Premise pLess = new Premise("Presion", Premise.Operator.LESS_THAN, "90", "Hipotension?");
        Premise pGeq = new Premise("Edad", Premise.Operator.GREATER_OR_EQUAL, "18", "Mayor de edad?");

        assertTrue("38.2 > 37.5", pGreater.evaluate(new Fact("Temperatura", "38.2")));
        assertFalse("36.5 > 37.5", pGreater.evaluate(new Fact("Temperatura", "36.5")));

        assertTrue("85 < 90", pLess.evaluate(new Fact("Presion", "85")));
        assertFalse("95 < 90", pLess.evaluate(new Fact("Presion", "95")));

        assertTrue("18 >= 18", pGeq.evaluate(new Fact("Edad", "18")));
        assertTrue("25 >= 18", pGeq.evaluate(new Fact("Edad", "25")));
        assertFalse("17 >= 18", pGeq.evaluate(new Fact("Edad", "17")));
    }

    @Test
    public void testFindRulesConcludingFact() {
        KnowledgeBase kb = new KnowledgeBase();

        Rule r1 = new Rule(1, "R1", null, new Fact("Target", "A"), 1.0, 1);
        Rule r2 = new Rule(2, "R2", null, new Fact("Target", "B"), 1.0, 1);
        Rule r3 = new Rule(3, "R3", null, new Fact("Other", "X"), 1.0, 1);

        kb.addRule(r1);
        kb.addRule(r2);
        kb.addRule(r3);

        List<Rule> concluding = kb.findRulesConcluding("Target");
        assertEquals("Should find exactly 2 rules concluding Target", 2, concluding.size());
    }
}
