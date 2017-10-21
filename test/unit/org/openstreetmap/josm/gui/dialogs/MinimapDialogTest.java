// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.gui.MainApplication;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MinimapDialog} class.
 */
public class MinimapDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().commands().platform().projection().fakeImagery();

    /**
     * Unit test of {@link MinimapDialog} class.
     */
    @Test
    public void testMinimapDialog() {
        MinimapDialog dlg = new MinimapDialog();
        dlg.showDialog();
        assertTrue(dlg.isVisible());
        dlg.hideDialog();
        assertFalse(dlg.isVisible());
    }

    @Test
    public void testPaint() throws InterruptedException {
        MinimapDialog dlg = new MinimapDialog();
        dlg.setSize(300, 200);
        dlg.invalidate();
        dlg.showDialog();

        BufferedImage image = new BufferedImage(
            dlg.getSize().width,
            dlg.getSize().height,
            BufferedImage.TYPE_INT_RGB
        );

        Thread.sleep(1000);

        Graphics2D g = image.createGraphics();
        dlg.paint(g);

        try {
            javax.imageio.ImageIO.write(image, "png", new java.io.File("painted.png"));
        } catch (java.io.IOException ioe) {
            System.err.println("Failed writing image");
        }
    }
}
