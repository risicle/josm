// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.ExtendedDialogMocker;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.google.common.io.CharStreams;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * Unit tests of {@link PluginHandler} class.
 */
public class PluginHandlerUpdatePluginsTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().main().assumeRevision(
        "Revision: 6000\n"
    );

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
        options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot())
    );

    @Before
    public void setUp() throws Exception {
        try (
            FileReader pluginListReader = new FileReader(
                new File(TestUtils.getTestDataRoot(), "plugin/remotePluginsList")
            );
        ) {
            final String pluginList = String.format(
                CharStreams.toString(pluginListReader),
                this.wireMockRule.port()
            );

            this.wireMockRule.stubFor(
                WireMock.get(WireMock.urlEqualTo("/plugins")).willReturn(
                    WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/plain")
                    .withBody(pluginList)
                )
            );
            ImmutableList.of("dummy_plugin", "baz_plugin").forEach(plugin_name -> this.wireMockRule.stubFor(
                WireMock.get(WireMock.urlEqualTo("/plugin/" + plugin_name + ".jar")).willReturn(
                    WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/java-archive")
                    .withBodyFile("plugin/" + plugin_name + ".jar")
                )
            ));
        }
    }

    /**
     * ---
     */
    @Test
    public void testUpdatePlugins() {
        new ExtendedDialogMocker(ImmutableMap.<String, Object>builder()
            .put("JOSM version 8,001 required for plugin baz_plugin.", "Download Plugin")
            .put("JOSM version 7,001 required for plugin dummy_plugin.", "Download Plugin")
            .build()
        );

        Config.getPref().putList("plugins", ImmutableList.of("dummy_plugin", "baz_plugin"));
        Config.getPref().putList("pluginmanager.sites",
            ImmutableList.of(String.format("http://localhost:%s/plugins", this.wireMockRule.port()))
        );

        final ArrayList<PluginInformation> updatedPlugins = new ArrayList<>(PluginHandler.updatePlugins(Main.parent, null, null, false));

        updatedPlugins.sort((a, b) -> a.name.compareTo(b.name));

        assertEquals(2, updatedPlugins.size());

        assertEquals(updatedPlugins.get(0).name, "baz_plugin");
        assertEquals("7", updatedPlugins.get(0).localversion);

        assertEquals(updatedPlugins.get(1).name, "dummy_plugin");
        assertEquals("31772", updatedPlugins.get(1).localversion);

        this.wireMockRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugins")));
        this.wireMockRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/dummy_plugin.jar")));
        this.wireMockRule.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/plugin/baz_plugin.jar")));
    }
}
