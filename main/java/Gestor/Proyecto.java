package Gestor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Proyecto {
    private final String id;
    private final String nombre;
    private final List<Tarea> raices = new ArrayList<>();

    public Proyecto(String id, String nombre) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre del proyecto es obligatorio");
        this.id = id;
        this.nombre = nombre.trim();
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public List<Tarea> getRaices() { return Collections.unmodifiableList(raices); }

    public void agregarRaiz(Tarea t) { agregarRaizEn(raices.size(), t); }
    public void agregarRaizEn(int indice, Tarea t) { raices.add(indice, t); }
    public boolean quitarRaiz(Tarea t) { return raices.remove(t); }
    public int indiceDe(Tarea t) { return raices.indexOf(t); }

    public List<Tarea> todasLasTareas() {
        List<Tarea> todas = new ArrayList<>();
        raices.forEach(r -> todas.addAll(r.aplanar()));
        return todas;
    }

    public Optional<Tarea> buscar(String idTarea) {
        if (idTarea == null) return Optional.empty();
        String buscado = idTarea.trim();
        return todasLasTareas().stream().filter(t -> t.getId().equalsIgnoreCase(buscado)).findFirst();
    }

    public double progreso() {
        return raices.isEmpty() ? 0 : raices.stream().mapToDouble(Tarea::progreso).average().orElse(0);
    }

    @Override public String toString() { return nombre + " [" + id + "]"; }
}
