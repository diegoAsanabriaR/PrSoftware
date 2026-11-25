package Gestor;

import Gestor.Estado.Transicion;
import Gestor.Tarea.Prioridad;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Comando {
    void ejecutar();
    void deshacer();
    String descripcion();

    final class Historial {
        private final Deque<Comando> paraDeshacer = new ArrayDeque<>();
        private final Deque<Comando> paraRehacer = new ArrayDeque<>();

        public void ejecutar(Comando comando) {
            comando.ejecutar();
            paraDeshacer.push(comando);
            paraRehacer.clear();
        }

        public Comando deshacer() {
            if (paraDeshacer.isEmpty()) throw new IllegalStateException("No hay acciones para deshacer");
            Comando c = paraDeshacer.pop();
            c.deshacer();
            paraRehacer.push(c);
            return c;
        }

        public Comando rehacer() {
            if (paraRehacer.isEmpty()) throw new IllegalStateException("No hay acciones para rehacer");
            Comando c = paraRehacer.pop();
            c.ejecutar();
            paraDeshacer.push(c);
            return c;
        }

        public boolean puedeDeshacer() { return !paraDeshacer.isEmpty(); }
        public boolean puedeRehacer() { return !paraRehacer.isEmpty(); }
        public Optional<String> proximaADeshacer() { return Optional.ofNullable(paraDeshacer.peek()).map(Comando::descripcion); }
        public Optional<String> proximaARehacer() { return Optional.ofNullable(paraRehacer.peek()).map(Comando::descripcion); }

        public void limpiar() {
            paraDeshacer.clear();
            paraRehacer.clear();
        }
    }

    final class ProyectoNuevo implements Comando {
        private final Proyecto proyecto;
        private final Consumer<Proyecto> agregar;
        private final Consumer<Proyecto> quitar;

        public ProyectoNuevo(Proyecto proyecto, Consumer<Proyecto> agregar, Consumer<Proyecto> quitar) {
            this.proyecto = proyecto;
            this.agregar = agregar;
            this.quitar = quitar;
        }

        @Override public void ejecutar() { agregar.accept(proyecto); }
        @Override public void deshacer() { quitar.accept(proyecto); }
        @Override public String descripcion() { return "Crear proyecto '" + proyecto.getNombre() + "'"; }
    }

    final class ProyectoBorrado implements Comando {
        private final Proyecto proyecto;
        private final Consumer<Proyecto> quitar;
        private final Consumer<Proyecto> restaurar;

        public ProyectoBorrado(Proyecto proyecto, Consumer<Proyecto> quitar, Consumer<Proyecto> restaurar) {
            this.proyecto = proyecto;
            this.quitar = quitar;
            this.restaurar = restaurar;
        }

        @Override public void ejecutar() { quitar.accept(proyecto); }
        @Override public void deshacer() { restaurar.accept(proyecto); }
        @Override public String descripcion() { return "Eliminar proyecto '" + proyecto.getNombre() + "'"; }
    }

    final class Agregar implements Comando {
        private final Proyecto proyecto;
        private final Tarea.Compuesta padre;
        private final Tarea tarea;

        public Agregar(Proyecto proyecto, Tarea.Compuesta padre, Tarea tarea) {
            this.proyecto = proyecto;
            this.padre = padre;
            this.tarea = tarea;
        }

        @Override public void ejecutar() {
            if (padre != null) {
                padre.validarPuedeAgregar();
                padre.agregar(tarea);
            } else {
                proyecto.agregarRaiz(tarea);
            }
        }

        @Override public void deshacer() {
            if (padre != null) padre.quitar(tarea); else proyecto.quitarRaiz(tarea);
        }

        @Override public String descripcion() { return "Agregar tarea '" + tarea.getTitulo() + "'"; }
    }

    final class Eliminar implements Comando {
        private final Proyecto proyecto;
        private final Tarea tarea;
        private Tarea.Compuesta padreOriginal;
        private int indiceOriginal;

        public Eliminar(Proyecto proyecto, Tarea tarea) {
            this.proyecto = proyecto;
            this.tarea = tarea;
        }

        @Override public void ejecutar() {
            padreOriginal = tarea.getPadre();
            if (padreOriginal != null) {
                indiceOriginal = padreOriginal.indiceDe(tarea);
                padreOriginal.quitar(tarea);
            } else {
                indiceOriginal = proyecto.indiceDe(tarea);
                proyecto.quitarRaiz(tarea);
            }
        }

        @Override public void deshacer() {
            if (padreOriginal != null) padreOriginal.agregarEn(indiceOriginal, tarea);
            else proyecto.agregarRaizEn(indiceOriginal, tarea);
        }

        @Override public String descripcion() { return "Eliminar tarea '" + tarea.getTitulo() + "'"; }
    }

    final class CambiarEstado implements Comando {
        private final Tarea tarea;
        private final Transicion transicion;
        private Estado estadoAnterior;

        public CambiarEstado(Tarea tarea, Transicion transicion) {
            this.tarea = tarea;
            this.transicion = transicion;
        }

        @Override public void ejecutar() {
            estadoAnterior = tarea.getEstado();
            tarea.aplicarTransicion(transicion);
        }

        @Override public void deshacer() { tarea.restaurarEstado(estadoAnterior); }

        @Override public String descripcion() { return transicion.etiqueta() + " la tarea '" + tarea.getTitulo() + "'"; }
    }

    final class Asignar implements Comando {
        private final Tarea tarea;
        private final Responsable responsable;
        private boolean huboCambio;

        public Asignar(Tarea tarea, Responsable responsable) {
            this.tarea = tarea;
            this.responsable = responsable;
        }

        @Override public void ejecutar() {
            huboCambio = tarea.asignarResponsable(responsable);
            if (!huboCambio) throw new IllegalStateException(responsable.getNombre() + " ya es responsable de '" + tarea.getTitulo() + "'");
        }

        @Override public void deshacer() { if (huboCambio) tarea.quitarResponsable(responsable); }

        @Override public String descripcion() { return "Asignar a " + responsable.getNombre() + " la tarea '" + tarea.getTitulo() + "'"; }
    }

    final class Cambio<T> implements Comando {
        private final String descripcion;
        private final Supplier<T> leer;
        private final Consumer<T> escribir;
        private final T nuevo;
        private T anterior;

        public Cambio(String descripcion, Supplier<T> leer, Consumer<T> escribir, T nuevo) {
            this.descripcion = descripcion;
            this.leer = leer;
            this.escribir = escribir;
            this.nuevo = nuevo;
        }

        @Override public void ejecutar() { anterior = leer.get(); escribir.accept(nuevo); }
        @Override public void deshacer() { escribir.accept(anterior); }
        @Override public String descripcion() { return descripcion; }
    }

    static Comando prioridad(Tarea tarea, Prioridad nueva) {
        return new Cambio<>("Cambiar prioridad de '" + tarea.getTitulo() + "' a " + nueva, tarea::getPrioridad, tarea::setPrioridad, nueva);
    }

    static Comando fechaLimite(Tarea tarea, LocalDate nueva) {
        return new Cambio<>("Cambiar fecha límite de '" + tarea.getTitulo() + "' a " + (nueva == null ? "sin fecha" : nueva),
                tarea::getFechaLimite, tarea::setFechaLimite, nueva);
    }
}
