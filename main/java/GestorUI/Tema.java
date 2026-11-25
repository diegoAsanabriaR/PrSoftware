package GestorUI;

import Gestor.Tarea.Prioridad;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

final class Tema {
    private Tema() { }

    // ---------------- paleta ----------------
    static final Color FONDO = new Color(10, 10, 10);           
    static final Color PANEL = new Color(22, 22, 22);            
    static final Color BORDE = new Color(58, 58, 58);
    static final Color TEXTO = new Color(235, 235, 235);
    static final Color TEXTO_SUAVE = new Color(150, 150, 150);
    static final Color SELECCION = new Color(62, 62, 62);

    static final Color BOTON = new Color(84, 84, 84);           
    static final Color BOTON_PRINCIPAL = new Color(122, 122, 122);
    static final Color BOTON_SOBRE = new Color(108, 108, 108);  
    static final Color BOTON_BORDE = new Color(130, 130, 130);

    static final Color CARPETA = new Color(226, 208, 170);      
    static final Color CARPETA_BORDE = new Color(168, 148, 108);

    static Color colorPrioridad(Prioridad p) {
        return switch (p) {
            case CRITICA -> new Color(240, 68, 56);   
            case ALTA -> new Color(255, 152, 0);      
            case MEDIA -> new Color(255, 214, 10);    
            case BAJA -> new Color(144, 205, 250);    
        };
    }

    static String etiquetaPrioridad(Prioridad p) {
        return switch (p) {
            case CRITICA -> "Crítica";
            case ALTA -> "Alta";
            case MEDIA -> "Media";
            case BAJA -> "Baja";
        };
    }
    
