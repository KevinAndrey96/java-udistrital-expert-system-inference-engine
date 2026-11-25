package edu.udistrital.expert.cli;

import edu.udistrital.expert.engine.ConflictResolutionStrategy;
import edu.udistrital.expert.engine.InferenceEngine;
import edu.udistrital.expert.engine.InferenceResult;
import edu.udistrital.expert.model.Fact;
import edu.udistrital.expert.model.KnowledgeBase;
import edu.udistrital.expert.model.Premise;
import edu.udistrital.expert.model.Rule;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Headless Command Line Interface for Universidad Distrital Inference Engine.
 * Supports batch inference, interactive chaining, saturation, and benchmark execution.
 */
public class ExpertSystemCli {

    private static final String BANNER =
            "================================================================================\n" +
            "  UNIVERSIDAD DISTRITAL FRANCISCO JOSE DE CALDAS\n" +
            "  Facultad de Ingenieria - Inteligencia Artificial\n" +
            "  Motor de Inferencia y Sistema Experto (Forward / Backward / Chaining / CF)\n" +
            "================================================================================";

    public static void main(String[] args) {
        System.out.println(BANNER);

        String kbPath = null;
        String mode = "forward";
        String goal = null;
        String strategyName = "premises";
        String initialFactsStr = null;
        boolean runDemo = false;
        boolean launchGui = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--kb":
                case "-k":
                    if (i + 1 < args.length) kbPath = args[++i];
                    break;
                case "--mode":
                case "-m":
                    if (i + 1 < args.length) mode = args[++i].toLowerCase();
                    break;
                case "--goal":
                case "-g":
                    if (i + 1 < args.length) goal = args[++i];
                    break;
                case "--strategy":
                case "-s":
                    if (i + 1 < args.length) strategyName = args[++i].toLowerCase();
                    break;
                case "--facts":
                case "-f":
                    if (i + 1 < args.length) initialFactsStr = args[++i];
                    break;
                case "--demo":
                case "-d":
                    runDemo = true;
                    break;
                case "--gui":
                    launchGui = true;
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    return;
            }
        }

        if (launchGui) {
            launchSwingGui();
            return;
        }

        if (runDemo) {
            executeDemo();
            return;
        }

        ConflictResolutionStrategy strategy = parseStrategy(strategyName);
        KnowledgeBase kb = loadKnowledgeBase(kbPath);

        if (initialFactsStr != null) {
            seedFacts(kb, initialFactsStr);
        }

        System.out.println("\n[Configuracion de Inferencia]");
        System.out.println("  Base de Conocimiento : " + kb.getName());
        System.out.println("  Reglas cargadas      : " + kb.getRuleBase().size());
        System.out.println("  Hechos iniciales     : " + kb.getFactBase().size());
        System.out.println("  Modo de Inferencia   : " + mode.toUpperCase());
        System.out.println("  Estrategia Conflicto : " + strategy.getDescription());
        if (goal != null) {
            System.out.println("  Meta objetivo        : " + goal);
        }

        InferenceEngine engine = new InferenceEngine(kb);
        InferenceResult result;

        switch (mode) {
            case "backward":
            case "atras":
                if (goal == null) {
                    System.err.println("[ERROR] El modo backward requiere especificar una meta (--goal).");
                    System.exit(1);
                }
                result = engine.inferBackward(goal);
                break;
            case "saturate":
            case "saturacion":
                result = engine.saturate(strategy);
                break;
            case "hybrid":
            case "hibrido":
                if (goal == null) {
                    System.err.println("[ERROR] El modo hybrid requiere especificar una meta (--goal).");
                    System.exit(1);
                }
                result = engine.inferHybrid(goal);
                break;
            case "forward":
            case "adelante":
            default:
                result = engine.inferForward(goal, strategy);
                break;
        }

        printResult(result);
    }

    private static ConflictResolutionStrategy parseStrategy(String name) {
        switch (name.toLowerCase()) {
            case "first":
            case "primera":
                return ConflictResolutionStrategy.FIRST_RULE;
            case "certainty":
            case "certeza":
                return ConflictResolutionStrategy.HIGHEST_CERTAINTY;
            case "priority":
            case "prioridad":
                return ConflictResolutionStrategy.HIGHEST_PRIORITY;
            case "premises":
            case "especificidad":
            default:
                return ConflictResolutionStrategy.MOST_PREMISES;
        }
    }

    private static KnowledgeBase loadKnowledgeBase(String path) {
        KnowledgeBase kb = new KnowledgeBase();
        try {
            if (path != null && new File(path).exists()) {
                System.out.println("Cargando base de conocimiento desde archivo: " + path);
                try (InputStream is = new FileInputStream(path)) {
                    kb.loadFromStream(is);
                }
            } else {
                // Fallback to internal weather domain
                System.out.println("Cargando base de conocimiento interna por defecto (Prediccion de Clima UD)...");
                InputStream is = ExpertSystemCli.class.getResourceAsStream("/kb/clima-prediccion.kb");
                if (is != null) {
                    kb.loadFromStream(is);
                } else {
                    populateDefaultUdWeatherKb(kb);
                }
            }
        } catch (Exception e) {
            System.err.println("[ADVERTENCIA] Error leyendo KB (" + e.getMessage() + "). Usando base interna.");
            populateDefaultUdWeatherKb(kb);
        }
        return kb;
    }

    private static void populateDefaultUdWeatherKb(KnowledgeBase kb) {
        kb.setName("Prediccion de Clima - Universidad Distrital");
        kb.setDescription("Sistema experto clasico para prediccion del clima con factores de certeza.");

        // Facts
        kb.addFact("SolHoy", "1", 1.0);
        kb.addFact("SolAyer", "1", 1.0);

        // Rules
        Rule r1 = new Rule();
        r1.setRuleNumber(1);
        r1.setName("R1_SoleadoConsecutivo");
        r1.addPremise(new Premise("SolHoy", Premise.Operator.EQUALS, "1", "Hizo sol hoy?"));
        r1.addPremise(new Premise("SolAyer", Premise.Operator.EQUALS, "1", "Hizo sol ayer?"));
        r1.setConsequent(new Fact("SolManana", "1", "Manana hara sol", 0.95));
        r1.setRuleCertainty(0.95);
        r1.setPriority(10);
        kb.addRule(r1);

        Rule r2 = new Rule();
        r2.setRuleNumber(2);
        r2.setName("R2_PresionAlta");
        r2.addPremise(new Premise("SolManana", Premise.Operator.EQUALS, "1", "Manana hara sol?"));
        r2.setConsequent(new Fact("TardeAgradable", "1", "Tarde calida y agradable", 0.90));
        r2.setRuleCertainty(0.90);
        r2.setPriority(5);
        kb.addRule(r2);
    }

    private static void seedFacts(KnowledgeBase kb, String factsStr) {
        String[] pairs = factsStr.split(",");
        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] kv = pair.split("=", 2);
                double cf = 1.0;
                String val = kv[1].trim();
                if (val.contains("[CF=")) {
                    int idx = val.indexOf("[CF=");
                    int end = val.indexOf("]", idx);
                    try {
                        cf = Double.parseDouble(val.substring(idx + 4, end));
                    } catch (Exception ignored) {}
                    val = val.substring(0, idx).trim();
                }
                kb.addFact(kv[0].trim(), val, cf);
            } else {
                kb.addFact(pair.trim(), "true", 1.0);
            }
        }
    }

    private static void printResult(InferenceResult result) {
        System.out.println("\n=========================== RESULTADO DE INFERENCIA ===========================");
        System.out.println("  Meta Evaluada       : " + result.getTargetGoal());
        System.out.println("  Estado              : " + (result.isSuccess() ? "EXITOSA (DEMOSTRADA)" : "NO DEMOSTRADA"));
        if (result.getResultingFact() != null) {
            Fact rf = result.getResultingFact();
            System.out.printf("  Hecho Concluyente   : %s = %s\n", rf.getName(), rf.getValue());
            System.out.printf("  Factor de Certeza   : %.4f (%.2f%%)\n", rf.getCertaintyFactor(), rf.getCertaintyFactor() * 100);
        }
        System.out.println("  Reglas Disparadas   : " + result.getFiredRules().size());
        System.out.println("  Nuevos Hechos       : " + result.getNewlyInferredFacts().size());
        System.out.println("  Tiempo de Computo   : " + result.getExecutionTimeMs() + " ms");

        System.out.println("\n------------------------- TRAZA PASO A PASO (EXPLICACION) -------------------------");
        System.out.println(result.getTraceLog());
        System.out.println("================================================================================");
    }

    private static void executeDemo() {
        System.out.println("\n[EJECUTANDO DEMOSTRACION COMPLETA MULTI-DOMINIO]");

        // 1. Clima
        KnowledgeBase weatherKb = new KnowledgeBase();
        populateDefaultUdWeatherKb(weatherKb);
        InferenceEngine engine1 = new InferenceEngine(weatherKb);
        System.out.println("\n>>> Caso 1: Prediccion de Clima UD (Forward Chaining)");
        InferenceResult res1 = engine1.inferForward("SolManana", ConflictResolutionStrategy.MOST_PREMISES);
        printResult(res1);

        // 2. Diagnostico Medico (Backward Chaining)
        KnowledgeBase medKb = new KnowledgeBase("Diagnostico Medico UD", "Sistema de diagnostico con factores de certeza.");
        medKb.addFact("Fiebre", "alta", 0.90);
        medKb.addFact("Tos", "seca", 0.85);

        Rule rFlu = new Rule(1, "R_Gripe", null, new Fact("Patologia", "Gripe", "Diagnostico Gripe", 0.85), 0.85, 5);
        rFlu.addPremise(new Premise("Fiebre", Premise.Operator.EQUALS, "alta", "Tiene fiebre alta?"));
        rFlu.addPremise(new Premise("Tos", Premise.Operator.EQUALS, "seca", "Tiene tos seca?"));
        medKb.addRule(rFlu);

        Rule rRest = new Rule(2, "R_Tratamiento", null, new Fact("Tratamiento", "Reposo_Hidratacion", "Tratamiento ambulatorio", 0.95), 0.95, 1);
        rRest.addPremise(new Premise("Patologia", Premise.Operator.EQUALS, "Gripe", "Tiene gripe?"));
        medKb.addRule(rRest);

        InferenceEngine engine2 = new InferenceEngine(medKb);
        System.out.println("\n>>> Caso 2: Diagnostico Medico UD (Backward Chaining - Meta: Tratamiento)");
        InferenceResult res2 = engine2.inferBackward("Tratamiento");
        printResult(res2);
    }

    private static void launchSwingGui() {
        try {
            System.out.println("Iniciando Interfaz Grafica Universidad Distrital (Swing)...");
            java.awt.EventQueue.invokeLater(() -> {
                try {
                    Diseño.Inicio gui = new Diseño.Inicio();
                    gui.setVisible(true);
                } catch (Throwable t) {
                    System.err.println("No se pudo iniciar la interfaz grafica: " + t.getMessage());
                }
            });
        } catch (Throwable t) {
            System.err.println("Entorno grafico no disponible (Headless). Use el modo CLI.");
        }
    }

    private static void printHelp() {
        System.out.println("\nUso: java -jar expert-system.jar [OPCIONES]");
        System.out.println("Opciones:");
        System.out.println("  --kb, -k <archivo>         Ruta al archivo de base de conocimiento (.kb)");
        System.out.println("  --mode, -m <modo>          Modo de inferencia: forward (defecto), backward, saturate, hybrid");
        System.out.println("  --goal, -g <meta>          Nombre del atributo/meta a demostrar");
        System.out.println("  --strategy, -s <estrategia> Estrategia de resolucion: premises (defecto), first, certainty, priority");
        System.out.println("  --facts, -f <lista>        Hechos iniciales: \"attr1=val1[CF=0.9],attr2=val2\"");
        System.out.println("  --demo, -d                 Ejecuta suite de demostracion predefinida");
        System.out.println("  --gui                      Lanza la interfaz grafica Swing");
        System.out.println("  --help, -h                 Muestra este mensaje de ayuda");
    }
}
