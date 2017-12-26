// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.Logging;

public final class PluginVerifier {
    private PluginVerifier() {
        // Hide default constructor for utils classes
    }

    public static class RelevantCodeSigners {
        final public Set<CodeSigner> authorizedSigners = new HashSet<CodeSigner>();

        /**
         * CodeSigner(s) whose certificates couldn't be traced back to our TrustAnchor found on JarEntrys
         * which had no otherwise authorized signers for them
         */
        final public Set<CodeSigner> unverifiedSigners = new HashSet<CodeSigner>();

        /** CodeSigner(s) whose certificates could be traced back to our TrustAnchor but didn't have the
         * correct plugin-name permissions, found on JarEntrys which had no otherwise authorized signers
         * for them
         */
        final public Set<CodeSigner> unauthorizedSigners = new HashSet<CodeSigner>();

        public boolean passedValidation() {
            return this.unverifiedSigners.isEmpty() && this.unauthorizedSigners.isEmpty();
        }

        @Override
        public String toString() {
            return String.format(
                "authorizedSigners: %s\nunverifiedSigners: %s\nunauthorizedSigners: %s",
                this.authorizedSigners,
                this.unverifiedSigners,
                this.unauthorizedSigners
            );
        }
    }

    public static RelevantCodeSigners verifyJarFile(final File file, final String pluginName) throws
        NoSuchAlgorithmException,
        InvalidAlgorithmParameterException,
        IOException,
        CertificateException {
        final InputStream rootCertificateStream = PluginVerifier.class.getResourceAsStream("rootCertificate.pem");
        try {
            return verifyJarFile(
                file,
                pluginName,
                (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(
                    rootCertificateStream
                )
            );
        } finally {
            rootCertificateStream.close();
        }
    }

    public static RelevantCodeSigners verifyJarFile(
        final File file,
        final String pluginName,
        final X509Certificate rootCertificate
    ) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException, CertificateException {
        final JarFile jarFile = new JarFile(file, true);
        try {
            final byte[] readBuffer = new byte[1024];

            final RelevantCodeSigners jarSigners = new RelevantCodeSigners();

            final CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            final TrustAnchor trustAnchor = new TrustAnchor(rootCertificate, null);
            final PKIXParameters pkixParameters = new PKIXParameters(Collections.singleton(trustAnchor));
            pkixParameters.setRevocationEnabled(false);

            for (JarEntry jarEntry : Collections.list(jarFile.entries())) {
                final String entryName = jarEntry.getName();
                if (entryName.endsWith("/") || entryName.startsWith("META-INF/"))
                    // don't expect these to be inclued in signed portion
                    continue;

                final InputStream entryInputStream = jarFile.getInputStream(jarEntry);
                // read to end of stream
                while (entryInputStream.read(readBuffer) != -1) {}

                final CodeSigner[] codeSigners = jarEntry.getCodeSigners();
                if (codeSigners == null || codeSigners.length == 0)
                    // if we'd need to accept unsigned entries to allow this jar there's no point continuing
                    // because we'd already be at the bottom rung of permissiveness
                    return null;

                final Set<CodeSigner> entryUnverifiedSigners = new HashSet<CodeSigner>();
                final Set<CodeSigner> entryUnauthorizedSigners = new HashSet<CodeSigner>();

                boolean foundAuthorized = false;
                for (CodeSigner codeSigner : codeSigners) {
                    try {
                        validator.validate(codeSigner.getSignerCertPath(), pkixParameters);
                    } catch (CertPathValidatorException e) {
                        Logging.warn(String.format(
                            "Validating plugin %s, entry %s failed: %s",
                            pluginName,
                            jarEntry.getName(),
                            e.getMessage()
                        ));
                        entryUnverifiedSigners.add(codeSigner);
                        continue;
                    }

                    final Collection<List<?>> sans = ((X509Certificate) codeSigner.getSignerCertPath().getCertificates().get(0)).getSubjectAlternativeNames();
                    if (sans != null) {
                        final String targetURI = "http://josm.openstreetmap.de/plugin/" + pluginName;

                        for (List<?> san : sans) {
                            if (((Integer) san.get(0) == 6) && ((String) san.get(1)).equals(targetURI)) {
                                jarSigners.authorizedSigners.add(codeSigner);
                                foundAuthorized = true;
                                break;
                            }
                        }
                    }

                    if (foundAuthorized)
                        break;

                    entryUnauthorizedSigners.add(codeSigner);
                }
                if (foundAuthorized)
                    // don't bother turning over entryUnauthorizedSigners, entryUnverifiedSigners to the 
                    // jar-wide sets as it's not relevant - we wouldn't need to trust these signers to
                    // safely continue
                    continue;

                // copy this entry's unauthorized and unverified signers to jar-wide Sets as at least one of
                // them would have to be trusted as an exception were we to allow this jar
                jarSigners.unauthorizedSigners.addAll(entryUnauthorizedSigners);
                jarSigners.unverifiedSigners.addAll(entryUnverifiedSigners);
            }
            return jarSigners;
        } finally {
            jarFile.close();
        }
    }
}
