package edu.udistrital.expert.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Knowledge Base (Base de Conocimiento) aggregating the Fact Base (BH) and Rule Base (BR).
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private final List<Fact> factBase;
    private final List<Rule> ruleBase;

    public KnowledgeBase() {
        this("Sistema Experto UD", "Base de conocimiento para inferencia logica.");
    }

    public KnowledgeBase(String name, String description) {
        this.name = name != null ? name : "Base de Conocimiento";
        this.description = description != null ? description : "";
        this.factBase = new ArrayList<>();
        this.ruleBase = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Fact Base (BH) operations
    public List<Fact> getFactBase() {
        return Collections.unmodifiableList(factBase);
    }

    public void addFact(Fact fact) {
        if (fact == null) return;
        for (int i = 0; i < factBase.size(); i++) {
            if (factBase.get(i).getName().equalsIgnoreCase(fact.getName())) {
                // Update existing fact
                factBase.set(i, fact);
                return;
            }
        }
        factBase.add(fact);
    }

    public void addFact(String name, String value) {
        addFact(new Fact(name, value));
    }

    public void addFact(String name, String value, double certainty) {
        addFact(new Fact(name, value, name + "=" + value, certainty));
    }

    public boolean hasFact(String factName) {
        if (factName == null) return false;
        for (Fact f : factBase) {
            if (f.getName().equalsIgnoreCase(factName.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFact(String factName, String factValue) {
        if (factName == null) return false;
        for (Fact f : factBase) {
            if (f.matches(factName, factValue)) {
                return true;
            }
        }
        return false;
    }

    public Fact getFact(String factName) {
        if (factName == null) return null;
        for (Fact f : factBase) {
            if (f.getName().equalsIgnoreCase(factName.trim())) {
                return f;
            }
        }
        return null;
    }

    public void clearFacts() {
        this.factBase.clear();
    }

    // Rule Base (BR) operations
    public List<Rule> getRuleBase() {
        return Collections.unmodifiableList(ruleBase);
    }

    public void addRule(Rule rule) {
        if (rule == null) return;
        if (!ruleBase.contains(rule)) {
            ruleBase.add(rule);
        }
    }

    public void clearRules() {
        this.ruleBase.clear();
    }

    public List<Rule> findRulesConcluding(String factName) {
        List<Rule> result = new ArrayList<>();
        if (factName == null) return result;
        for (Rule r : ruleBase) {
            if (r.concludes(factName)) {
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Parses a Knowledge Base definition from an InputStream.
     * Supported format:
     * # Comments
     * FACT name=value [CF=0.95]
     * RULE R1: IF attr1=val1 AND attr2=val2 THEN concl=val [CF=0.90] [PRIORITY=10]
     */
    public void loadFromStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int autoRuleNum = 1;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }

                if (line.toUpperCase().startsWith("FACT ")) {
                    parseFactLine(line.substring(5).trim());
                } else if (line.toUpperCase().startsWith("RULE ")) {
                    parseRuleLine(line.substring(5).trim(), autoRuleNum++);
                }
            }
        }
    }

    private void parseFactLine(String line) {
        // e.g. "SolHoy=1 [CF=1.0]" or "Fiebre=true"
        double cf = 1.0;
        if (line.contains("[CF=") || line.contains("[cf=")) {
            int idx = line.toUpperCase().indexOf("[CF=");
            int endIdx = line.indexOf("]", idx);
            if (endIdx > idx) {
                try {
                    cf = Double.parseDouble(line.substring(idx + 4, endIdx).trim());
                } catch (NumberFormatException ignored) {}
                line = line.substring(0, idx).trim();
            }
        }

        String[] parts = line.split("=", 2);
        if (parts.length == 2) {
            addFact(parts[0].trim(), parts[1].trim(), cf);
        } else {
            addFact(line.trim(), "true", cf);
        }
    }

    private void parseRuleLine(String line, int defaultNum) {
        // e.g. "R1: IF SolHoy=1 AND SolAyer=1 THEN SolManana=1 [CF=0.95] [PRIORITY=5]"
        String ruleName = "R" + defaultNum;
        int ruleNum = defaultNum;
        if (line.contains(":")) {
            String[] nameParts = line.split(":", 2);
            ruleName = nameParts[0].trim();
            line = nameParts[1].trim();
        }

        double cf = 1.0;
        int priority = 0;

        if (line.toUpperCase().contains("[CF=")) {
            int idx = line.toUpperCase().indexOf("[CF=");
            int endIdx = line.indexOf("]", idx);
            if (endIdx > idx) {
                try {
                    cf = Double.parseDouble(line.substring(idx + 4, endIdx).trim());
                } catch (NumberFormatException ignored) {}
                line = line.substring(0, idx) + line.substring(endIdx + 1);
            }
        }

        if (line.toUpperCase().contains("[PRIORITY=")) {
            int idx = line.toUpperCase().indexOf("[PRIORITY=");
            int endIdx = line.indexOf("]", idx);
            if (endIdx > idx) {
                try {
                    priority = Integer.parseInt(line.substring(idx + 10, endIdx).trim());
                } catch (NumberFormatException ignored) {}
                line = line.substring(0, idx) + line.substring(endIdx + 1);
            }
        }

        line = line.trim();
        if (!line.toUpperCase().startsWith("IF ")) {
            return;
        }

        int thenIdx = line.toUpperCase().indexOf(" THEN ");
        if (thenIdx == -1) return;

        String ifPart = line.substring(3, thenIdx).trim();
        String thenPart = line.substring(thenIdx + 6).trim();

        // Parse premises
        List<Premise> premises = new ArrayList<>();
        String[] premiseTokens = ifPart.split("(?i)\\s+AND\\s+");
        for (String token : premiseTokens) {
            token = token.trim();
            if (token.isEmpty()) continue;
            Premise.Operator op = Premise.Operator.EQUALS;
            String attr = token;
            String val = "true";

            for (Premise.Operator possibleOp : Premise.Operator.values()) {
                if (token.contains(possibleOp.getSymbol())) {
                    String[] opParts = token.split(java.util.regex.Pattern.quote(possibleOp.getSymbol()), 2);
                    attr = opParts[0].trim();
                    val = opParts[1].trim();
                    op = possibleOp;
                    break;
                }
            }
            premises.add(new Premise(attr, op, val, attr + " " + op.getSymbol() + " " + val + "?"));
        }

        // Parse consequent
        String conclAttr = thenPart;
        String conclVal = "true";
        if (thenPart.contains("=")) {
            String[] cParts = thenPart.split("=", 2);
            conclAttr = cParts[0].trim();
            conclVal = cParts[1].trim();
        }

        Fact consequent = new Fact(conclAttr, conclVal, conclAttr + "=" + conclVal, cf);
        Rule rule = new Rule(ruleNum, ruleName, premises, consequent, cf, priority);
        addRule(rule);
    }
}
