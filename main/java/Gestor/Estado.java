package Gestor;

import java.util.List;

public enum Estado {
    PENDIENTE {
        @Override public List<Transicion> permitidas() { return List.of(Transicion.INICIAR); }
        @Override void iniciar(Tarea t) { t.transicionarA(EN_CURSO); }
    },
    EN_CURSO {
        @Override public List<Transicion> permitidas() { return List.of(Transicion.ENVIAR_A_REVISION, Transicion.PAUSAR); }
        @Override void enviarARevision(Tarea t) { t.transicionarA(EN_REVISION); }
        @Override void pausar(Tarea t) { t.transicionarA(PENDIENTE); }
    },
    EN_REVISION {
        @Override public List<Transicion> permitidas() { return List.of(Transicion.APROBAR, Transicion.RECHAZAR); }
        @Override void aprobar(Tarea t) {
            if (!t.puedeTerminar()) {
                throw new TransicionInvalidaException("No se puede aprobar la tarea '" + t.getTitulo() + "': tiene subtareas sin terminar");
            }
            t.transicionarA(TERMINADA);
        }
        @Override void rechazar(Tarea t) { t.transicionarA(EN_CURSO); }
    },
    TERMINADA {
        @Override public List<Transicion> permitidas() { return List.of(Transicion.REABRIR); }
        @Override void reabrir(Tarea t) {
            if (t.padreTerminado()) {
                throw new TransicionInvalidaException("No se puede reabrir '" + t.getTitulo() + "': su tarea padre está terminada. Reabre primero el padre");
            }
            t.transicionarA(EN_CURSO);
        }
    };

    public abstract List<Transicion> permitidas();

    void iniciar(Tarea t) { throw noPermitida(Transicion.INICIAR, t); }
    void pausar(Tarea t) { throw noPermitida(Transicion.PAUSAR, t); }
    void enviarARevision(Tarea t) { throw noPermitida(Transicion.ENVIAR_A_REVISION, t); }
    void aprobar(Tarea t) { throw noPermitida(Transicion.APROBAR, t); }
    void rechazar(Tarea t) { throw noPermitida(Transicion.RECHAZAR, t); }
    void reabrir(Tarea t) { throw noPermitida(Transicion.REABRIR, t); }

    private TransicionInvalidaException noPermitida(Transicion transicion, Tarea t) {
        return new TransicionInvalidaException("No se puede '" + transicion.etiqueta() + "' la tarea '" + t.getTitulo()
                + "' porque está " + this + ". Desde este estado se permite: " + permitidas());
    }

    public enum Transicion {
        INICIAR("Iniciar") { @Override void ejecutar(Estado e, Tarea t) { e.iniciar(t); } },
        PAUSAR("Pausar") { @Override void ejecutar(Estado e, Tarea t) { e.pausar(t); } },
        ENVIAR_A_REVISION("Enviar a revisión") { @Override void ejecutar(Estado e, Tarea t) { e.enviarARevision(t); } },
        APROBAR("Aprobar") { @Override void ejecutar(Estado e, Tarea t) { e.aprobar(t); } },
        RECHAZAR("Rechazar") { @Override void ejecutar(Estado e, Tarea t) { e.rechazar(t); } },
        REABRIR("Reabrir") { @Override void ejecutar(Estado e, Tarea t) { e.reabrir(t); } };

        private final String etiqueta;

        Transicion(String etiqueta) { this.etiqueta = etiqueta; }

        public String etiqueta() { return etiqueta; }

        abstract void ejecutar(Estado estado, Tarea tarea);
    }

    public static class TransicionInvalidaException extends RuntimeException {
        public TransicionInvalidaException(String mensaje) { super(mensaje); }
    }
}
