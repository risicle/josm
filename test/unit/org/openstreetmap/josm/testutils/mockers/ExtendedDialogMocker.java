// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static org.junit.Assert.fail;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.Logging;

import mockit.Deencapsulation;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp for {@link ExtendedDialog} allowing a test to pre-seed uses of {@link ExtendedDialog}
 * with mock "responses". This works best with {@link ExtendedDialog}s which have their contents set
 * through {@link ExtendedDialog#setContent(String)} as simple strings. In such a case, responses can
 * be defined through a mapping from content {@link String}s to button indexes ({@link Integer}s) or
 * button names ({@link String}s). Example:
 *
 * <pre>
 *      new ExtendedDialogMocker(ImmutableMap.<String, Object>builder()
 *          .put("JOSM version 8,001 required for plugin baz_plugin.", "Download Plugin")
 *          .put("JOSM version 7,001 required for plugin dummy_plugin.", "Cancel")
 *          .put("Are you sure you want to do foo bar?", ExtendedDialog.DialogClosedOtherwise)
 *          .build()
 *      );
 * </pre>
 * 
 * Testing examples with more complicated contents would require overriding
 * {@link #getMockResult(ExtendedDialog)} with custom logic.
 *
 * The default {@link #getMockResult(ExtendedDialog)} will raise an
 * {@link junit.framework.AssertionFailedError} on an {@link ExtendedDialog} activation without a
 * matching mapping entry or if the named button doesn't exist.
 *
 * {@link #simpleStringContentMockResultMap} is exposed as a public field to allow for situations
 * where the desired result might need to be changed mid-test.
 */
public class ExtendedDialogMocker extends MockUp<ExtendedDialog> {
    /**
     * Because we're unable to add fields to the mocked class, we need to use this external global
     * mapping to be able to keep a note of the most recently set simple String contents of each
     * {@link ExtendedDialog} instance - {@link ExtendedDialog} doesn't store this information 
     * itself, instead converting it directly into the embedded {@link Component}.
     */
    protected final Map<ExtendedDialog, String> simpleStringContentMemo = new WeakHashMap<ExtendedDialog, String>();

    /**
     * Mapping to {@link Object}s so response button can be specified by String (label) or Integer -
     * sorry, no type safety as java doesn't support union types
     */
    final Map<String, Object> simpleStringContentMockResultMap;

    /**
     * Construct an {@link ExtendedDialogMocker} with an empty {@link #simpleStringContentMockResultMap}.
     */
    public ExtendedDialogMocker() {
        this(new HashMap<String, Object>());
    }

    /**
     * Construct an {@link ExtendedDialogMocker} with the provided {@link #simpleStringContentMockResultMap}.
     * @param simpleStringContentMockResultMap mapping of {@link ExtendedDialog} string contents to
     *      result button label or integer index.
     */
    public ExtendedDialogMocker(final Map<String, Object> simpleStringContentMockResultMap) {
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        this.simpleStringContentMockResultMap = simpleStringContentMockResultMap;
    }

    protected String getContentDescription(final String stringContent) {
        return Optional.ofNullable(stringContent)
            .map(sc -> "\""+sc+"\"")
            .orElse("[no content or content non-simple]");
    }

    protected int getMockResult(final ExtendedDialog instance) {
        final String stringContent = this.simpleStringContentMemo.get(instance);
        final Object result = this.simpleStringContentMockResultMap.get(stringContent);

        if (result == null) {
            fail(
                "Unexpected ExtendedDialog content: " + this.getContentDescription(stringContent)
            );
        } else if (result instanceof Integer) {
            return (Integer) result;
        } else if (result instanceof String) {
            final String[] bTexts = Deencapsulation.getField(instance, "bTexts");
            final int position = Arrays.asList(bTexts).indexOf((String) result);
            if (position == -1) {
                fail("Unable to find button labeled \"" + result + "\". Instead found: " + Arrays.toString(bTexts));
            }
            // buttons are numbered with 1-based indexing
            return 1 + position;
        }

        throw new IllegalArgumentException(
            "ExtendedDialog contents mapped to unsupported type of Object: " + result
        );
    }

    @Mock
    private void setupDialog(final Invocation invocation) {
        if (!GraphicsEnvironment.isHeadless()) {
            invocation.proceed();
        }
        // else do nothing - WindowMocker-ed Windows doesn't work well enough for some of the
        // component constructions
    }

    @Mock
    private void setVisible(final Invocation invocation, final boolean value) {
        if (value == true) {
            final int mockResult = this.getMockResult((ExtendedDialog) invocation.getInvokedInstance());
            Deencapsulation.setField((ExtendedDialog) invocation.getInvokedInstance(), "result", mockResult);
            Logging.info(
                "ExtendedDialogMocker answering {0} to ExtendedDialog with content {1}",
                mockResult,
                this.getContentDescription(this.simpleStringContentMemo.get(invocation.getInvokedInstance()))
            );
        }
    }

    @Mock
    private ExtendedDialog setContent(final Invocation invocation, final String message) {
        final ExtendedDialog retval = invocation.proceed(message);
        // must set this *after* the regular invocation else that will fall through to
        // setContent(Component, boolean) which would overwrite it (with null)
        this.simpleStringContentMemo.put((ExtendedDialog) invocation.getInvokedInstance(), message);
        return retval;
    }

    @Mock
    private ExtendedDialog setContent(final Invocation invocation, final Component content, final boolean placeContentInScrollPane) {
        this.simpleStringContentMemo.put((ExtendedDialog) invocation.getInvokedInstance(), null);
        return invocation.proceed(content, placeContentInScrollPane);
    }
}