    static Color mezclar(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static void aplicar() {
        poner("Panel.background", PANEL);
        poner("Panel.foreground", TEXTO);
        poner("OptionPane.background", PANEL);
        poner("OptionPane.messageForeground", TEXTO);
        poner("OptionPane.foreground", TEXTO);
        poner("Label.foreground", TEXTO);
        poner("Label.disabledForeground", TEXTO_SUAVE);
        poner("CheckBox.background", PANEL);
        poner("CheckBox.foreground", TEXTO);

        poner("Button.background", BOTON);
        poner("Button.foreground", TEXTO);
        poner("Button.select", new Color(66, 66, 66));
        poner("Button.focus", new Color(190, 190, 190));
        poner("Button.disabledText", new Color(130, 130, 130));
        poner("Button.disabledForeground", new Color(130, 130, 130));
        UIManager.put("Button.gradient", null);

        for (String campo : new String[]{"TextField", "TextArea", "TextPane", "PasswordField", "FormattedTextField"}) {
            poner(campo + ".background", new Color(36, 36, 36));
            poner(campo + ".foreground", TEXTO);
            poner(campo + ".caretForeground", TEXTO);
            poner(campo + ".selectionBackground", new Color(110, 110, 110));
            poner(campo + ".selectionForeground", Color.WHITE);
            poner(campo + ".inactiveForeground", TEXTO_SUAVE);
        }

        poner("ComboBox.background", new Color(52, 52, 52));
        poner("ComboBox.foreground", TEXTO);
        poner("ComboBox.selectionBackground", new Color(110, 110, 110));
        poner("ComboBox.selectionForeground", Color.WHITE);
        poner("ComboBox.buttonBackground", new Color(84, 84, 84));
        poner("ComboBox.buttonShadow", new Color(70, 70, 70));
        poner("ComboBox.buttonDarkShadow", new Color(160, 160, 160));
        poner("ComboBox.buttonHighlight", new Color(100, 100, 100));
        poner("ComboBox.disabledBackground", new Color(40, 40, 40));
        poner("ComboBox.disabledForeground", TEXTO_SUAVE);

        poner("List.background", new Color(30, 30, 30));
        poner("List.foreground", TEXTO);
        poner("List.selectionBackground", new Color(110, 110, 110));
        poner("List.selectionForeground", Color.WHITE);

        poner("ScrollPane.background", PANEL);
        poner("Viewport.background", PANEL);
        poner("Viewport.foreground", TEXTO);
        poner("ScrollBar.background", PANEL);
        poner("ScrollBar.thumb", new Color(100, 100, 100));
        poner("ScrollBar.thumbShadow", new Color(70, 70, 70));
        poner("ScrollBar.thumbHighlight", new Color(130, 130, 130));
        poner("ScrollBar.track", new Color(30, 30, 30));
        poner("ScrollBar.trackHighlight", new Color(30, 30, 30));
        UIManager.put("ScrollBar.gradient", null);

        poner("Tree.background", PANEL);
        poner("Tree.foreground", TEXTO);
        poner("Tree.textBackground", PANEL);
        poner("Tree.textForeground", TEXTO);
        poner("Tree.selectionBackground", SELECCION);
        poner("Tree.selectionForeground", TEXTO);
        poner("Tree.hash", new Color(80, 80, 80));
        poner("Tree.line", new Color(80, 80, 80));
        UIManager.put("Tree.expandedIcon", flecha(true));
        UIManager.put("Tree.collapsedIcon", flecha(false));

        poner("ToolTip.background", new Color(50, 50, 50));
        poner("ToolTip.foreground", TEXTO);
        poner("Separator.foreground", BORDE);
    }

    private static void poner(String clave, Color color) { UIManager.put(clave, color); }
    static Icon carpeta(boolean abierta) {
        return new Icono(20, 16) {
            @Override void dibujar(Graphics2D g) {
                Color relleno = abierta ? CARPETA.brighter() : CARPETA;
                Path2D atras = new Path2D.Float();
                atras.moveTo(1, 3); atras.lineTo(7, 3); atras.lineTo(9, 5); atras.lineTo(18, 5); atras.lineTo(18, 14); atras.lineTo(1, 14);
                atras.closePath();
                g.setColor(CARPETA_BORDE);
                g.fill(atras);
                if (abierta) {
                    Path2D frente = new Path2D.Float();
                    frente.moveTo(4, 7); frente.lineTo(20, 7); frente.lineTo(17, 14); frente.lineTo(1, 14);
                    frente.closePath();
                    g.setColor(relleno);
                    g.fill(frente);
                } else {
                    g.setColor(relleno);
                    g.fill(new RoundRectangle2D.Float(1, 6, 17, 8, 2, 2));
                }
            }
        };
    }
    static Icon documento() {
        return new Icono(14, 16) {
            @Override void dibujar(Graphics2D g) {
                Path2D hoja = new Path2D.Float();
                hoja.moveTo(2, 1); hoja.lineTo(8, 1); hoja.lineTo(12, 5); hoja.lineTo(12, 14); hoja.lineTo(2, 14);
                hoja.closePath();
                g.setColor(new Color(170, 170, 170));
                g.fill(hoja);
                g.setColor(new Color(110, 110, 110));
                g.fillPolygon(new int[]{8, 12, 8}, new int[]{1, 5, 5}, 3);
                g.setColor(new Color(90, 90, 90));
                g.setStroke(new BasicStroke(1f));
                g.drawLine(4, 8, 10, 8);
                g.drawLine(4, 10, 10, 10);
            }
        };
    }
    static Icon flecha(boolean expandida) {
        return new Icono(12, 12) {
            @Override void dibujar(Graphics2D g) {
                g.setColor(new Color(190, 190, 190));
                if (expandida) g.fillPolygon(new int[]{2, 10, 6}, new int[]{4, 4, 9}, 3);
                else g.fillPolygon(new int[]{4, 4, 9}, new int[]{2, 10, 6}, 3);
            }
        };
    }

    private abstract static class Icono implements Icon {
        private final int ancho, alto;

        Icono(int ancho, int alto) { this.ancho = ancho; this.alto = alto; }

        abstract void dibujar(Graphics2D g);

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            dibujar(g2);
            g2.dispose();
        }

        @Override public int getIconWidth() { return ancho; }
        @Override public int getIconHeight() { return alto; }
    }
}
