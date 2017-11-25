// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilities to aid in making assertions about images using regular expressions.
 */
public final class ImagePatternMatching {
    private ImagePatternMatching() {}

    private static final Map<String, Pattern> patternCache = new HashMap<String, Pattern>();

    private static Matcher imageStripPatternMatchInner(
        final BufferedImage image,
        final int columnOrRowIndex,
        IntFunction<String> paletteMapFn,
        final Map<Integer, String> paletteMap,
        Pattern pattern,
        final String patternString,
        final boolean isColumn,
        final boolean assertMatch
    ) {
        paletteMapFn = Optional.ofNullable(paletteMapFn)
            // using "#" as the default "unmapped" character as it can be used in regexes without escaping
            .orElse(i -> paletteMap.getOrDefault(i, "#"));
        pattern = Optional.ofNullable(pattern)
            .orElseGet(() -> patternCache.computeIfAbsent(patternString, k -> Pattern.compile(k)));

        int[] columnOrRow = isColumn
            ? image.getRGB(columnOrRowIndex, 0, 1, image.getHeight(), null, 0, 1)
            : image.getRGB(0, columnOrRowIndex, image.getWidth(), 1, null, 0, image.getWidth());

        String stringRepr = Arrays.stream(columnOrRow).mapToObj(paletteMapFn).collect(Collectors.joining());
        Matcher result = pattern.matcher(stringRepr);

        if (assertMatch && !result.matches()) {
            System.err.println(String.format("Full strip failing to match pattern %s: %s", pattern, stringRepr));
            fail(String.format(
                "%s %d failed to match pattern %s",
                isColumn ? "Column" : "Row",
                columnOrRowIndex,
                pattern
            ));
        }

        return result;
    }

    public static Matcher columnMatch(
        final BufferedImage image,
        final int rowNumber,
        final Map<Integer, String> paletteMap,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            null,
            paletteMap,
            null,
            patternString,
            true,
            true
        );
    }

    public static Matcher columnMatch(
        final BufferedImage image,
        final int rowNumber,
        final IntFunction<String> paletteMapFn,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            paletteMapFn,
            null,
            null,
            patternString,
            true,
            true
        );
    }

    public static Matcher columnMatch(
        final BufferedImage image,
        final int rowNumber,
        final Map<Integer, String> paletteMap,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            null,
            paletteMap,
            pattern,
            null,
            true,
            true
        );
    }

    public static Matcher columnMatch(
        final BufferedImage image,
        final int rowNumber,
        final IntFunction<String> paletteMapFn,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            paletteMapFn,
            null,
            pattern,
            null,
            true,
            true
        );
    }

    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final Map<Integer, String> paletteMap,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            null,
            paletteMap,
            null,
            patternString,
            false,
            true
        );
    }

    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final IntFunction<String> paletteMapFn,
        final String patternString,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            paletteMapFn,
            null,
            null,
            patternString,
            false,
            true
        );
    }

    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final Map<Integer, String> paletteMap,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            null,
            paletteMap,
            pattern,
            null,
            false,
            true
        );
    }

    public static Matcher rowMatch(
        final BufferedImage image,
        final int rowNumber,
        final IntFunction<String> paletteMapFn,
        final Pattern pattern,
        final boolean assertMatch
    ) {
        return imageStripPatternMatchInner(
            image,
            rowNumber,
            paletteMapFn,
            null,
            pattern,
            null,
            false,
            true
        );
    }
}
