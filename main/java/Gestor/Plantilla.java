package Gestor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Plantilla {
    private final String nombre;
    private final Tarea prototipo;
    private final LocalDate fechaBase;

    public Plantilla(String nombre, Tarea origen, LocalDate fechaBase) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la plantilla es obligatorio");
        this.nombre = nombre.trim();
        this.fechaBase = fechaBase;
        int[] contador = {0};
        this.prototipo = origen.copiar(() -> "PL-" + (++contador[0]), 0);
    }

    public Tarea instanciar(LocalDate nuevaFechaBase, Supplier<String> ids) {
        return prototipo.copiar(ids, ChronoUnit.DAYS.between(fechaBase, nuevaFechaBase));
    }

    public String getNombre() { return nombre; }
    public LocalDate getFechaBase() { return fechaBase; }
    public int totalTareas() { return prototipo.totalTareas(); }

    public static class Catalogo {
        private final Map<String, Plantilla> plantillas = new LinkedHashMap<>();

        public void registrar(Plantilla p) { plantillas.put(p.getNombre().toLowerCase(), p); }

        public Optional<Plantilla> buscar(String nombre) {
            return Optional.ofNullable(nombre == null ? null : plantillas.get(nombre.trim().toLowerCase()));
        }

        public List<String> nombres() { return plantillas.values().stream().map(Plantilla::getNombre).toList(); }

        public List<Plantilla> listar() { return new ArrayList<>(plantillas.values()); }
    }
}
