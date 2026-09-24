package tw.nekomimi.nekogram.passkey;

import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;

public class WebAuthnCrypto {

    private final static byte[] AAGUID = new byte[]{
            (byte) 0x4a, (byte) 0x27, (byte) 0x57, (byte) 0x9c,
            (byte) 0x63, (byte) 0x56, (byte) 0x48, (byte) 0x4f,
            (byte) 0xa3, (byte) 0x83, (byte) 0x66, (byte) 0x1b,
            (byte) 0x0f, (byte) 0x60, (byte) 0xfb, (byte) 0x34
    };

    public static KeyPair generateP256KeyPair() throws NoSuchAlgorithmException {
        var keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        return keyGen.generateKeyPair();
    }

    /**
     * Builds standard COSE_Key format using co.nstant.in:cbor
     * Key mappings:
     * 1 (kty)  -> 2 (EC2)
     * 3 (alg)  -> -7 (ES256)
     * -1 (crv)  -> 1 (P-256)
     * -2 (x)    -> 32-byte binary
     * -3 (y)    -> 32-byte binary
     */
    public static byte[] encodeCosePublicKey(ECPublicKey pubKey) throws CborException {
        var point = pubKey.getW();
        var x = toFixedLength(point.getAffineX().toByteArray(), 32);
        var y = toFixedLength(point.getAffineY().toByteArray(), 32);

        var baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                // Positive keys first for canonical order
                .put(1, 2)    // kty: EC2
                .put(3, -7)   // alg: ES256
                // Negative keys
                .put(-1, 1)   // crv: P-256
                .put(-2, x)   // x-coordinate
                .put(-3, y)   // y-coordinate
                .end()
                .build()
        );
        return baos.toByteArray();
    }

    /**
     * Packages the WebAuthn attestationObject CBOR map:
     * {
     * "fmt": "none",
     * "attStmt": {},
     * "authData": h'...'
     * }
     */
    public static byte[] buildAttestationObject(byte[] authData) throws CborException {
        var baos = new ByteArrayOutputStream();
        new CborEncoder(baos).encode(new CborBuilder()
                .addMap()
                .put("fmt", "none")
                .putMap("attStmt").end() // empty map
                .put("authData", authData)
                .end()
                .build()
        );
        return baos.toByteArray();
    }

    /**
     * Constructs raw binary WebAuthn Authenticator Data.
     * Flags: Bit 0 = UP (0x01), Bit 2 = UV (0x04), Bit 6 = AT (0x40)
     */
    public static byte[] buildAuthenticatorData(String rpId, boolean userVerified, int signCount, byte[] credentialId, ECPublicKey credentialPublicKey) throws Exception {
        var rpIdHash = Utilities.computeSHA256(rpId.getBytes(StandardCharsets.UTF_8));

        byte flags = 0x01; // UP
        if (userVerified) flags |= 0x04; // UV

        var attestedCredData = new byte[0];
        if (credentialId != null && credentialPublicKey != null) {
            flags |= 0x40; // AT
            var coseKey = encodeCosePublicKey(credentialPublicKey);

            var credBuffer = ByteBuffer.allocate(16 + 2 + credentialId.length + coseKey.length);
            credBuffer.put(AAGUID);
            credBuffer.putShort((short) credentialId.length);
            credBuffer.put(credentialId);
            credBuffer.put(coseKey);
            attestedCredData = credBuffer.array();
        }

        var authDataBuffer = ByteBuffer.allocate(32 + 1 + 4 + attestedCredData.length);
        authDataBuffer.put(rpIdHash);
        authDataBuffer.put(flags);
        authDataBuffer.putInt(signCount);
        if (attestedCredData.length > 0) {
            authDataBuffer.put(attestedCredData);
        }

        return authDataBuffer.array();
    }

    /**
     * Signs (authData || sha256(clientDataJSON)) using the private EC key.
     */
    public static byte[] signAssertion(byte[] privateKeyBytes, byte[] authData, byte[] clientDataJson) throws Exception {
        var clientDataHash = Utilities.computeSHA256(clientDataJson);
        var signatureBase = ByteBuffer.allocate(authData.length + clientDataHash.length)
                .put(authData)
                .put(clientDataHash)
                .array();

        var signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(decodePrivateKey(privateKeyBytes));
        signature.update(signatureBase);
        return signature.sign(); // DER encoded ASN.1 sequence
    }

    private static byte[] toFixedLength(byte[] val, int length) {
        if (val.length == length) return val;
        var fixed = new byte[length];
        if (val.length > length) {
            System.arraycopy(val, val.length - length, fixed, 0, length);
        } else {
            System.arraycopy(val, 0, fixed, length - val.length, length - val.length);
        }
        return fixed;
    }

    private static PrivateKey decodePrivateKey(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var kf = KeyFactory.getInstance("EC");
        var ks = new PKCS8EncodedKeySpec(bytes);
        return kf.generatePrivate(ks);
    }
}