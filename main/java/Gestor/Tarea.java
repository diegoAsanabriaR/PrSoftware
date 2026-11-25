package Gestor;

import Gestor.Estado.Transicion;
import Gestor.Observador.Alerta;
import Gestor.Observador.TipoAlerta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class Tarea {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public enum Prioridad {
        BAJA(1), MEDIA(2), ALTA(3), CRITICA(4);

        private final int peso;

        Prioridad(int peso) { this.peso = peso; }

        public int peso() { return peso; }
    }

    private final String id;
    private final String titulo;
    private final String descripcion;
    private Prioridad prioridad;
    private LocalDate fechaLimite;
    private Estado estado = Estado.PENDIENTE;
    private final List<Responsable> responsables = new ArrayList<>();
    private final List<Observador> observadores = new ArrayList<>();
    private Compuesta padre;

    protected Tarea(String id, String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El id de la tarea es obligatorio");
        if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("El título de la tarea es obligatorio");
        this.id = id;
        this.titulo = titulo.trim();
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.prioridad = prioridad == null ? Prioridad.MEDIA : prioridad;
        this.fechaLimite = fechaLimite;
    }

    public abstract int totalTareas();
    public abstract double progreso();
    public abstract List<Tarea> aplanar();
    abstract boolean puedeTerminar();
    public boolean esCompuesta() { return false; }
    public List<Tarea> getHijos() { return List.of(); }

    public abstract Tarea copiar(Supplier<String> ids, long dias);

    private static LocalDate desplazar(LocalDate fecha, long dias) { return fecha == null ? null : fecha.plusDays(dias); }

    public void aplicarTransicion(Transicion transicion) { transicion.ejecutar(estado, this); }

    void transicionarA(Estado nuevo) {
        Estado anterior = estado;
        estado = nuevo;
        notificar(new Alerta(TipoAlerta.ESTADO_CAMBIADO, id, "La tarea '" + titulo + "' pasó de " + anterior + " a " + nuevo));
    }

    void restaurarEstado(Estado anterior) { estado = anterior; }

    boolean padreTerminado() { return padre != null && padre.getEstado() == Estado.TERMINADA; }

    public Estado getEstado() { return estado; }

    public void agregarObservador(Observador o) { if (!observadores.contains(o)) observadores.add(o); }
    public void quitarObservador(Observador o) { observadores.remove(o); }

    public void notificar(Alerta alerta) {
        for (Observador o : List.copyOf(observadores)) o.alertar(alerta);
    }

    public boolean asignarResponsable(Responsable r) {
        if (responsables.contains(r)) return false;
        responsables.add(r);
        agregarObservador(r);
        return true;
    }

    public boolean quitarResponsable(Responsable r) {
        boolean quitado = responsables.remove(r);
        if (quitado) quitarObservador(r);
        return quitado;
    }

    public List<Responsable> getResponsables() { return Collections.unmodifiableList(responsables); }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad p) { this.prioridad = p == null ? Prioridad.MEDIA : p; }
    public LocalDate getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDate f) { this.fechaLimite = f; }
    public Compuesta getPadre() { return padre; }
    private void setPadre(Compuesta p) { this.padre = p; }

    public String resumen() {
        String resp = responsables.isEmpty() ? ""
                : " - resp: " + responsables.stream().map(Responsable::getNombre).collect(Collectors.joining(", "));
        return String.format("[%s] %s%s - %s - %s%s - %.0f%%%s", id, titulo, esCompuesta() ? " (compuesta)" : "",
                estado, prioridad, fechaLimite == null ? "" : " - vence " + fechaLimite.format(FORMATO_FECHA), progreso() * 100, resp);
    }

    @Override public String toString() { return titulo + " [" + id + "]"; }

    public static final class Simple extends Tarea {
        public Simple(String id, String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite) {
            super(id, titulo, descripcion, prioridad, fechaLimite);
        }

        @Override public int totalTareas() { return 1; }
        @Override public double progreso() { return getEstado() == Estado.TERMINADA ? 1.0 : 0.0; }
        @Override boolean puedeTerminar() { return true; }

        @Override public List<Tarea> aplanar() {
            List<Tarea> lista = new ArrayList<>();
            lista.add(this);
            return lista;
        }

        @Override public Tarea copiar(Supplier<String> ids, long dias) {
            return new Simple(ids.get(), getTitulo(), getDescripcion(), getPrioridad(), desplazar(getFechaLimite(), dias));
        }
    }

    public static final class Compuesta extends Tarea {
        private final List<Tarea> hijos = new ArrayList<>();

        public Compuesta(String id, String titulo, String descripcion, Prioridad prioridad, LocalDate fechaLimite) {
            super(id, titulo, descripcion, prioridad, fechaLimite);
        }

        public void agregar(Tarea hija) { agregarEn(hijos.size(), hija); }

        public void agregarEn(int indice, Tarea hija) {
            hijos.add(indice, hija);
            hija.setPadre(this);
        }

        public boolean quitar(Tarea hija) {
            boolean quitada = hijos.remove(hija);
            if (quitada) hija.setPadre(null);
            return quitada;
        }

        public int indiceDe(Tarea hija) { return hijos.indexOf(hija); }

        public void validarPuedeAgregar() {
            if (getEstado() == Estado.TERMINADA) {
                throw new IllegalStateException("La tarea '" + getTitulo() + "' ya está terminada: reábrela para agregarle subtareas");
            }
        }

        @Override public boolean esCompuesta() { return true; }
        @Override public List<Tarea> getHijos() { return Collections.unmodifiableList(hijos); }
        @Override public int totalTareas() { return 1 + hijos.stream().mapToInt(Tarea::totalTareas).sum(); }
        @Override public double progreso() {
            if (hijos.isEmpty()) return getEstado() == Estado.TERMINADA ? 1.0 : 0.0;
            return hijos.stream().mapToDouble(Tarea::progreso).average().orElse(0);
        }

        @Override public List<Tarea> aplanar() {
            List<Tarea> lista = new ArrayList<>();
            lista.add(this);
            hijos.forEach(h -> lista.addAll(h.aplanar()));
            return lista;
        }

        @Override boolean puedeTerminar() { return hijos.stream().allMatch(h -> h.getEstado() == Estado.TERMINADA); }

        @Override public Tarea copiar(Supplier<String> ids, long dias) {
            Compuesta copia = new Compuesta(ids.get(), getTitulo(), getDescripcion(), getPrioridad(), desplazar(getFechaLimite(), dias));
            for (Tarea hija : hijos) copia.agregar(hija.copiar(ids, dias));
            return copia;
        }
    }
}
