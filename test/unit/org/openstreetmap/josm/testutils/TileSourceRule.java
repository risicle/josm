// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.tools.Logging;

import static org.openstreetmap.josm.TestUtils.getPrivateStaticField;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;


/**
 * A JUnit rule, based on {@link WireMockRule} to provide a test with a simple mock tile server serving multiple tile
 * sources.
 */
public class TileSourceRule extends WireMockRule {
    private static class ByteArrayWrapper{
        // i don't believe you're making me do this, java
        public final byte[] byteArray;

        public ByteArrayWrapper(byte[] ba) {
            this.byteArray = ba;
        }
    }

    // allocation is expensive and many tests may be wanting to set up the same tile sources one after the other, hence
    // this cache
    public static HashMap<ConstSource, ByteArrayWrapper> constPayloadCache = new HashMap<>();

    /**
     * Class defining a tile source for TileSourceRule to mock. Due to the way WireMock is designed, it is far more
     * straightforward to serve a single image in all tile positions 
     */
    public static abstract class ConstSource {
        /**
         * method for actually generating the payload body bytes, uncached
         */
        public abstract byte[] generatePayloadBytes();

        /**
         * @return a {@link MappingBuilder} representing the request matching properties of this tile source, suitable
         * for passing to {@link WireMockRule#stubFor}.
         */
        public abstract MappingBuilder getMappingBuilder();

        /**
         * @return text label/name for this source if displayed in JOSM menus
         */
        public abstract String getLabel();

        /**
         * @param port the port this WireMock server is running on
         * @return {@link ImageryInfo} describing this tile source, as might be submitted to {@link ImageryLayerInfo#add}
         */
        public abstract ImageryInfo getImageryInfo(int port);

        /**
         * @return byte array of the payload body for this source, possibly retrieved from a global cache
         */
        public byte[] getPayloadBytes() {
            ByteArrayWrapper payloadWrapper = constPayloadCache.get(this);
            if (payloadWrapper == null) {
                payloadWrapper = new ByteArrayWrapper(this.generatePayloadBytes());
                constPayloadCache.put(this, payloadWrapper);
            }
            return payloadWrapper.byteArray;
        }

        /**
         * @return a {@link ResponseDefinitionBuilder} embodying the payload of this tile source suitable for
         * application to a {@link MappingBuilder}.
         */
        public ResponseDefinitionBuilder getResponseDefinitionBuilder() {
            return WireMock.aResponse().withStatus(200).withHeader("Content-Type", "image/png").withBody(
                this.getPayloadBytes()
            );
        }
    }

    /**
     * A plain color tile source
     */
    public static class ColorSource extends ConstSource {
        protected final Color color;
        protected final String label;
        protected final int tileSize;

        /**
         * @param color Color for these tiles
         * @param label text label/name for this source if displayed in JOSM menus
         * @param tileSize Pixel dimension of tiles (usually 256)
         */
        public ColorSource(Color color, String label, int tileSize) {
            this.color = color;
            this.label = label;
            this.tileSize = tileSize;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.color, this.label, this.tileSize, this.getClass());
        }

        @Override
        public byte[] generatePayloadBytes() {
            BufferedImage image = new BufferedImage(this.tileSize, this.tileSize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setBackground(this.color);
            g.clearRect(0, 0, image.getWidth(), image.getHeight());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                ImageIO.write(image, "png", outputStream);
            } catch (IOException e) {
                // I don't see how this would be possible writing to a ByteArrayOutputStream
            }
            return outputStream.toByteArray();
        }

        @Override
        public MappingBuilder getMappingBuilder() {
            return WireMock.get(WireMock.urlMatching(String.format("/%h/(\\d+)/(\\d+)/(\\d+)\\.png", this.hashCode())));
        }

        @Override
        public ImageryInfo getImageryInfo(int port) {
            return new ImageryInfo(
                this.label,
                String.format("tms[20]:http://localhost:%d/%h/{z}/{x}/{y}.png", port, this.hashCode()),
                "tms",
                (String)null,
                (String)null
            );
        }

