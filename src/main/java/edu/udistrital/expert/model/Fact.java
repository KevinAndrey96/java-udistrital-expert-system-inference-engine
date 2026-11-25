package edu.udistrital.expert.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Representation of a proposition or fact in the Knowledge Base (Base de Hechos).
 * Universidad Distrital Francisco Jose de Caldas - Expert Systems Engine.
 */
public class Fact implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private String description;
    private double certaintyFactor; // CF in [-1.0, 1.0]

    public Fact() {
        this("", "true", "", 1.0);
    }

    public Fact(String name) {
        this(name, "true", name, 1.0);
    }

    public Fact(String name, String value) {
        this(name, value, name + "=" + value, 1.0);
    }

    public Fact(String name, String value, double certaintyFactor) {
        this(name, value, name + "=" + value, certaintyFactor);
    }

    public Fact(String name, String value, String description, double certaintyFactor) {
        this.name = name != null ? name.trim() : "";
        this.value = value != null ? value.trim() : "true";
        this.description = description != null ? description.trim() : "";
        this.certaintyFactor = Math.max(-1.0, Math.min(1.0, certaintyFactor));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value.trim() : "true";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public double getCertaintyFactor() {
        return certaintyFactor;
    }

    public void setCertaintyFactor(double certaintyFactor) {
        this.certaintyFactor = Math.max(-1.0, Math.min(1.0, certaintyFactor));
    }

    public boolean matches(String factName, String factValue) {
        if (this.name.equalsIgnoreCase(factName)) {
            return this.value.equalsIgnoreCase(factValue);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fact fact = (Fact) o;
        return name.equalsIgnoreCase(fact.name) && value.equalsIgnoreCase(fact.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), value.toLowerCase());
    }

    @Override
    public String toString() {
        if ("true".equalsIgnoreCase(value)) {
            return name + " [CF=" + String.format("%.2f", certaintyFactor) + "]";
        }
        return name + "=" + value + " [CF=" + String.format("%.2f", certaintyFactor) + "]";
    }
}
