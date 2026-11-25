package Gestor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Responsable implements Observador {
    private final String id;
    private final String nombre;
    private final String correo;
    private final List<Alerta> bandeja = new ArrayList<>();

    public Responsable(String id, String nombre, String correo) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre del responsable es obligatorio");
        this.id = id;
        this.nombre = nombre.trim();
        this.correo = correo == null ? "" : correo.trim();
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }

    @Override public void alertar(Alerta alerta) { bandeja.add(alerta); }

    public List<Alerta> getBandeja() { return Collections.unmodifiableList(bandeja); }

    @Override public boolean equals(Object o) { return o instanceof Responsable r && Objects.equals(id, r.id); }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return nombre + " (" + id + ")"; }
}
