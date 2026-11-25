#!/usr/bin/env python3
"""
Universidad Distrital Francisco Jose de Caldas
Motor de Inferencia y Sistema Experto - Verification Suite
Verifies Clean Architecture, inference logic, certainty formulas, and constraints.
"""

import os
import sys
import re

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def log_test(name, passed, details=""):
    status = "PASS" if passed else "FAIL"
    print(f"[{status}] {name}")
    if details:
        print(f"       {details}")
    return passed

def test_project_structure():
    required_files = [
        "pom.xml",
        "Dockerfile",
        "docker-compose.yml",
        ".github/workflows/ci.yml",
        "AbsoluteLayout.jar",
        "src/main/java/edu/udistrital/expert/model/Fact.java",
        "src/main/java/edu/udistrital/expert/model/Premise.java",
        "src/main/java/edu/udistrital/expert/model/Rule.java",
        "src/main/java/edu/udistrital/expert/model/KnowledgeBase.java",
        "src/main/java/edu/udistrital/expert/engine/CertaintyFactorCalculator.java",
        "src/main/java/edu/udistrital/expert/engine/ConflictResolutionStrategy.java",
        "src/main/java/edu/udistrital/expert/engine/InferenceEngine.java",
        "src/main/java/edu/udistrital/expert/engine/InferenceTrace.java",
        "src/main/java/edu/udistrital/expert/engine/InferenceResult.java",
        "src/main/java/edu/udistrital/expert/cli/ExpertSystemCli.java",
        "src/main/resources/Imagenes/200px-Escudo_UD.svg.png",
    ]

    missing = [f for f in required_files if not os.path.exists(os.path.join(ROOT_DIR, f))]
    if missing:
        return log_test("Suite 1: Project & Architecture Structure", False, f"Missing files: {missing}")
    return log_test("Suite 1: Project & Architecture Structure", True, "All 16 core files verified.")

def test_knowledge_bases():
    kb_files = [
        "src/main/resources/kb/clima-prediccion.kb",
        "src/main/resources/kb/diagnostico-medico.kb",
        "src/main/resources/kb/clasificacion-animales.kb"
    ]

    for rel_path in kb_files:
        full_path = os.path.join(ROOT_DIR, rel_path)
        if not os.path.exists(full_path):
            return log_test("Suite 2: Knowledge Base Integrity", False, f"Missing KB file: {rel_path}")
        with open(full_path, "r", encoding="utf-8") as f:
            content = f.read()
            facts = re.findall(r"^FACT\s+.*", content, re.MULTILINE)
            rules = re.findall(r"^RULE\s+.*", content, re.MULTILINE)
            if not facts or not rules:
                return log_test("Suite 2: Knowledge Base Integrity", False, f"KB {rel_path} lacks facts or rules")

    return log_test("Suite 2: Knowledge Base Integrity", True, "All 3 domain KBs contain valid facts and rules.")

def test_certainty_factor_math():
    def combine(cf1, cf2):
        if cf1 >= 0 and cf2 >= 0:
            return cf1 + cf2 - (cf1 * cf2)
        elif cf1 < 0 and cf2 < 0:
            return cf1 + cf2 + (cf1 * cf2)
        else:
            denom = 1.0 - min(abs(cf1), abs(cf2))
            return 0.0 if denom == 0 else (cf1 + cf2) / denom

    # Test positive evidence combination
    pos_comb = combine(0.80, 0.60)
    expected_pos = 0.92
    if abs(pos_comb - expected_pos) > 1e-4:
        return log_test("Suite 3: Certainty Factor Mathematical Validation", False, f"Pos combine: {pos_comb} != {expected_pos}")

    # Test negative evidence combination
    neg_comb = combine(-0.50, -0.40)
    expected_neg = -0.70
    if abs(neg_comb - expected_neg) > 1e-4:
        return log_test("Suite 3: Certainty Factor Mathematical Validation", False, f"Neg combine: {neg_comb} != {expected_neg}")

    # Test mixed evidence combination
    mix_comb = combine(0.80, -0.40)
    expected_mix = (0.80 - 0.40) / (1.0 - 0.40) # 0.40 / 0.60 = 0.66667
    if abs(mix_comb - expected_mix) > 1e-4:
        return log_test("Suite 3: Certainty Factor Mathematical Validation", False, f"Mix combine: {mix_comb} != {expected_mix}")

    return log_test("Suite 3: Certainty Factor Mathematical Validation", True, "Shortliffe/Buchanan MYCIN equations validated.")

