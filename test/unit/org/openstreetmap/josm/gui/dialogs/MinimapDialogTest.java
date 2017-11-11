// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.bbox.SourceButton;
import org.openstreetmap.josm.testutils.JOSMTestRules;

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
    public JOSMTestRules josmTestRules = new JOSMTestRules().main().platform().projection().fakeImagery();

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

    private static void assertSingleSelectedSourceLabel(JPopupMenu menu, String label) {
        boolean found = false;
        for (Component c: menu.getComponents()) {
            if (JPopupMenu.Separator.class.isInstance(c)) {
                break;
            } else {
                boolean equalText = ((JMenuItem) c).getText() == label;
                boolean isSelected = ((JMenuItem) c).isSelected();
                assertEquals(equalText, isSelected);
                if (equalText) {
                    assertFalse("Second selected source found", found);
                    found = true;
                }
            }
        }
        assertTrue("Selected source not found in menu", found);
    }

    private static JMenuItem getSourceMenuItemByLabel(JPopupMenu menu, String label) {
        for (Component c: menu.getComponents()) {
            if (JPopupMenu.Separator.class.isInstance(c)) {
                break;
            } else if (((JMenuItem) c).getText() == label) {
                return (JMenuItem) c;
            }
            // else continue...
        }
        fail("Failed to find menu item with label " + label);
        return null;
    }

    protected MinimapDialog minimap;
    protected SlippyMapBBoxChooser slippyMap;
    protected SourceButton sourceButton;

    protected void setUpMiniMap() throws Exception {
        this.minimap = new MinimapDialog();
        this.minimap.setSize(300, 200);
        this.minimap.showDialog();
        this.slippyMap = (SlippyMapBBoxChooser) TestUtils.getPrivateField(this.minimap, "slippyMap");
        this.sourceButton = (SourceButton) TestUtils.getPrivateField(this.slippyMap, "iSourceButton");

        // get dlg in a paintable state
        this.minimap.addNotify();
        this.minimap.doLayout();
    }

    /**
     * Tests to switch imagery source.
     * @throws Exception if any error occurs
     */
    @Test
    public void testSourceSwitching() throws Exception {
        // relevant prefs starting out empty, should choose the first source and have shown download area enabled
        // (not that there's a data layer for it to use)

        this.setUpMiniMap();

        BufferedImage image = new BufferedImage(
            this.slippyMap.getSize().width,
            this.slippyMap.getSize().height,
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = image.createGraphics();
        // an initial paint operation is required to trigger the tile fetches
        this.slippyMap.paintAll(g);
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        this.slippyMap.paintAll(g);

        assertEquals(0xffffffff, image.getRGB(0, 0));

        assertSingleSelectedSourceLabel(this.sourceButton.getPopupMenu(), "White Tiles");

        getSourceMenuItemByLabel(this.sourceButton.getPopupMenu(), "Magenta Tiles").doClick();
        assertSingleSelectedSourceLabel(this.sourceButton.getPopupMenu(), "Magenta Tiles");
        // call paint to trigger new tile fetch
        this.slippyMap.paintAll(g);

        // clear background to a recognizably "wrong" color & dispose our Graphics2D so we don't risk carrying over
        // any state
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        this.slippyMap.paintAll(g);

        assertEquals(0xffff00ff, image.getRGB(0, 0));

        getSourceMenuItemByLabel(this.sourceButton.getPopupMenu(), "Green Tiles").doClick();
        assertSingleSelectedSourceLabel(this.sourceButton.getPopupMenu(), "Green Tiles");
        // call paint to trigger new tile fetch
        this.slippyMap.paintAll(g);

        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        this.slippyMap.paintAll(g);

        assertEquals(0xff00ff00, image.getRGB(0, 0));

        assertEquals("Green Tiles", Main.pref.get("slippy_map_chooser.mapstyle", "Fail"));
    }

    @Test
    public void testSourcePrefObeyed() throws Exception {
        Main.pref.put("slippy_map_chooser.mapstyle", "Green Tiles");

        this.setUpMiniMap();

        assertSingleSelectedSourceLabel(this.sourceButton.getPopupMenu(), "Green Tiles");

        BufferedImage image = new BufferedImage(
            this.slippyMap.getSize().width,
            this.slippyMap.getSize().height,
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = image.createGraphics();
        // an initial paint operation is required to trigger the tile fetches
        this.slippyMap.paintAll(g);
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        this.slippyMap.paintAll(g);

        assertEquals(0xff00ff00, image.getRGB(0, 0));

        getSourceMenuItemByLabel(this.sourceButton.getPopupMenu(), "Magenta Tiles").doClick();
        assertSingleSelectedSourceLabel(this.sourceButton.getPopupMenu(), "Magenta Tiles");

        assertEquals("Magenta Tiles", Main.pref.get("slippy_map_chooser.mapstyle", "Fail"));
    }

    @Test
    public void testSourcePrefInvalid() throws Exception {
        Main.pref.put("slippy_map_chooser.mapstyle", "Hooloovoo Tiles");

        this.setUpMiniMap();

        assertSingleSelectedSourceLabel(this.sourceButton.getPopupMenu(), "White Tiles");

        BufferedImage image = new BufferedImage(
            this.slippyMap.getSize().width,
            this.slippyMap.getSize().height,
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = image.createGraphics();
        // an initial paint operation is required to trigger the tile fetches
        this.slippyMap.paintAll(g);
        g.setBackground(Color.BLUE);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        Thread.sleep(500);

        g = image.createGraphics();
        this.slippyMap.paintAll(g);

        assertEquals(0xffffffff, image.getRGB(0, 0));
    }
}
