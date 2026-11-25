package Gestor;

import Gestor.Tarea.Prioridad;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class Consulta {
    private Consulta() { }

    public static List<Tarea> ejecutar(Collection<Tarea> tareas, Filtro filtro, CriterioOrden orden) {
        return tareas.stream().filter(filtro::cumple).sorted(orden).toList();
    }

    public interface Filtro {
        boolean cumple(Tarea tarea);
        String descripcion();

        default Filtro y(Filtro otro) {
            Filtro esta = this;
            return de(esta.descripcion() + " y " + otro.descripcion(), t -> esta.cumple(t) && otro.cumple(t));
        }

        static Filtro de(String descripcion, Predicate<Tarea> regla) {
            return new Filtro() {
                @Override public boolean cumple(Tarea t) { return regla.test(t); }
                @Override public String descripcion() { return descripcion; }
            };
        }

        static Filtro todas() { return de("todas", t -> true); }
        static Filtro porEstado(Estado estado) { return de("estado " + estado, t -> t.getEstado() == estado); }
        static Filtro porPrioridad(Prioridad p) { return de("prioridad " + p, t -> t.getPrioridad() == p); }
        static Filtro porResponsable(Responsable r) { return de("responsable " + r.getNombre(), t -> t.getResponsables().contains(r)); }
        static Filtro prioridadMinima(Prioridad minima) {
            return de("prioridad " + minima + " o mayor", t -> t.getPrioridad().peso() >= minima.peso());
        }
        static Filtro sinResponsable() { return de("sin responsable", t -> t.getResponsables().isEmpty()); }
        static Filtro sinFecha() { return de("sin fecha límite", t -> t.getFechaLimite() == null); }
        static Filtro venceEnLosProximos(LocalDate hoy, int dias) {
            return de("vencen en los próximos " + dias + " días", t -> t.getFechaLimite() != null
                    && t.getEstado() != Estado.TERMINADA
                    && !t.getFechaLimite().isBefore(hoy) && !t.getFechaLimite().isAfter(hoy.plusDays(dias)));
        }
        static Filtro venceHoy(LocalDate hoy) {
            return de("vencen hoy", t -> hoy.equals(t.getFechaLimite()) && t.getEstado() != Estado.TERMINADA);
        }
        static Filtro fechaEntre(LocalDate desde, LocalDate hasta) {
            return de("fecha límite entre " + desde + " y " + hasta, t -> t.getFechaLimite() != null
                    && (desde == null || !t.getFechaLimite().isBefore(desde))
                    && (hasta == null || !t.getFechaLimite().isAfter(hasta)));
        }
        static Filtro vencidas(LocalDate hoy) {
            return de("vencidas al " + hoy, t -> t.getFechaLimite() != null && hoy.isAfter(t.getFechaLimite()) && t.getEstado() != Estado.TERMINADA);
        }
    }

    public interface CriterioOrden extends Comparator<Tarea> {
        String nombre();

        static CriterioOrden de(String nombre, Comparator<Tarea> comparador) {
            return new CriterioOrden() {
                @Override public int compare(Tarea a, Tarea b) { return comparador.compare(a, b); }
                @Override public String nombre() { return nombre; }
            };
        }

        static CriterioOrden porPrioridad() {
            return de("prioridad (mayor primero)", (a, b) -> Integer.compare(b.getPrioridad().peso(), a.getPrioridad().peso()));
        }

        static CriterioOrden porFechaLimite() {
            return de("fecha límite (más próxima primero)",
                    Comparator.comparing(Tarea::getFechaLimite, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        static CriterioOrden porResponsable() {
            return de("responsable (A-Z)", Comparator.comparing(
                    (Tarea t) -> t.getResponsables().isEmpty() ? null : t.getResponsables().get(0).getNombre(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        }

        static CriterioOrden porPrioridadAscendente() {
            return de("prioridad (menor primero)", Comparator.comparingInt((Tarea t) -> t.getPrioridad().peso()));
        }

        static CriterioOrden porFechaLimiteDescendente() {
            return de("fecha límite (más lejana primero)",
                    Comparator.comparing(Tarea::getFechaLimite, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())));
        }

        static CriterioOrden porResponsableDescendente() {
            return de("responsable (Z-A)", Comparator.comparing(
                    (Tarea t) -> t.getResponsables().isEmpty() ? null : t.getResponsables().get(0).getNombre(),
                    Comparator.nullsLast(Comparator.<String>reverseOrder())));
        }

        static CriterioOrden porEstado() {
            return de("estado (pendiente primero)", Comparator.comparingInt((Tarea t) -> t.getEstado().ordinal()));
        }

        static CriterioOrden porTitulo() {
            return de("título (A-Z)", Comparator.comparing(Tarea::getTitulo, String.CASE_INSENSITIVE_ORDER));
        }
    }
}