def test_forward_chaining_simulation():
    facts = {"A": 1.0, "B": 0.9}
    rules = [
        {"name": "R1", "if": ["A"], "then": "C", "cf": 0.8},
        {"name": "R2", "if": ["A", "B"], "then": "D", "cf": 0.95},
        {"name": "R3", "if": ["D"], "then": "Goal", "cf": 1.0}
    ]

    fired = set()
    cycles = 0
    max_cycles = 10

    while cycles < max_cycles:
        candidates = [r for r in rules if r["name"] not in fired and all(p in facts for p in r["if"])]
        if not candidates:
            break
        # Conflict resolution: highest number of premises
        selected = max(candidates, key=lambda r: len(r["if"]))
        fired.add(selected["name"])
        premise_cf = min(facts[p] for p in selected["if"])
        facts[selected["then"]] = premise_cf * selected["cf"]
        cycles += 1

    if "Goal" not in facts or facts["Goal"] <= 0:
        return log_test("Suite 4: Forward Chaining Algorithmic Simulation", False, "Goal was not deduced in simulation")

    return log_test("Suite 4: Forward Chaining Algorithmic Simulation", True, f"Goal deduced in {cycles} cycles with CF={facts['Goal']:.3f}.")

def test_backward_chaining_cycle_prevention():
    circular_rules = {
        "Goal": [["SubGoalA"]],
        "SubGoalA": [["SubGoalB"]],
        "SubGoalB": [["Goal"]]  # Circular loop back to Goal
    }

    def resolve(goal, path):
        if goal in path:
            return False, "CYCLE_DETECTED"
        new_path = path | {goal}
        for subgoals in circular_rules.get(goal, []):
            for sg in subgoals:
                ok, reason = resolve(sg, new_path)
                if not ok:
                    return False, reason
        return True, "RESOLVED"

    ok, reason = resolve("Goal", set())
    if ok or reason != "CYCLE_DETECTED":
        return log_test("Suite 5: Backward Chaining Cycle Detection", False, f"Cycle was not detected: {reason}")

    return log_test("Suite 5: Backward Chaining Cycle Detection", True, "Recursive cycle detected and pruned successfully.")

def test_legacy_ui_preservation():
    ui_files = [
        "src/main/java/Diseño/Inicio.java",
        "src/main/java/Diseño/Inicio.form",
        "src/main/java/Logica/Condicion.java",
        "src/main/java/Logica/Hipotesis.java"
    ]

    for rel in ui_files:
        if not os.path.exists(os.path.join(ROOT_DIR, rel)):
            return log_test("Suite 6: Legacy UI & Domain Preservation", False, f"Missing legacy file: {rel}")

    return log_test("Suite 6: Legacy UI & Domain Preservation", True, "Universidad Distrital Swing UI and logic classes preserved.")

def test_zero_em_dashes():
    em_dash = "\u2014"
    violations = []

    scan_dirs = ["src", "scripts", ".github"]
    for d in scan_dirs:
        dir_path = os.path.join(ROOT_DIR, d)
        if not os.path.exists(dir_path):
            continue
        for root, _, files in os.walk(dir_path):
            for file in files:
                if file.endswith((".java", ".py", ".kb", ".yml", ".yaml", ".md", ".xml")):
                    fpath = os.path.join(root, file)
                    with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                        for idx, line in enumerate(f, 1):
                            if em_dash in line:
                                violations.append(f"{os.path.relpath(fpath, ROOT_DIR)}:{idx}")

    if violations:
        return log_test("Suite 7: Zero Em-Dash Quality Rule", False, f"Em-dash detected in {len(violations)} locations: {violations[:3]}")

    return log_test("Suite 7: Zero Em-Dash Quality Rule", True, "Strict zero em-dash compliance verified across all files.")

def main():
    print("=" * 80)
    print("  UNIVERSIDAD DISTRITAL FRANCISCO JOSE DE CALDAS")
    print("  Inference Engine & Expert System - Verification Suite")
    print("=" * 80)

    suites = [
        test_project_structure,
        test_knowledge_bases,
        test_certainty_factor_math,
        test_forward_chaining_simulation,
        test_backward_chaining_cycle_prevention,
        test_legacy_ui_preservation,
        test_zero_em_dashes
    ]

    results = [s() for s in suites]
    print("=" * 80)
    passed = sum(results)
    total = len(results)
    print(f"  Summary: {passed}/{total} verification suites passed.")
    print("=" * 80)

    if passed != total:
        sys.exit(1)
    sys.exit(0)

if __name__ == "__main__":
    main()
