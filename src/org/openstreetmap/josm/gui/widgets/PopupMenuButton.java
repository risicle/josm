// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;


import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;


/**
 * Button triggering the appearance of a JPopupMenu when activated
 */
public class PopupMenuButton extends JButton implements ActionListener {
    private JPopupMenu menu;

    /* Pass-throughs to JButton's constructors */

    public PopupMenuButton() {
        super();
        this.initialize();
    }

    public PopupMenuButton(Action a) {
        super(a);
        this.initialize();
    }

    public PopupMenuButton(Icon i) {
        super(i);
        this.initialize();
    }

    public PopupMenuButton(String t, Icon i) {
        super(t, i);
        this.initialize();
    }

    /* Pass-throughs to JButton's constructors with addition of JPopupMenu */

    public PopupMenuButton(JPopupMenu m) {
        super();
        this.initialize(m);
    }

    public PopupMenuButton(Action a, JPopupMenu m) {
        super(a);
        this.initialize(m);
    }

    public PopupMenuButton(Icon i, JPopupMenu m) {
        super(i);
        this.initialize(m);
    }

    public PopupMenuButton(String t, Icon i, JPopupMenu m) {
        super(t, i);
        this.initialize(m);
    }


    private void initialize(JPopupMenu m) {
        this.menu = m;
        this.initialize();
    }

    private void initialize() {
        this.addActionListener(this);
    }


    public JPopupMenu getPopupMenu() {
        return this.menu;
    }

    public void setPopupMenu(JPopupMenu m) {
        this.menu = m;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        this.menu.show(this, 0, this.getHeight());
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D)g;

        //
        // paint small arrow in bottom right corner
        //
        Rectangle clip = g2d.getClipBounds();
        Point br = new Point(clip.x + clip.width, clip.y + clip.height);  // "bottom right"

        Path2D p = new Path2D.Float();
        p.moveTo(br.x - 7, br.y - 4);
        p.lineTo(br.x - 1, br.y - 4);
        p.lineTo(br.x - 4, br.y - 1);
        p.closePath();

        g2d.setPaint(Color.BLACK);
        g2d.fill(p);
    }
}
