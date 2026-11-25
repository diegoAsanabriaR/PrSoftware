package GestorUI;

import Gestor.Consulta.CriterioOrden;
import Gestor.Consulta.Filtro;
import Gestor.Estado;
import Gestor.Estado.Transicion;
import Gestor.GestorProyectos;
import Gestor.Observador.Alerta;
import Gestor.Proyecto;
import Gestor.Responsable;
import Gestor.Tarea;
import Gestor.Tarea.Prioridad;


import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Icon;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class AppGrafica {
    private final GestorProyectos gestor;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private JFrame ventana;
    private JComboBox<Proyecto> comboProyectos;
    private JComboBox<OpcionFiltro> filtroPrioridad;
    private JComboBox<OpcionFiltro> filtroFecha;
    private JComboBox<OpcionFiltro> filtroResponsable;
    private JComboBox<OpcionOrden> ordenArbol;
    private RenderizadorTareas renderizador;
    private boolean cargando;
    private JTree arbol;
    private DefaultTreeModel modeloArbol;
    private JLabel barraEstado;
    private JButton botonDeshacer;
    private JButton botonRehacer;
    private javax.swing.Timer temporizadorRecordatorios;

    public AppGrafica(GestorProyectos gestor) { this.gestor = gestor; }

    public void iniciar() { SwingUtilities.invokeLater(this::construirVentana); }

    private void construirVentana() {
        Tema.aplicar();
        ventana = new JFrame("Gestor de tareas");
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setMinimumSize(new Dimension(900, 600));
        ventana.setSize(1050, 700);
        ventana.setLocationRelativeTo(null);
        ventana.getContentPane().setBackground(Tema.FONDO);
        ventana.setLayout(new BorderLayout(12, 12));

        JPanel encabezado = new JPanel(new BorderLayout(12, 8));
        encabezado.setBackground(Tema.PANEL);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));

        JLabel titulo = new JLabel("Gestor de tareas");
        titulo.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 22));
        titulo.setForeground(Tema.TEXTO);
        encabezado.add(titulo, BorderLayout.WEST);

        JPanel selector = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        selector.setOpaque(false);
        JLabel lblProyecto = new JLabel("Proyecto:");
        lblProyecto.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        comboProyectos = new JComboBox<>();
        comboProyectos.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        comboProyectos.setPreferredSize(new Dimension(220, 30));
        recargarProyectos();
        comboProyectos.addActionListener(e -> { if (!cargando) refrescar(); });
        selector.add(lblProyecto);
        selector.add(comboProyectos);
        selector.add(botonAccion("Nuevo proyecto", this::nuevoProyecto, true));
        selector.add(botonAccion("Eliminar proyecto", this::eliminarProyecto, false));
        encabezado.add(selector, BorderLayout.EAST);
        ventana.add(encabezado, BorderLayout.NORTH);

        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        contenido.setOpaque(false);
        contenido.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));

        JPanel herramientas = barra();
        herramientas.add(botonAccion("Nueva tarea", this::nuevaTareaPrincipal, true));
        herramientas.add(botonAccion("Subtarea", () -> nuevaTarea(true), false));
        herramientas.add(botonAccion("Estado", this::cambiarEstado, false));
        herramientas.add(botonAccion("Responsable", this::asignarResponsable, false));
        herramientas.add(botonAccion("Prioridad", this::cambiarPrioridad, false));
        herramientas.add(botonAccion("Fecha", this::cambiarFecha, false));
        herramientas.add(botonAccion("Eliminar tarea", this::eliminar, false));

        JPanel filtros = barra();
        filtroPrioridad = comboFiltro("Prioridad", opcionesPrioridad());
        filtroFecha = comboFiltro("Fecha", opcionesFecha());
        filtroResponsable = comboFiltro("Responsable", opcionesResponsable());
        ordenArbol = new JComboBox<>(opcionesOrden(true).toArray(new OpcionOrden[0]));
        ordenArbol.setRenderer(new PrefijoRenderer("Orden"));
        ordenArbol.setPreferredSize(new Dimension(290, 30));
        ordenArbol.addActionListener(e -> { if (!cargando) refrescar(); });
        filtros.add(filtroPrioridad);
        filtros.add(filtroFecha);
        filtros.add(filtroResponsable);
        filtros.add(ordenArbol);
        filtros.add(botonAccion("Limpiar", this::limpiarFiltros, false));

        JPanel barras = new JPanel(new GridLayout(2, 1, 0, 8));
        barras.setOpaque(false);
        barras.add(herramientas);
        barras.add(filtros);
        contenido.add(barras, BorderLayout.NORTH);

        JPanel panelTareas = new JPanel(new BorderLayout(0, 8));
        panelTareas.setBackground(Tema.PANEL);
        panelTareas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JPanel cabeceraTareas = new JPanel(new BorderLayout(10, 0));
        cabeceraTareas.setOpaque(false);
        JLabel tituloTareas = new JLabel("Tareas del proyecto");
        tituloTareas.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
        tituloTareas.setForeground(Tema.TEXTO);
        cabeceraTareas.add(tituloTareas, BorderLayout.WEST);
        cabeceraTareas.add(leyendaPrioridades(), BorderLayout.EAST);
        panelTareas.add(cabeceraTareas, BorderLayout.NORTH);

        modeloArbol = new DefaultTreeModel(new DefaultMutableTreeNode("No hay proyectos. Usa 'Nuevo proyecto' para comenzar."));
        arbol = new JTree(modeloArbol);
        renderizador = new RenderizadorTareas();
        arbol.setCellRenderer(renderizador);
        arbol.setRootVisible(true);
        arbol.setShowsRootHandles(true);
        arbol.setRowHeight(29);
        arbol.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        arbol.setBackground(Tema.PANEL);
        JScrollPane scrollArbol = new JScrollPane(arbol);
        scrollArbol.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        scrollArbol.getViewport().setBackground(Tema.PANEL);
        panelTareas.add(scrollArbol, BorderLayout.CENTER);
        contenido.add(panelTareas, BorderLayout.CENTER);
        ventana.add(contenido, BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(8, 0));
        pie.setBackground(Tema.FONDO);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        JPanel secundarias = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        secundarias.setOpaque(false);
        secundarias.add(botonAccion("Responsables", this::nuevoResponsable, false));
        secundarias.add(botonAccion("Consultar", this::consultar, false));
        secundarias.add(botonAccion("Recordatorios", this::recordatorios, false));
        secundarias.add(botonAccion("Alertas", this::bandeja, false));
        secundarias.add(botonAccion("Guardar plantilla", this::guardarPlantilla, false));
        secundarias.add(botonAccion("Usar plantilla", this::desdePlantilla, false));
        secundarias.add(botonAccion("Guardar proyecto", this::guardarPlantillaProyecto, false));
        secundarias.add(botonAccion("Crear desde proyecto", this::usarPlantillaProyecto, false));
        pie.add(secundarias, BorderLayout.NORTH);

        JPanel estadoPanel = new JPanel(new BorderLayout());
        estadoPanel.setOpaque(false);
        barraEstado = new JLabel("Listo. Selecciona una tarea para comenzar.");
        barraEstado.setForeground(Tema.TEXTO_SUAVE);
        barraEstado.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        estadoPanel.add(barraEstado, BorderLayout.CENTER);
        JPanel historial = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        historial.setOpaque(false);
        botonDeshacer = botonAccion("Deshacer", this::deshacer, false);
        botonRehacer = botonAccion("Rehacer", this::rehacer, false);
        historial.add(botonDeshacer);
        historial.add(botonRehacer);
        estadoPanel.add(historial, BorderLayout.EAST);
        pie.add(estadoPanel, BorderLayout.SOUTH);
        ventana.add(pie, BorderLayout.SOUTH);

        refrescar();
        ventana.setVisible(true);
        temporizadorRecordatorios = new javax.swing.Timer(60_000, e -> revisarAlertasAutomaticas());
        temporizadorRecordatorios.start();
        revisarAlertasAutomaticas();
    }

    private JPanel barra() {
        JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 8));
        b.setBackground(Tema.PANEL);
        b.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        return b;
    }

    private JLabel leyendaPrioridades() {
        StringBuilder html = new StringBuilder("<html>");
        Prioridad[] orden = {Prioridad.CRITICA, Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA};
        for (Prioridad pr : orden) {
            Color c = Tema.colorPrioridad(pr);
            html.append(String.format("<span style='color:#%02x%02x%02x'>&#9679; %s</span>&nbsp;&nbsp;&nbsp;",
                    c.getRed(), c.getGreen(), c.getBlue(), Tema.etiquetaPrioridad(pr)));
        }
        JLabel l = new JLabel(html.append("</html>").toString());
        l.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        return l;
    }

    private JComboBox<OpcionFiltro> comboFiltro(String prefijo, List<OpcionFiltro> opciones) {
        JComboBox<OpcionFiltro> combo = new JComboBox<>(opciones.toArray(new OpcionFiltro[0]));
        combo.setRenderer(new PrefijoRenderer(prefijo));
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width + 8, 30));
        combo.addActionListener(e -> { if (!cargando) refrescar(); });
        return combo;
    }

    private JPanel tarjetaResumen(String etiqueta, String titulo, String detalle) {
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(225,231,239)),
                BorderFactory.createEmptyBorder(13, 16, 13, 12)));
        JLabel e = new JLabel(etiqueta);
        e.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 9));
        e.setForeground(new Color(45, 116, 190));
        JLabel t = new JLabel(titulo);
        t.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        t.setForeground(new Color(39, 53, 77));
        JLabel d = new JLabel(detalle);
        d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
        d.setForeground(new Color(125, 139, 158));
        p.add(e); p.add(t); p.add(d);
        return p;
    }

    private JLabel etiquetaSeccion(String texto) {
        JLabel l = new JLabel("  " + texto);
        l.setForeground(new Color(128, 151, 184));
        l.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 10));
        return l;
    }

    private Component espacio(int alto) { JPanel p = new JPanel(); p.setOpaque(false); p.setPreferredSize(new Dimension(1, alto)); return p; }

    private JButton botonLateral(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        b.setForeground(new Color(226, 234, 246));
        b.setBackground(new Color(35, 53, 83));
        b.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 8));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        b.addActionListener(e -> ejecutarAccion(accion));
        return b;
    }

    private JButton botonAccion(String texto, Runnable accion, boolean principal) {
        JButton b = new JButton(texto);
        Color base = principal ? Tema.BOTON_PRINCIPAL : Tema.BOTON;
        Color sobre = principal ? Tema.BOTON_PRINCIPAL.brighter() : Tema.BOTON_SOBRE;
        b.setFont(new java.awt.Font("SansSerif", principal ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 12));
        b.setForeground(principal ? Color.WHITE : Tema.TEXTO);
        b.setBackground(base);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BOTON_BORDE),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(sobre); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(base); }
        });
        b.addActionListener(e -> ejecutarAccion(accion));
        return b;
    }

    private void nuevaTareaPrincipal() { nuevaTarea(false); }

    private void ejecutarAccion(Runnable accion) {
        try { accion.run(); }
        catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(ventana, ex.getMessage(), "No se pudo completar la acción", JOptionPane.WARNING_MESSAGE);
        }
        recargarProyectos();
        refrescar();
    }

    private void nuevoProyecto() {
        String nombre = JOptionPane.showInputDialog(ventana, "Nombre del proyecto:");
        if (nombre == null) return;
        Proyecto p = gestor.crearProyecto(nombre);
        recargarProyectos();
        comboProyectos.setSelectedItem(p);
    }

    private void eliminarProyecto() {
        Proyecto p = exigirProyecto();
        int total = p.todasLasTareas().size();
        String detalle = total == 0 ? "No tiene tareas." : "Se eliminarán también sus " + total + " tarea(s).";
        int respuesta = JOptionPane.showConfirmDialog(ventana,
                "¿Eliminar el proyecto '" + p.getNombre() + "'?\n" + detalle + "\n(Podrás deshacerlo)",
                "Eliminar proyecto", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) return;
        gestor.eliminarProyecto(p.getId());
        estado("Proyecto eliminado: " + p.getNombre());
    }

    private void nuevoResponsable() {
        JTextField nombre = new JTextField();
        JTextField correo = new JTextField();
        JPanel form = formulario("Nombre:", nombre, "Correo:", correo);
        if (JOptionPane.showConfirmDialog(ventana, form, "Nuevo responsable", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Responsable r = gestor.registrarResponsable(nombre.getText(), correo.getText());
        estado("Responsable registrado: " + r);
    }

    private void nuevaTarea(boolean comoSubtarea) {
        Proyecto p = exigirProyecto();
        String idPadre = null;
        if (comoSubtarea) {
            Tarea sel = exigirSeleccion();
            if (!sel.esCompuesta()) throw new IllegalStateException("'" + sel.getTitulo() + "' es una tarea simple. Crea la tarea como compuesta para poder agregarle subtareas");
            idPadre = sel.getId();
        }
        JTextField titulo = new JTextField();
        JTextField descripcion = new JTextField();
        JComboBox<Prioridad> prioridad = new JComboBox<>(Prioridad.values());
        prioridad.setRenderer(new PrioridadRenderer());
        prioridad.setSelectedItem(Prioridad.MEDIA);
        JTextField fecha = new JTextField();
        JCheckBox compuesta = new JCheckBox("Tendrá subtareas");
        JPanel form = formulario("Título:", titulo, "Descripción:", descripcion, "Prioridad:", prioridad,
                "Fecha límite (dd-mm-aaaa):", fecha, "", compuesta);
        if (JOptionPane.showConfirmDialog(ventana, form, comoSubtarea ? "Nueva subtarea" : "Nueva tarea", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Tarea t = gestor.agregarTarea(p.getId(), idPadre, titulo.getText(), descripcion.getText(),
                (Prioridad) prioridad.getSelectedItem(), leerFecha(fecha.getText()), compuesta.isSelected());
        estado("Creada: " + t.resumen());
    }

    private void cambiarEstado() {
        Tarea t = exigirSeleccion();
        JComboBox<Transicion> combo = new JComboBox<>(Transicion.values());
        combo.setRenderer(new EtiquetaRenderer());
        JPanel form = formulario("Estado actual:", new JLabel(t.getEstado().toString()), "Acción:", combo);
        if (JOptionPane.showConfirmDialog(ventana, form, "Cambiar estado", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        gestor.cambiarEstado(t.getId(), (Transicion) combo.getSelectedItem());
        estado("Nuevo estado de '" + t.getTitulo() + "': " + t.getEstado());
    }

    private void asignarResponsable() {
        Tarea t = exigirSeleccion();
        List<Responsable> disponibles = gestor.responsables();
        if (disponibles.isEmpty()) throw new IllegalStateException("Primero registra un responsable");
        JList<Responsable> lista = new JList<>(disponibles.toArray(new Responsable[0]));
        lista.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lista.setVisibleRowCount(Math.min(7, disponibles.size()));
        for (int i = 0; i < disponibles.size(); i++) {
            if (t.getResponsables().contains(disponibles.get(i))) lista.addSelectionInterval(i, i);
        }
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel("Selecciona uno o varios responsables:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(lista), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(320, 220));
        if (JOptionPane.showConfirmDialog(ventana, panel, "Asignar responsables", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        List<Responsable> seleccionados = lista.getSelectedValuesList();
        if (seleccionados.isEmpty()) throw new IllegalArgumentException("Selecciona al menos un responsable");
        int nuevos = 0;
        for (Responsable responsable : seleccionados) {
            if (!t.getResponsables().contains(responsable)) {
                gestor.asignarResponsable(t.getId(), responsable.getId());
                nuevos++;
            }
        }
        estado(nuevos + " responsable(s) agregado(s) a '" + t.getTitulo() + "'");
    }

    private void cambiarPrioridad() {
        Tarea t = exigirSeleccion();
        JComboBox<Prioridad> combo = new JComboBox<>(Prioridad.values());
        combo.setRenderer(new PrioridadRenderer());
        combo.setSelectedItem(t.getPrioridad());
        if (JOptionPane.showConfirmDialog(ventana, combo, "Nueva prioridad", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        gestor.cambiarPrioridad(t.getId(), (Prioridad) combo.getSelectedItem());
        estado("Prioridad actualizada");
    }

    private void cambiarFecha() {
        Tarea t = exigirSeleccion();
        String texto = JOptionPane.showInputDialog(ventana, "Nueva fecha límite (dd-mm-aaaa, vacío = sin fecha):",
                t.getFechaLimite() == null ? "" : t.getFechaLimite().format(FORMATO_FECHA));
        if (texto == null) return;
        gestor.cambiarFechaLimite(t.getId(), leerFecha(texto));
        estado("Fecha límite actualizada");
    }

    private void eliminar() {
        Tarea t = exigirSeleccion();
        String detalle = t.totalTareas() > 1 ? " junto con sus " + (t.totalTareas() - 1) + " subtarea(s)" : "";
        if (JOptionPane.showConfirmDialog(ventana, "¿Eliminar '" + t.getTitulo() + "'" + detalle + "?\n(Podrás deshacerlo)", "Eliminar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        gestor.eliminarTarea(t.getId());
        estado("Eliminada: " + t.getTitulo());
    }

    private void deshacer() { estado("Deshecho: " + gestor.deshacer()); }

    private void rehacer() { estado("Rehecho: " + gestor.rehacer()); }

    private void guardarPlantilla() {
        Tarea t = exigirSeleccion();
        String nombre = JOptionPane.showInputDialog(ventana, "Nombre de la plantilla (incluye las subtareas de '" + t.getTitulo() + "'):");
        if (nombre == null) return;
        var p = gestor.guardarComoPlantilla(t.getId(), nombre);
        estado("Plantilla '" + p.getNombre() + "' guardada con " + p.totalTareas() + " tarea(s)");
    }

    private void desdePlantilla() {
        Proyecto p = exigirProyecto();
        List<String> nombres = gestor.nombresPlantillas();
        if (nombres.isEmpty()) throw new IllegalStateException("No hay plantillas todavía. Guarda una con 'Guardar como plantilla'");
        JComboBox<String> combo = new JComboBox<>(nombres.toArray(new String[0]));
        JTextField fecha = new JTextField(gestor.hoy().format(FORMATO_FECHA));
        JPanel form = formulario("Plantilla:", combo, "Nueva fecha base (dd-mm-aaaa):", fecha);
        if (JOptionPane.showConfirmDialog(ventana, form, "Crear desde plantilla", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Tarea sel = seleccionada();
        String idPadre = (sel != null && sel.esCompuesta()) ? sel.getId() : null;
        LocalDate base = leerFecha(fecha.getText());
        Tarea creada = gestor.crearDesdePlantilla((String) combo.getSelectedItem(), p.getId(), idPadre, base == null ? gestor.hoy() : base);
        estado("Creada desde plantilla: " + creada.getTitulo() + (idPadre == null ? "" : " (dentro de '" + sel.getTitulo() + "')"));
    }

    private void guardarPlantillaProyecto() {
        Proyecto proyecto = exigirProyecto();
        if (proyecto.getRaices().isEmpty()) throw new IllegalStateException("Agrega tareas al proyecto antes de guardarlo como plantilla");
        String nombre = JOptionPane.showInputDialog(ventana, "Nombre para la plantilla del proyecto:", proyecto.getNombre());
        if (nombre == null) return;
        var plantilla = gestor.guardarProyectoComoPlantilla(proyecto.getId(), nombre);
        estado("Plantilla de proyecto guardada: " + plantilla.getNombre() + " (" + plantilla.totalTareas() + " tareas)");
    }

    private void usarPlantillaProyecto() {
        List<String> nombres = gestor.nombresPlantillasProyecto();
        if (nombres.isEmpty()) throw new IllegalStateException("Todavía no hay plantillas de proyectos. Usa 'Guardar proyecto'");
        JComboBox<String> plantilla = new JComboBox<>(nombres.toArray(new String[0]));
        JTextField nombre = new JTextField("Nuevo proyecto");
        JTextField fecha = new JTextField(gestor.hoy().format(FORMATO_FECHA));
        JPanel form = formulario("Plantilla:", plantilla, "Nombre del nuevo proyecto:", nombre,
                "Fecha base (dd-mm-aaaa):", fecha);
        if (JOptionPane.showConfirmDialog(ventana, form, "Crear proyecto desde plantilla", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        LocalDate fechaBase = leerFecha(fecha.getText());
        Proyecto creado = gestor.crearProyectoDesdePlantilla((String) plantilla.getSelectedItem(), nombre.getText(), fechaBase == null ? gestor.hoy() : fechaBase);
        recargarProyectos();
        comboProyectos.setSelectedItem(creado);
        estado("Proyecto creado desde plantilla: " + creado.getNombre());
    }

    private void revisarAlertasAutomaticas() {
        List<Alerta> alertas = gestor.revisarRecordatorios();
        if (!alertas.isEmpty() && ventana != null && ventana.isVisible()) {
            String texto = alertas.stream().map(Alerta::toString).reduce("", (a, b) -> a + b + "\n");
            estado(alertas.size() + " nueva(s) alerta(s) de fecha límite");
            JOptionPane.showMessageDialog(ventana, texto, "Recordatorios automáticos", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void consultar() {
        JComboBox<OpcionFiltro> prioridad = comboFiltro("Prioridad", opcionesPrioridad());
        JComboBox<OpcionFiltro> fecha = comboFiltro("Fecha", opcionesFecha());
        JComboBox<OpcionFiltro> responsable = comboFiltro("Responsable", opcionesResponsable());
        JComboBox<OpcionFiltro> estadoTarea = comboFiltro("Estado", opcionesEstado());
        JComboBox<OpcionOrden> orden = new JComboBox<>(opcionesOrden(false).toArray(new OpcionOrden[0]));
        orden.setRenderer(new PrefijoRenderer("Orden"));
        JTextField desde = new JTextField();
        JTextField hasta = new JTextField();
        JCheckBox todosLosProyectos = new JCheckBox("Buscar en todos los proyectos");
        JPanel form = formulario("Prioridad:", prioridad, "Fecha límite:", fecha, "Responsable:", responsable,
                "Estado:", estadoTarea, "Fecha límite desde (dd-mm-aaaa):", desde, "Fecha límite hasta (dd-mm-aaaa):", hasta,
                "Ordenar por:", orden, "", todosLosProyectos);
        if (JOptionPane.showConfirmDialog(ventana, form, "Consultar tareas", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        Filtro filtro = combinar(prioridad, fecha, responsable, estadoTarea);
        LocalDate fechaDesde = leerFecha(desde.getText());
        LocalDate fechaHasta = leerFecha(hasta.getText());
        if (fechaDesde != null || fechaHasta != null) {
            Filtro rango = Filtro.fechaEntre(fechaDesde, fechaHasta);
            filtro = filtro == null ? rango : filtro.y(rango);
        }
        if (filtro == null) filtro = Filtro.todas();
        CriterioOrden criterio = ((OpcionOrden) orden.getSelectedItem()).criterio();

        Proyecto p = (Proyecto) comboProyectos.getSelectedItem();
        String idProyecto = todosLosProyectos.isSelected() || p == null ? null : p.getId();
        List<Tarea> resultado = gestor.consultar(idProyecto, filtro, criterio);
        mostrarTareas("Resultado de la consulta",
                resultado.size() + " tarea(s)  |  filtro: " + filtro.descripcion() + "  |  orden: " + criterio.nombre(), resultado);
    }

    private void recordatorios() {
        String texto = JOptionPane.showInputDialog(ventana, "Fecha a revisar (dd-mm-aaaa):", gestor.hoy().format(FORMATO_FECHA));
        if (texto == null) return;
        LocalDate fecha = leerFecha(texto);
        List<Alerta> alertas = gestor.revisarRecordatorios(fecha == null ? gestor.hoy() : fecha);
        mostrarTexto("Recordatorios", alertas.isEmpty() ? "No hay alertas nuevas." : alertas.stream().map(Alerta::toString).reduce("", (a, b) -> a + b + "\n"));
    }

    private void bandeja() {
        List<Responsable> lista = gestor.responsables();
        if (lista.isEmpty()) throw new IllegalStateException("No hay responsables registrados");
        JComboBox<Responsable> combo = new JComboBox<>(lista.toArray(new Responsable[0]));
        if (JOptionPane.showConfirmDialog(ventana, combo, "¿De quién?", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        List<Alerta> alertas = gestor.bandejaDe(((Responsable) combo.getSelectedItem()).getId());
        mostrarTexto("Bandeja de alertas", alertas.isEmpty() ? "Bandeja vacía." : alertas.stream().map(Alerta::toString).reduce("", (a, b) -> a + b + "\n"));
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = new JButton(texto);
        b.addActionListener(e -> ejecutarAccion(accion));
        return b;
    }

    private void refrescar() {
        sincronizarFiltroResponsables();
        String idSeleccion = seleccionada() == null ? null : seleccionada().getId();
        Proyecto p = (Proyecto) comboProyectos.getSelectedItem();
        Filtro filtro = combinar(filtroPrioridad, filtroFecha, filtroResponsable);
        CriterioOrden orden = ((OpcionOrden) ordenArbol.getSelectedItem()).criterio();
        Set<String> coincidencias = new HashSet<>();
        DefaultMutableTreeNode raiz;
        if (p == null) {
            raiz = new DefaultMutableTreeNode("Sin proyectos: crea uno con 'Nuevo proyecto'");
        } else {
            String texto = String.format("%s - progreso %.0f%%", p.getNombre(), p.progreso() * 100);
            raiz = new DefaultMutableTreeNode(texto);
            Filtro aplicado = filtro == null ? Filtro.todas() : filtro;
            for (Tarea t : ordenar(p.getRaices(), orden)) construirNodos(raiz, t, aplicado, orden, coincidencias);
            if (filtro != null) {
                raiz.setUserObject(texto + String.format("  -  filtro activo: %d de %d tarea(s)",
                        coincidencias.size(), p.todasLasTareas().size()));
            }
        }
        renderizador.setCoincidencias(filtro == null ? null : coincidencias);
        modeloArbol.setRoot(raiz);
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);
        if (idSeleccion != null) seleccionarPorId(raiz, idSeleccion);

        botonDeshacer.setEnabled(gestor.puedeDeshacer());
        botonRehacer.setEnabled(gestor.puedeRehacer());
        botonDeshacer.setToolTipText(gestor.proximaADeshacer());
        botonRehacer.setToolTipText(gestor.proximaARehacer());
    }

    private boolean construirNodos(DefaultMutableTreeNode padre, Tarea t, Filtro filtro, CriterioOrden orden, Set<String> coincidencias) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(t);
        boolean algunaHija = false;
        for (Tarea h : ordenar(t.getHijos(), orden)) {
            algunaHija |= construirNodos(nodo, h, filtro, orden, coincidencias);
        }
        boolean coincide = filtro.cumple(t);
        if (coincide) coincidencias.add(t.getId());
        if (coincide || algunaHija) padre.add(nodo);
        return coincide || algunaHija;
    }

    private List<Tarea> ordenar(List<Tarea> tareas, CriterioOrden orden) {
        return orden == null ? tareas : tareas.stream().sorted(orden).toList();
    }
    @SafeVarargs
    private Filtro combinar(JComboBox<OpcionFiltro>... combos) {
        Filtro resultado = null;
        for (JComboBox<OpcionFiltro> combo : combos) {
            OpcionFiltro opcion = (OpcionFiltro) combo.getSelectedItem();
            if (opcion == null || opcion.filtro() == null) continue;
            Filtro f = opcion.filtro().get();
            resultado = resultado == null ? f : resultado.y(f);
        }
        return resultado;
    }

    private List<OpcionFiltro> opcionesPrioridad() {
        List<OpcionFiltro> lista = new ArrayList<>();
        lista.add(new OpcionFiltro("Todas", null, null));
        Prioridad[] deMayorAMenor = {Prioridad.CRITICA, Prioridad.ALTA, Prioridad.MEDIA, Prioridad.BAJA};
        for (Prioridad pr : deMayorAMenor) {
            lista.add(new OpcionFiltro(Tema.etiquetaPrioridad(pr), () -> Filtro.porPrioridad(pr), Tema.colorPrioridad(pr)));
        }
        lista.add(new OpcionFiltro("Alta o superior", () -> Filtro.prioridadMinima(Prioridad.ALTA), Tema.colorPrioridad(Prioridad.ALTA)));
        return lista;
    }

    private List<OpcionFiltro> opcionesFecha() {
        return List.of(
                new OpcionFiltro("Todas", null, null),
                new OpcionFiltro("Vencidas", () -> Filtro.vencidas(gestor.hoy()), null),
                new OpcionFiltro("Vencen hoy", () -> Filtro.venceHoy(gestor.hoy()), null),
                new OpcionFiltro("Próximos 7 días", () -> Filtro.venceEnLosProximos(gestor.hoy(), 7), null),
                new OpcionFiltro("Próximos 30 días", () -> Filtro.venceEnLosProximos(gestor.hoy(), 30), null),
                new OpcionFiltro("Sin fecha límite", Filtro::sinFecha, null));
    }

    private List<OpcionFiltro> opcionesResponsable() {
        List<OpcionFiltro> lista = new ArrayList<>();
        lista.add(new OpcionFiltro("Todos", null, null));
        for (Responsable r : gestor.responsables()) lista.add(new OpcionFiltro(r.getNombre(), () -> Filtro.porResponsable(r), null));
        lista.add(new OpcionFiltro("Sin responsable", Filtro::sinResponsable, null));
        return lista;
    }

    private List<OpcionFiltro> opcionesEstado() {
        List<OpcionFiltro> lista = new ArrayList<>();
        lista.add(new OpcionFiltro("Todos", null, null));
        for (Estado e : Estado.values()) lista.add(new OpcionFiltro(e.toString(), () -> Filtro.porEstado(e), null));
        return lista;
    }

    private List<OpcionOrden> opcionesOrden(boolean incluirOriginal) {
        List<OpcionOrden> lista = new ArrayList<>();
        if (incluirOriginal) lista.add(new OpcionOrden("Original (como se crearon)", null));
        for (CriterioOrden c : List.of(CriterioOrden.porPrioridad(), CriterioOrden.porPrioridadAscendente(),
                CriterioOrden.porFechaLimite(), CriterioOrden.porFechaLimiteDescendente(),
                CriterioOrden.porResponsable(), CriterioOrden.porResponsableDescendente(),
                CriterioOrden.porEstado(), CriterioOrden.porTitulo())) {
            lista.add(new OpcionOrden(c.nombre(), c));
        }
        return lista;
    }
    
    private void sincronizarFiltroResponsables() {
        if (filtroResponsable == null || filtroResponsable.getItemCount() == gestor.responsables().size() + 2) return;
        OpcionFiltro previa = (OpcionFiltro) filtroResponsable.getSelectedItem();
        cargando = true;
        try {
            filtroResponsable.removeAllItems();
            List<OpcionFiltro> nuevas = opcionesResponsable();
            nuevas.forEach(filtroResponsable::addItem);
            for (OpcionFiltro o : nuevas) {
                if (previa != null && o.texto().equals(previa.texto())) { filtroResponsable.setSelectedItem(o); break; }
            }
        } finally {
            cargando = false;
        }
    }

    private void limpiarFiltros() {
        cargando = true;
        try {
            filtroPrioridad.setSelectedIndex(0);
            filtroFecha.setSelectedIndex(0);
            filtroResponsable.setSelectedIndex(0);
            ordenArbol.setSelectedIndex(0);
        } finally {
            cargando = false;
        }
        estado("Filtros y orden restablecidos");
    }

    private void seleccionarPorId(DefaultMutableTreeNode raiz, String id) {
        Enumeration<?> nodos = raiz.preorderEnumeration();
        while (nodos.hasMoreElements()) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) nodos.nextElement();
            if (n.getUserObject() instanceof Tarea t && t.getId().equals(id)) {
                arbol.setSelectionPath(new TreePath(n.getPath()));
                return;
            }
        }
    }

    private void recargarProyectos() {
        Proyecto seleccionado = (Proyecto) comboProyectos.getSelectedItem();
        cargando = true;
        try {
            comboProyectos.removeAllItems();
            List<Proyecto> lista = gestor.proyectos();
            lista.forEach(comboProyectos::addItem);
            if (seleccionado != null && lista.contains(seleccionado)) comboProyectos.setSelectedItem(seleccionado);
            else if (!lista.isEmpty()) comboProyectos.setSelectedIndex(0);
        } finally {
            cargando = false;
        }
    }

    private Tarea seleccionada() {
        TreePath ruta = arbol == null ? null : arbol.getSelectionPath();
        if (ruta == null) return null;
        Object o = ((DefaultMutableTreeNode) ruta.getLastPathComponent()).getUserObject();
        return o instanceof Tarea t ? t : null;
    }

    private Tarea exigirSeleccion() {
        Tarea t = seleccionada();
        if (t == null) throw new IllegalStateException("Selecciona primero una tarea en el árbol");
        return t;
    }

    private Proyecto exigirProyecto() {
        Proyecto p = (Proyecto) comboProyectos.getSelectedItem();
        if (p == null) throw new IllegalStateException("Primero crea un proyecto");
        return p;
    }

    private LocalDate leerFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim(), FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha no válida (usa dd-mm-aaaa): " + texto);
        }
    }

    private void estado(String mensaje) { barraEstado.setText("●  " + mensaje); }

    private void mostrarTexto(String titulo, String texto) {
        JTextArea area = new JTextArea(texto, 14, 70);
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(700, 280));
        JOptionPane.showMessageDialog(ventana, scroll, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarTareas(String titulo, String encabezado, List<Tarea> tareas) {
        JTextPane panel = new JTextPane();
        panel.setEditable(false);
        panel.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 13));
        StyledDocument doc = panel.getStyledDocument();
        try {
            SimpleAttributeSet normal = new SimpleAttributeSet();
            StyleConstants.setForeground(normal, Tema.TEXTO);
            doc.insertString(doc.getLength(), encabezado + "\n\n", normal);
            for (Tarea t : tareas) {
                SimpleAttributeSet estilo = new SimpleAttributeSet();
                StyleConstants.setForeground(estilo, Tema.colorPrioridad(t.getPrioridad()));
                doc.insertString(doc.getLength(), t.resumen() + "\n", estilo);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        panel.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setPreferredSize(new Dimension(760, 300));
        JOptionPane.showMessageDialog(ventana, scroll, titulo, JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel formulario(Object... pares) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        for (Object o : pares) {
            panel.add(o instanceof String s ? new JLabel(s) : (Component) o);
        }
        return panel;
    }

    private record OpcionFiltro(String texto, Supplier<Filtro> filtro, Color color) {
        @Override public String toString() { return texto; }
    }

    private record OpcionOrden(String texto, CriterioOrden criterio) {
        @Override public String toString() { return texto; }
    }

    private static class PrefijoRenderer extends javax.swing.DefaultListCellRenderer {
        private final String prefijo;

        PrefijoRenderer(String prefijo) { this.prefijo = prefijo; }

        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor, int i, boolean sel, boolean foco) {
            Component c = super.getListCellRendererComponent(lista, valor, i, sel, foco);
            if (i < 0) setText(prefijo + ": " + valor);
            if (valor instanceof OpcionFiltro o && o.color() != null && !sel) setForeground(o.color());
            return c;
        }
    }

    private static class PrioridadRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor, int i, boolean sel, boolean foco) {
            Component c = super.getListCellRendererComponent(lista, valor, i, sel, foco);
            if (valor instanceof Prioridad p) {
                setText(Tema.etiquetaPrioridad(p));
                if (!sel) setForeground(Tema.colorPrioridad(p));
            }
            return c;
        }
    }

    private static class EtiquetaRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> lista, Object valor, int i, boolean sel, boolean foco) {
            Component c = super.getListCellRendererComponent(lista, valor, i, sel, foco);
            if (valor instanceof Transicion t) setText(t.etiqueta());
            return c;
        }
    }

    private static class RenderizadorTareas extends DefaultTreeCellRenderer {
        private final Icon carpetaCerrada = Tema.carpeta(false);
        private final Icon carpetaAbierta = Tema.carpeta(true);
        private final Icon iconoHoja = Tema.documento();
        private Set<String> coincidencias;

        RenderizadorTareas() {
            setBackgroundNonSelectionColor(Tema.PANEL);
            setBackgroundSelectionColor(Tema.SELECCION);
            setBorderSelectionColor(new Color(120, 120, 120));
        }

        void setCoincidencias(Set<String> coincidencias) { this.coincidencias = coincidencias; }

        @Override
        public Component getTreeCellRendererComponent(JTree arbol, Object valor, boolean sel, boolean exp, boolean hoja, int fila, boolean foco) {
            Component c = super.getTreeCellRendererComponent(arbol, valor, sel, exp, hoja, fila, foco);
            Object o = ((DefaultMutableTreeNode) valor).getUserObject();
            setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            if (o instanceof Tarea t) {
                setText("  " + t.resumen());
                setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));
                setIcon(t.esCompuesta() ? (exp ? carpetaAbierta : carpetaCerrada) : iconoHoja);
                Color color = Tema.colorPrioridad(t.getPrioridad());
                if (coincidencias != null && !coincidencias.contains(t.getId())) color = Tema.mezclar(color, Tema.PANEL, 0.55f);
                setForeground(color);
            } else {
                setText("  " + o);
                setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 13));
                setIcon(exp ? carpetaAbierta : carpetaCerrada);
                setForeground(Tema.TEXTO);
            }
            return c;
        }
    }
}
