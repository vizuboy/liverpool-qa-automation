package com.liverpool.qa.model;

public enum Category {
    BOTAS("Botas de hombre", "botas hombre", "Zapatos de hombre", "Botas"),
    MOCASINES("Mocasines de hombre", "mocasines hombre", "Zapatos de hombre", "Mocasines"),
    PERFUMES("Perfumes", "perfumes", "Perfumes"),
    COMPUTADORA_LENOVO("Computadora Lenovo", "computadora lenovo"),
    PANTALLA_SONY("Pantalla Sony", "pantalla sony"),
    XBOX("Xbox", "consola xbox series");

    private final String label;
    private final String searchTerm;
    private final String[] menuPath;

    Category(String label, String searchTerm, String... menuPath) {
        this.label = label;
        this.searchTerm = searchTerm;
        this.menuPath = menuPath;
    }

    public String searchTerm() { return searchTerm; }
    public String[] menuPath() { return menuPath; }

    @Override
    public String toString() { return label; }
}