        @Override
        public String getLabel() {
            return this.label;
        }
    }

    public final List<ConstSource> sourcesList;
    public final boolean clearLayerList;
    public final boolean clearSlippyMapSources;
    public final boolean registerInLayerList;

    /**
     * Construct a TileSourceRule for use with a JUnit test.
     *
     * This variant will not make any attempt to register the sources' existence with any JOSM subsystems, so is safe
     * for direct application to a JUnit test.
     *
     * @param sources tile sources to serve from this mock server
     */
    public TileSourceRule(ConstSource... sources) {
        this(false, false, false, sources);
    }

    /**
     * Construct a TileSourceRule for use with a JUnit test.
     *
     * The three boolean parameters control whether to perform various steps registering the tile sources with parts
     * of JOSM's internals as part of the setup process. It is advised to only enable any of these if it can be ensured
     * that this rule will have its setup routine executed *after* the relevant parts of JOSM have been set up, e.g.
     * when handled by {@link org.openstreetmap.josm.testutils.JOSMTestRules#fakeImagery}.
     *
     * @param clearLayerList whether to clear ImageryLayerInfo's layer list of any pre-existing entries
     * @param clearSlippyMapSources whether to clear SlippyMapBBoxChooser's stubborn fallback Mapnik TileSource
     * @param registerInLayerList whether to add sources to ImageryLayerInfo's layer list
     * @param sources tile sources to serve from this mock server
     */
    public TileSourceRule(
        boolean clearLayerList,
        boolean clearSlippyMapSources,
        boolean registerInLayerList,
        ConstSource... sources
    ) {
        super(options().dynamicPort());
        this.clearLayerList = clearLayerList;
        this.clearSlippyMapSources = clearSlippyMapSources;
        this.registerInLayerList = registerInLayerList;
        this.sourcesList = Collections.unmodifiableList(Arrays.asList(sources));
        for (ConstSource source : this.sourcesList) {
            this.stubFor(source.getMappingBuilder().willReturn(source.getResponseDefinitionBuilder()));
        }
    }

    /**
     * A junit-rule {@code apply} method exposed separately to allow a chaining rule to put this much earlier in
     * the test's initialization routine. The idea being to allow WireMock's web server to be starting up while other
     * necessary initialization is taking place.
     * See {@link org.junit.rules.TestRule#apply} for arguments.
     */
    public Statement applyRunServer(Statement base, Description description) {
        return super.apply(base, description);
    }

    /**
     * A junit-rule {@code apply} method exposed separately, containing initialization steps which can only be performed
     * once more of josm's environment has been set up.
     * See {@link org.junit.rules.TestRule#apply} for arguments.
     */
    public Statement applyRegisterLayers(Statement base, Description description) {
        if (this.registerInLayerList || this.clearLayerList) {
            return new Statement() {
                @Override
                @SuppressWarnings("unchecked")
                public void evaluate() throws Throwable {
                    List<SlippyMapBBoxChooser.TileSourceProvider> slippyMapProviders = null;
                    SlippyMapBBoxChooser.TileSourceProvider slippyMapDefaultProvider = null;
                    List<ImageryInfo> originalImageryInfoList = null;
                    if (TileSourceRule.this.clearSlippyMapSources) {
                        try {
                            slippyMapProviders = (List<SlippyMapBBoxChooser.TileSourceProvider>)getPrivateStaticField(
                                SlippyMapBBoxChooser.class,
                                "providers"
                            );
                            // pop this off the beginning of the list, keep for later
                            slippyMapDefaultProvider = slippyMapProviders.remove(0);
                        } catch (ReflectiveOperationException e) {
                            Logging.warn("Failed to remove default SlippyMapBBoxChooser TileSourceProvider");
                        }
                    }

                    if (TileSourceRule.this.clearLayerList) {
                        originalImageryInfoList = ImageryLayerInfo.instance.getLayers();
                        ImageryLayerInfo.instance.clear();
                    }
                    if (TileSourceRule.this.registerInLayerList) {
                        for (ConstSource source : TileSourceRule.this.sourcesList){
                            ImageryLayerInfo.addLayer(source.getImageryInfo(TileSourceRule.this.port()));
                        }
                    }

                    try {
                        base.evaluate();
                    } finally {
                        // clean up to original state
                        if (slippyMapDefaultProvider != null) {
                            slippyMapProviders.add(0, slippyMapDefaultProvider);
                        }
                        if (originalImageryInfoList != null) {
                            ImageryLayerInfo.instance.clear();
                            ImageryLayerInfo.addLayers(originalImageryInfoList);
                        }
                    }
                }
            };
        } else {
            return base;
        }
    }

    /**
     * A standard implementation of apply which simply calls both sub- {@code apply} methods, {@link #applyRunServer}
     * and {@link applyRegisterLayers}. Called when used as a standard junit rule.
     */
    @Override
    public Statement apply(Statement base, Description description) {
        return this.applyRunServer(this.applyRegisterLayers(base, description), description);
    }
}
