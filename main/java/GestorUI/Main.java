package GestorUI;

import Gestor.Estado.Transicion;
import Gestor.GestorProyectos;
import Gestor.Proyecto;
import Gestor.Responsable;
import Gestor.Tarea;
import Gestor.Tarea.Prioridad;

import java.time.Clock;
import java.time.LocalDate;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        GestorProyectos gestor = crearDemo();
        String modo = args.length > 0 ? args[0].toLowerCase() : "gui";
        switch (modo) {
            case "gui" -> new AppGrafica(gestor).iniciar();
            default -> System.out.println("Uso: java -jar gestor.jar [consola|gui]");
        }
    }
    
    public static GestorProyectos crearDemo() {
        GestorProyectos g = new GestorProyectos(Clock.systemDefaultZone(), 2);
        LocalDate hoy = g.hoy();

        Responsable ana = g.registrarResponsable("Ana Ríos", "ana@correo.co");
        Responsable luis = g.registrarResponsable("Luis Mora", "luis@correo.co");
        Responsable marcela = g.registrarResponsable("Marcela Gómez", "marcela@correo.co");

        Proyecto app = g.crearProyecto("Lanzamiento de la app");
        String p = app.getId();

        Tarea diseno = g.agregarTarea(p, null, "Diseño", "Interfaz y experiencia de usuario", Prioridad.ALTA, hoy.plusDays(10), true);
        Tarea wire = g.agregarTarea(p, diseno.getId(), "Wireframes", "", Prioridad.ALTA, hoy.plusDays(2), false);
        Tarea proto = g.agregarTarea(p, diseno.getId(), "Prototipo navegable", "", Prioridad.MEDIA, hoy.plusDays(7), false);

        Tarea desarrollo = g.agregarTarea(p, null, "Desarrollo", "Backend y frontend", Prioridad.CRITICA, hoy.plusDays(20), true);
        Tarea back = g.agregarTarea(p, desarrollo.getId(), "API de tareas", "", Prioridad.CRITICA, hoy.plusDays(14), false);
        Tarea front = g.agregarTarea(p, desarrollo.getId(), "Pantallas principales", "", Prioridad.ALTA, hoy.plusDays(18), false);

        Tarea pruebas = g.agregarTarea(p, null, "Pruebas de aceptación", "", Prioridad.MEDIA, hoy.minusDays(1), false);

        g.asignarResponsable(wire.getId(), ana.getId());
        g.asignarResponsable(proto.getId(), ana.getId());
        g.asignarResponsable(back.getId(), luis.getId());
        g.asignarResponsable(front.getId(), luis.getId());
        g.asignarResponsable(front.getId(), marcela.getId());
        g.asignarResponsable(pruebas.getId(), marcela.getId());

        g.cambiarEstado(wire.getId(), Transicion.INICIAR);
        g.cambiarEstado(back.getId(), Transicion.INICIAR);
        g.cambiarEstado(back.getId(), Transicion.ENVIAR_A_REVISION);

        g.guardarComoPlantilla(diseno.getId(), "Fase de diseño");
        g.limpiarHistorial();
        return g;
    }
}
