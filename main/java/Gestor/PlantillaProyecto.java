package Gestor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class PlantillaProyecto {
    private final String nombre;
    private final LocalDate fechaBase;
    private final List<Tarea> prototipos = new ArrayList<>();

    public PlantillaProyecto(String nombre, Proyecto origen, LocalDate fechaBase) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre de la plantilla es obligatorio");
        if (origen == null) throw new IllegalArgumentException("El proyecto de origen es obligatorio");
        if (origen.getRaices().isEmpty()) throw new IllegalArgumentException("El proyecto no tiene tareas para guardar");
        this.nombre = nombre.trim();
        this.fechaBase = fechaBase == null ? LocalDate.now() : fechaBase;
        int[] contador = {0};
        for (Tarea tarea : origen.getRaices()) {
            prototipos.add(tarea.copiar(() -> "PL-PROY-" + (++contador[0]), 0));
        }
    }

    public List<Tarea> instanciar(LocalDate nuevaFechaBase, Supplier<String> ids) {
        LocalDate base = nuevaFechaBase == null ? fechaBase : nuevaFechaBase;
        long desplazamiento = ChronoUnit.DAYS.between(fechaBase, base);
        List<Tarea> copias = new ArrayList<>();
        for (Tarea prototipo : prototipos) copias.add(prototipo.copiar(ids, desplazamiento));
        return copias;
    }

    public String getNombre() { return nombre; }
    public int totalTareas() { return prototipos.stream().mapToInt(Tarea::totalTareas).sum(); }
}
