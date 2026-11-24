package dev.felipe.canchas.core.model;

public enum StaffRole {
    ADMIN("Administrador"),
    RECEPTION("Recepción");

    private final String label;

    StaffRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
