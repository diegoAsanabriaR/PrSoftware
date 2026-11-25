package Gestor;

import Gestor.Consulta.CriterioOrden;
import Gestor.Consulta.Filtro;
import Gestor.Estado.Transicion;
import Gestor.Observador.Alerta;
import Gestor.Tarea.Prioridad;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GestorProyectos {
    private final Map<String, Proyecto> proyectos = new LinkedHashMap<>();
    private final Map<String, Responsable> responsables = new LinkedHashMap<>();
    private final Supplier<String> idsTareas = secuencia("T");
    private final Supplier<String> idsProyectos = secuencia("PR");
    private final Supplier<String> idsResponsables = secuencia("R");
    private final Comando.Historial historial = new Comando.Historial();
    private final Observador.Recordatorios recordatorios;
    private final Plantilla.Catalogo plantillas = new Plantilla.Catalogo();
    private final Map<String, PlantillaProyecto> plantillasProyecto = new LinkedHashMap<>();
    private final Clock reloj;

    public GestorProyectos(Clock reloj, int diasAnticipacionAlertas) {
        this.reloj = reloj;
        this.recordatorios = new Observador.Recordatorios(diasAnticipacionAlertas);
    }

    private static Supplier<String> secuencia(String prefijo) {
        int[] contador = {0};
        return () -> String.format("%s-%03d", prefijo, ++contador[0]);
    }

    public LocalDate hoy() { return LocalDate.now(reloj); }

    public Proyecto crearProyecto(String nombre) {
        Proyecto p = new Proyecto(idsProyectos.get(), nombre);
        historial.ejecutar(new Comando.ProyectoNuevo(p,
                proyecto -> proyectos.put(proyecto.getId(), proyecto),
                proyecto -> proyectos.remove(proyecto.getId())));
        return p;
    }

    public void eliminarProyecto(String idProyecto) {
        Proyecto p = proyecto(idProyecto);
        int posicion = new ArrayList<>(proyectos.values()).indexOf(p);
        historial.ejecutar(new Comando.ProyectoBorrado(p,
                proyecto -> proyectos.remove(proyecto.getId()),
                proyecto -> insertarProyectoEn(posicion, proyecto)));
    }

    public List<Proyecto> proyectos() { return new ArrayList<>(proyectos.values()); }

    public Proyecto proyecto(String idProyecto) {
        Proyecto p = idProyecto == null ? null : proyectos.get(idProyecto.trim().toUpperCase());
        if (p == null) throw new IllegalArgumentException("No existe el proyecto " + idProyecto);
        return p;
    }

    public Responsable registrarResponsable(String nombre, String correo) {
        Responsable r = new Responsable(idsResponsables.get(), nombre, correo);
        responsables.put(r.getId(), r);
        return r;
    }

    public List<Responsable> responsables() { return new ArrayList<>(responsables.values()); }

    public Responsable responsable(String idResponsable) {
        Responsable r = idResponsable == null ? null : responsables.get(idResponsable.trim().toUpperCase());
        if (r == null) throw new IllegalArgumentException("No existe el responsable " + idResponsable);
        return r;
    }

    public Tarea agregarTarea(String idProyecto, String idPadre, String titulo, String descripcion,
                              Prioridad prioridad, LocalDate fechaLimite, boolean compuesta) {
        Proyecto p = proyecto(idProyecto);
        Tarea.Compuesta padre = buscarPadre(p, idPadre);
        String id = idsTareas.get();
        Tarea nueva = compuesta ? new Tarea.Compuesta(id, titulo, descripcion, prioridad, fechaLimite)
                                : new Tarea.Simple(id, titulo, descripcion, prioridad, fechaLimite);
        historial.ejecutar(new Comando.Agregar(p, padre, nueva));
        return nueva;
    }

    public void eliminarTarea(String idTarea) {
        Tarea t = buscarTarea(idTarea);
        historial.ejecutar(new Comando.Eliminar(proyectoDe(t), t));
    }

    public void cambiarEstado(String idTarea, Transicion transicion) {
        historial.ejecutar(new Comando.CambiarEstado(buscarTarea(idTarea), transicion));
    }

    public void cambiarPrioridad(String idTarea, Prioridad prioridad) {
        historial.ejecutar(Comando.prioridad(buscarTarea(idTarea), prioridad));
    }

    public void cambiarFechaLimite(String idTarea, LocalDate fecha) {
        historial.ejecutar(Comando.fechaLimite(buscarTarea(idTarea), fecha));
    }

    public void asignarResponsable(String idTarea, String idResponsable) {
        historial.ejecutar(new Comando.Asignar(buscarTarea(idTarea), responsable(idResponsable)));
    }

    public String deshacer() { return historial.deshacer().descripcion(); }
    public String rehacer() { return historial.rehacer().descripcion(); }
    public boolean puedeDeshacer() { return historial.puedeDeshacer(); }
    public boolean puedeRehacer() { return historial.puedeRehacer(); }
    public String proximaADeshacer() { return historial.proximaADeshacer().orElse(null); }
    public String proximaARehacer() { return historial.proximaARehacer().orElse(null); }
    public void limpiarHistorial() { historial.limpiar(); }

    public Plantilla guardarComoPlantilla(String idTarea, String nombre) {
        Tarea t = buscarTarea(idTarea);
        Plantilla plantilla = new Plantilla(nombre, t, t.getFechaLimite() != null ? t.getFechaLimite() : hoy());
        plantillas.registrar(plantilla);
        return plantilla;
    }

    public List<String> nombresPlantillas() { return plantillas.nombres(); }

    public Tarea crearDesdePlantilla(String nombrePlantilla, String idProyecto, String idPadre, LocalDate fechaBase) {
        Plantilla plantilla = plantillas.buscar(nombrePlantilla)
                .orElseThrow(() -> new IllegalArgumentException("No existe la plantilla '" + nombrePlantilla + "'"));
        Proyecto p = proyecto(idProyecto);
        Tarea.Compuesta padre = buscarPadre(p, idPadre);
        Tarea copia = plantilla.instanciar(fechaBase, idsTareas);
        historial.ejecutar(new Comando.Agregar(p, padre, copia));
        return copia;
    }

    public PlantillaProyecto guardarProyectoComoPlantilla(String idProyecto, String nombre) {
        PlantillaProyecto plantilla = new PlantillaProyecto(nombre, proyecto(idProyecto), hoy());
        plantillasProyecto.put(nombre.trim().toLowerCase(), plantilla);
        return plantilla;
    }

    public List<String> nombresPlantillasProyecto() {
        return plantillasProyecto.values().stream().map(PlantillaProyecto::getNombre).toList();
    }
    
    public Proyecto crearProyectoDesdePlantilla(String nombrePlantilla, String nombreProyecto, LocalDate nuevaFechaBase) {
        if (nombrePlantilla == null || !plantillasProyecto.containsKey(nombrePlantilla.trim().toLowerCase()))
            throw new IllegalArgumentException("No existe la plantilla de proyecto '" + nombrePlantilla + "'");
        Proyecto nuevo = crearProyecto(nombreProyecto);
        PlantillaProyecto plantilla = plantillasProyecto.get(nombrePlantilla.trim().toLowerCase());
        for (Tarea tarea : plantilla.instanciar(nuevaFechaBase == null ? hoy() : nuevaFechaBase, idsTareas)) {
            nuevo.agregarRaiz(tarea);
        }
        return nuevo;
    }
    
    public List<Tarea> consultar(String idProyecto, Filtro filtro, CriterioOrden orden) {
        List<Tarea> base = new ArrayList<>();
        if (idProyecto == null || idProyecto.isBlank()) proyectos.values().forEach(p -> base.addAll(p.todasLasTareas()));
        else base.addAll(proyecto(idProyecto).todasLasTareas());
        return Consulta.ejecutar(base, filtro, orden);
    }

    public Tarea buscarTarea(String idTarea) {
        for (Proyecto p : proyectos.values()) {
            var encontrada = p.buscar(idTarea);
            if (encontrada.isPresent()) return encontrada.get();
        }
        throw new IllegalArgumentException("No existe la tarea " + idTarea);
    }

    public List<Alerta> revisarRecordatorios(LocalDate fecha) { return recordatorios.revisar(proyectos.values(), fecha); }
    public List<Alerta> revisarRecordatorios() { return revisarRecordatorios(hoy()); }
    public List<Alerta> bandejaDe(String idResponsable) { return responsable(idResponsable).getBandeja(); }

    private Tarea.Compuesta buscarPadre(Proyecto p, String idPadre) {
        if (idPadre == null || idPadre.isBlank()) return null;
        Tarea t = p.buscar(idPadre).orElseThrow(() -> new IllegalArgumentException("No existe la tarea " + idPadre + " en ese proyecto"));
        if (t instanceof Tarea.Compuesta c) return c;
        throw new IllegalArgumentException("La tarea " + idPadre + " es simple: no admite subtareas");
    }

    private void insertarProyectoEn(int posicion, Proyecto nuevo) {
        List<Proyecto> orden = new ArrayList<>(proyectos.values());
        orden.add(Math.min(posicion, orden.size()), nuevo);
        proyectos.clear();
        orden.forEach(pr -> proyectos.put(pr.getId(), pr));
    }

    private Proyecto proyectoDe(Tarea t) {
        for (Proyecto p : proyectos.values()) {
            if (p.todasLasTareas().contains(t)) return p;
        }
        throw new IllegalStateException("La tarea no pertenece a ningún proyecto");
    }
}
