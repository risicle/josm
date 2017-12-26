// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.lang.SecurityException;
import java.security.CodeSigner;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.stream.Collectors;


import org.junit.Test;
import com.google.common.collect.Sets;

import org.openstreetmap.josm.TestUtils;

public class PluginVerifierTest {
    protected static X509Certificate scenario01RootCertificate;

    protected static X509Certificate getScenario01RootCertificate() throws Exception {
        if (scenario01RootCertificate == null) {
            final FileInputStream certInputStream = new FileInputStream(
                TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/credentials/rootCertificate.pem"
            );
            try {
                scenario01RootCertificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certInputStream);
            } finally {
                certInputStream.close();
            }
        }
        return scenario01RootCertificate;
    }

    protected static String getCodeSignerName(final CodeSigner codeSigner) {
        return ((X509Certificate)codeSigner.getSignerCertPath().getCertificates().get(0)).getSubjectX500Principal().getName();
    }

    @Test
    public void testSimpleValidSignature() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "baz",
            rootCertificate
        );

        assertTrue(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.unverifiedSigners.isEmpty());
        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.authorizedSigners.size());
        assertEquals(
            "CN=Author B",
            getCodeSignerName(relevantCodeSigners.authorizedSigners.iterator().next())
        );
    }

    @Test
    public void testSimpleUnauthorizedSignature() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "boz",  // not an allowed plugin name for the CodeSigner's certificate
            rootCertificate
        );

        assertFalse(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.unverifiedSigners.isEmpty());
        assertTrue(relevantCodeSigners.authorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.unauthorizedSigners.size());
        assertEquals(
            "CN=Author B",
            getCodeSignerName(relevantCodeSigners.unauthorizedSigners.iterator().next())
        );
    }

    @Test
    public void testSimpleUnsignedJar() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.unsigned.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "baz",
            rootCertificate
        );

        assertNull(relevantCodeSigners);
    }

    @Test
    public void testSimpleUnverifiedSigner() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.author-z.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "baz",
            rootCertificate
        );

        assertFalse(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.authorizedSigners.isEmpty());
        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.unverifiedSigners.size());
        assertEquals(
            "CN=Author Z",
            getCodeSignerName(relevantCodeSigners.unverifiedSigners.iterator().next())
        );
    }

    @Test
    public void testSignerCertSignedByNonCA() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.author-x.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "baz",
            rootCertificate
        );

        assertFalse(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.authorizedSigners.isEmpty());
        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.unverifiedSigners.size());
        assertEquals(
            "CN=Author X",
            getCodeSignerName(relevantCodeSigners.unverifiedSigners.iterator().next())
        );
    }

    @Test
    public void testSignerCertPathTooLong() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.author-y.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "baz",
            rootCertificate
        );

        assertFalse(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.authorizedSigners.isEmpty());
        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.unverifiedSigners.size());
        assertEquals(
            "CN=Author Y",
            getCodeSignerName(relevantCodeSigners.unverifiedSigners.iterator().next())
        );
    }

    @Test
    public void testSimpleDoubleSigned() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/baz.author-b.author-y.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "baz",
            rootCertificate
        );

        assertTrue(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.unverifiedSigners.isEmpty());
        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.authorizedSigners.size());
        assertEquals(
            "CN=Author B",
            getCodeSignerName(relevantCodeSigners.authorizedSigners.iterator().next())
        );
    }

    @Test(expected=SecurityException.class)
    public void testIncorrectSignature() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/bar.author-b.incorrect-sig.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "bar",
            rootCertificate
        );
    }

    @Test
    public void testJarPartlyUnsigned() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/bar.author-b.unsigned-entry.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "bar",
            rootCertificate
        );

        assertNull(relevantCodeSigners);
    }

    @Test
    public void testJarPartlySignedUnverified() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/bar.author-b.entry-author-z.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "bar",
            rootCertificate
        );

        assertFalse(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());

        assertEquals(1, relevantCodeSigners.authorizedSigners.size());
        assertEquals(
            "CN=Author B",
            getCodeSignerName(relevantCodeSigners.authorizedSigners.iterator().next())
        );

        assertEquals(1, relevantCodeSigners.unverifiedSigners.size());
        assertEquals(
            "CN=Author Z",
            getCodeSignerName(relevantCodeSigners.unverifiedSigners.iterator().next())
        );
    }

    @Test
    public void testJarSignedByTrustedUnionValid() throws Exception {
        final File plugin = new File(TestUtils.getTestDataRoot() + "plugin/verifier/scenario01/bar.author-b.author-d.union.jar");
        final X509Certificate rootCertificate = getScenario01RootCertificate();
        final PluginVerifier.RelevantCodeSigners relevantCodeSigners = PluginVerifier.verifyJarFile(
            plugin,
            "bar",
            rootCertificate
        );

        assertTrue(relevantCodeSigners.passedValidation());

        assertTrue(relevantCodeSigners.unauthorizedSigners.isEmpty());
        assertTrue(relevantCodeSigners.unverifiedSigners.isEmpty());

        assertEquals(2, relevantCodeSigners.authorizedSigners.size());
        assertEquals(
            Sets.newHashSet("CN=Author B", "CN=Author D"),
            relevantCodeSigners.authorizedSigners.stream().map(
                PluginVerifierTest::getCodeSignerName
            ).collect(
                Collectors.toCollection(HashSet::new)
            )
        );
    }
}
