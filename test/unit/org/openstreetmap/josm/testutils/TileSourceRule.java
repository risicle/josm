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

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;


public class TileSourceRule extends WireMockRule {
    private static class ByteArrayWrapper{
        // i don't believe you're making me do this, java
        public final byte[] byteArray;

        public ByteArrayWrapper(byte[] ba) {
            this.byteArray = ba;
        }
    }

    public static HashMap<ConstSource, ByteArrayWrapper> constPayloadCache = new HashMap<>();

    public static abstract class ConstSource {
        public abstract byte[] generatePayloadBytes();
        public abstract MappingBuilder getMappingBuilder();
        public abstract String getLabel();
        public abstract ImageryInfo getImageryInfo(int port);

        public byte[] getPayloadBytes() {
            ByteArrayWrapper payloadWrapper = constPayloadCache.get(this);
            if (payloadWrapper == null) {
                payloadWrapper = new ByteArrayWrapper(this.generatePayloadBytes());
                constPayloadCache.put(this, payloadWrapper);
            }
            return payloadWrapper.byteArray;
        }

        public ResponseDefinitionBuilder getResponseDefinitionBuilder() {
            return WireMock.aResponse().withStatus(200).withHeader("Content-Type", "image/png").withBody(
                this.getPayloadBytes()
            );
        }
    }

    public static class ColorSource extends ConstSource {
        protected final Color color;
        protected final String label;
        protected final int tileSize;

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
                String.format("tms[20]:http://localhost:%d/%h", port, this.hashCode()),
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

    private final List<ConstSource> sourcesList;
    private final boolean clearLayerList;
    private final boolean registerInLayerList;

    public TileSourceRule(ConstSource... sources) {
        this(false, false, sources);
    }

    public TileSourceRule(boolean clearLayerList, boolean registerInLayerList, ConstSource... sources) {
        super(options().dynamicPort());
        this.clearLayerList = clearLayerList;
        this.registerInLayerList = registerInLayerList;
        this.sourcesList = Collections.unmodifiableList(Arrays.asList(sources));
        for (ConstSource source : this.sourcesList) {
            this.stubFor(source.getMappingBuilder().willReturn(source.getResponseDefinitionBuilder()));
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Statement statement = base;
        if (this.registerInLayerList || this.clearLayerList) {
            statement = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (TileSourceRule.this.clearLayerList) {
                        ImageryLayerInfo.instance.clear();
                    }
                    if (TileSourceRule.this.registerInLayerList) {
                        for (ConstSource source : TileSourceRule.this.sourcesList){
                            ImageryLayerInfo.addLayer(source.getImageryInfo(TileSourceRule.this.port()));
                        }
                    }
                    base.evaluate();
                }
            };
        }
        return super.apply(statement, description);
    }
}
