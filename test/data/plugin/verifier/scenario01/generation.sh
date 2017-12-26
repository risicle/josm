# A root keypair & certificate is generated. The "extension" parameters are
# specified here because at the top level they need to get applied to the
# root key's self-signed certificate
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias root -dname "cn=JOSM" -ext bc:c=ca:true,pathlen:1 -ext ku:c=digitalSignature,nonRepudiation,keyCertSign,cRLSign -keyalg EC

# Core Developer A generates their keypair and certificate signing request,
# no extended parameters as those are decided by the certificate issuer,
# which in this case will be root.
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias core-a -dname "cn=Core Developer A" -keyalg EC
keytool -certreq -storepass banana -keypass banana -keystore credentials/keystore.jks -alias core-a -file credentials/core-a-certreq.csr

# root signs Core Developer A's key, granting it CA abilities but not the
# ability to create further CAs (pathlen:0)
keytool -gencert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias root -infile credentials/core-a-certreq.csr -outfile credentials/core-a-cert.der -ext bc:c=ca:true,pathlen:0 -ext ku:c=digitalSignature,nonRepudiation,keyCertSign -ext eku=codeSigning

# Core Developer A imports that back into their keychain
keytool -importcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias core-a -file credentials/core-a-cert.der

# Plugin Author B generates their keypair and certificate signing request
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-b -dname "cn=Author B" -keyalg EC
keytool -certreq -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-b -file credentials/author-b-certreq.csr

# Core Developer A signs this generating a certificate. The extension params
# include the plugin name(s) Author B is permitted to sign against encoded as
# URI subjectAlternativeNames. These are effectively arbitrary URIs where we
# can choose our own convention
keytool -gencert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias core-a -infile credentials/author-b-certreq.csr -outfile credentials/author-b-cert.der -ext san=uri:http://josm.openstreetmap.de/plugin/foo,uri:http://josm.openstreetmap.de/plugin/bar,uri:http://josm.openstreetmap.de/plugin/baz -ext bc=ca:false -ext ku:c=digitalSignature,nonRepudiation -ext eku:c=codeSigning

# Author B imports this certificate back into their keychain
keytool -importcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-b -file credentials/author-b-cert.der

# The root public certificate is exported and shipped with the verification code...
keytool -exportcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias root -file credentials/rootCertificate.pem


# Author X generates their key
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-x -dname "cn=Author X" -keyalg EC
keytool -certreq -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-x -file credentials/author-x-certreq.csr

# Author B attempts to sign key for Author X
keytool -gencert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-b -infile credentials/author-x-certreq.csr -outfile credentials/author-x-cert.der -ext san=uri:http://josm.openstreetmap.de/plugin/foo,uri:http://josm.openstreetmap.de/plugin/bar,uri:http://josm.openstreetmap.de/plugin/baz -ext bc=ca:false -ext ku:c=digitalSignature,nonRepudiation -ext eku:c=codeSigning
keytool -importcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-x -file credentials/author-x-cert.der


# Author C generates their keypair
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-c -dname "cn=Author C" -keyalg EC
keytool -certreq -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-c -file credentials/author-c-certreq.csr

# Core Developer A mistakenly issues a CA certificate to Author C
keytool -gencert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias core-a -infile credentials/author-c-certreq.csr -outfile credentials/author-c-cert.der -ext san=uri:http://josm.openstreetmap.de/plugin/foo,uri:http://josm.openstreetmap.de/plugin/bar,uri:http://josm.openstreetmap.de/plugin/baz -ext bc:c=ca:true,pathlen:0 -ext ku:c=digitalSignature,nonRepudiation,keyCertSign -ext eku=codeSigning

# Author C imports certificate back into their keychain
keytool -importcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-c -file credentials/author-c-cert.der


# Author Y generates their keypair
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-y -dname "cn=Author Y" -keyalg EC
keytool -certreq -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-y -file credentials/author-y-certreq.csr

# Author C attempts to sign key for Author Y
keytool -gencert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-c -infile credentials/author-y-certreq.csr -outfile credentials/author-y-cert.der -ext san=uri:http://josm.openstreetmap.de/plugin/foo,uri:http://josm.openstreetmap.de/plugin/bar,uri:http://josm.openstreetmap.de/plugin/baz -ext bc=ca:false -ext ku:c=digitalSignature,nonRepudiation -ext eku:c=codeSigning

# Author Y imports certificate back into their keychain
keytool -importcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-y -file credentials/author-y-cert.der


# Author Z uses a self-signed certificate
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias root -dname "cn=Author Z" -ext san=uri:http://josm.openstreetmap.de/plugin/foo,uri:http://josm.openstreetmap.de/plugin/bar,uri:http://josm.openstreetmap.de/plugin/baz -ext bc=ca:false -ext ku:c=digitalSignature,nonRepudiation -ext eku:c=codeSigning -keyalg EC


# Author D generates their keypair
keytool -genkeypair -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-d -dname "cn=Author D" -keyalg EC
keytool -certreq -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-d -file credentials/author-d-certreq.csr

# Core Developer A signs this generating a certificate.
keytool -gencert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias core-a -infile credentials/author-d-certreq.csr -outfile credentials/author-d-cert.der -ext san=uri:http://josm.openstreetmap.de/plugin/foo,uri:http://josm.openstreetmap.de/plugin/bar,uri:http://josm.openstreetmap.de/plugin/fuz -ext bc=ca:false -ext ku:c=digitalSignature,nonRepudiation -ext eku:c=codeSigning

# Author D imports certificate back into their keychain
keytool -importcert -storepass banana -keypass banana -keystore credentials/keystore.jks -alias author-d -file credentials/author-d-cert.der
