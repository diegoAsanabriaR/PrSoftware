package Gestor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface Observador {
    void alertar(Alerta alerta);

    enum TipoAlerta { ESTADO_CAMBIADO, POR_VENCER, VENCIDA }

    record Alerta(TipoAlerta tipo, String idTarea, String mensaje) {
        @Override public String toString() { return "[" + tipo + "] " + mensaje; }
    }

    final class Recordatorios {
        private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        private final int diasAnticipacion;
        private final Set<String> yaEnviadas = new HashSet<>();

        public Recordatorios(int diasAnticipacion) {
            if (diasAnticipacion < 0) throw new IllegalArgumentException("Los días de anticipación no pueden ser negativos");
            this.diasAnticipacion = diasAnticipacion;
        }

        public List<Alerta> revisar(Collection<Proyecto> proyectos, LocalDate hoy) {
            List<Alerta> emitidas = new ArrayList<>();
            for (Proyecto p : proyectos) {
                for (Tarea t : p.todasLasTareas()) {
                    Alerta alerta = evaluar(t, hoy);
                    if (alerta != null) {
                        t.notificar(alerta);
                        emitidas.add(alerta);
                    }
                }
            }
            return emitidas;
        }

        private Alerta evaluar(Tarea t, LocalDate hoy) {
            LocalDate limite = t.getFechaLimite();
            if (limite == null || t.getEstado() == Estado.TERMINADA) return null;

            TipoAlerta tipo;
            String mensaje;
            if (hoy.isAfter(limite)) {
                tipo = TipoAlerta.VENCIDA;
                mensaje = "La tarea '" + t.getTitulo() + "' venció el " + limite.format(FORMATO_FECHA) + " (hace " + ChronoUnit.DAYS.between(limite, hoy) + " día(s))";
            } else if (ChronoUnit.DAYS.between(hoy, limite) <= diasAnticipacion) {
                tipo = TipoAlerta.POR_VENCER;
                mensaje = "La tarea '" + t.getTitulo() + "' vence el " + limite.format(FORMATO_FECHA) + " (en " + ChronoUnit.DAYS.between(hoy, limite) + " día(s))";
            } else {
                return null;
            }
            return yaEnviadas.add(t.getId() + ":" + tipo + ":" + limite) ? new Alerta(tipo, t.getId(), mensaje) : null;
        }
    }
}
