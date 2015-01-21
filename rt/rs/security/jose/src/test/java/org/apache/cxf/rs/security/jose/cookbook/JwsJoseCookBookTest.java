/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.jose.cookbook;

import java.io.InputStream;
import java.security.Security;
import java.util.List;

import javax.crypto.Cipher;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.provider.json.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.jwa.Algorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.EcDsaJwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProtectedHeader;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JwsJoseCookBookTest {
    private static final String PAYLOAD = "It’s a dangerous business, Frodo, going out your door. "
        + "You step onto the road, and if you don't keep your feet, "
        + "there’s no knowing where you might be swept off to.";
    private static final String ENCODED_PAYLOAD = "SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywgZ29pbmcgb3V0IH"
        + "lvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9hZCwgYW5kIGlmIHlvdSBk"
        + "b24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXigJlzIG5vIGtub3dpbmcgd2hlcm"
        + "UgeW91IG1pZ2h0IGJlIHN3ZXB0IG9mZiB0by4";
    private static final String RSA_KID_VALUE = "bilbo.baggins@hobbiton.example";
    private static final String RSA_V1_5_SIGNATURE_PROTECTED_HEADER =
          "eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX"
        + "hhbXBsZSJ9";
    private static final String RSA_V1_5_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"RS256\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replaceAll(" ", "");
    private static final String RSA_V1_5_SIGNATURE_VALUE =
          "MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHoxnW2e5CZ5NlKtainoFmK"
        + "ZopdHM1O2U4mwzJdQx996ivp83xuglII7PNDi84wnB-BDkoBwA78185hX-Es4J"
        + "IwmDLJK3lfWRa-XtL0RnltuYv746iYTh_qHRD68BNt1uSNCrUCTJDt5aAE6x8w"
        + "W1Kt9eRo4QPocSadnHXFxnt8Is9UzpERV0ePPQdLuW3IS_de3xyIrDaLGdjluP"
        + "xUAhb6L2aXic1U12podGU0KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJushZ41Axf_f"
        + "cIe8u9ipH84ogoree7vjbU5y18kDquDg";
    private static final String RSA_V1_5_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2"
        + "dpbnNAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHo"
        + "xnW2e5CZ5NlKtainoFmKZopdHM1O2U4mwzJdQx996ivp83xuglII"
        + "7PNDi84wnB-BDkoBwA78185hX-Es4JIwmDLJK3lfWRa-XtL0Rnlt"
        + "uYv746iYTh_qHRD68BNt1uSNCrUCTJDt5aAE6x8wW1Kt9eRo4QPo"
        + "cSadnHXFxnt8Is9UzpERV0ePPQdLuW3IS_de3xyIrDaLGdjluPxU"
        + "Ahb6L2aXic1U12podGU0KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJush"
        + "Z41Axf_fcIe8u9ipH84ogoree7vjbU5y18kDquDg\""
        + "}"
        + "]"
        + "}").replaceAll(" ", "");
    private static final String RSA_V1_5_JSON_FLATTENED_SERIALIZATION =  ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"protected\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6ImJpbGJvLmJhZ2dpbn"
        + "NAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"MRjdkly7_-oTPTS3AXP41iQIGKa80A0ZmTuV5MEaHoxnW2"
        + "e5CZ5NlKtainoFmKZopdHM1O2U4mwzJdQx996ivp83xuglII7PNDi84w"
        + "nB-BDkoBwA78185hX-Es4JIwmDLJK3lfWRa-XtL0RnltuYv746iYTh_q"
        + "HRD68BNt1uSNCrUCTJDt5aAE6x8wW1Kt9eRo4QPocSadnHXFxnt8Is9U"
        + "zpERV0ePPQdLuW3IS_de3xyIrDaLGdjluPxUAhb6L2aXic1U12podGU0"
        + "KLUQSE_oI-ZnmKJ3F4uOZDnd6QZWJushZ41Axf_fcIe8u9ipH84ogore"
        + "e7vjbU5y18kDquDg\""
        + "}").replaceAll(" ", "");
    private static final String RSA_PSS_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"PS384\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replaceAll(" ", "");
    private static final String RSA_PSS_SIGNATURE_PROTECTED_HEADER =
          "eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX"
        + "hhbXBsZSJ9";
    private static final String RSA_PSS_SIGNATURE_VALUE =
          "cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy42miAh2qyBzk1xEsnk2I"
        + "pN6-tPid6VrklHkqsGqDqHCdP6O8TTB5dDDItllVo6_1OLPpcbUrhiUSMxbbXU"
        + "vdvWXzg-UD8biiReQFlfz28zGWVsdiNAUf8ZnyPEgVFn442ZdNqiVJRmBqrYRX"
        + "e8P_ijQ7p8Vdz0TTrxUeT3lm8d9shnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT"
        + "0qI0n6uiP1aCN_2_jLAeQTlqRHtfa64QQSUmFAAjVKPbByi7xho0uTOcbH510a"
        + "6GYmJUAfmWjwZ6oD4ifKo8DYM-X72Eaw";
    private static final String RSA_PSS_JSON_GENERAL_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"signatures\": ["
        + "{"
        + "\"protected\": \"eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2"
        + "dpbnNAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy"
        + "42miAh2qyBzk1xEsnk2IpN6-tPid6VrklHkqsGqDqHCdP6O8TTB5"
        + "dDDItllVo6_1OLPpcbUrhiUSMxbbXUvdvWXzg-UD8biiReQFlfz2"
        + "8zGWVsdiNAUf8ZnyPEgVFn442ZdNqiVJRmBqrYRXe8P_ijQ7p8Vd"
        + "z0TTrxUeT3lm8d9shnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT0q"
        + "I0n6uiP1aCN_2_jLAeQTlqRHtfa64QQSUmFAAjVKPbByi7xho0uT"
        + "OcbH510a6GYmJUAfmWjwZ6oD4ifKo8DYM-X72Eaw\""
        + "}"
        + "]"
        + "}").replace(" ", "");
    private static final String RSA_PSS_JSON_FLATTENED_SERIALIZATION = ("{"
        + "\"payload\": \"SXTigJlzIGEgZGFuZ2Vyb3VzIGJ1c2luZXNzLCBGcm9kbywg"
        + "Z29pbmcgb3V0IHlvdXIgZG9vci4gWW91IHN0ZXAgb250byB0aGUgcm9h"
        + "ZCwgYW5kIGlmIHlvdSBkb24ndCBrZWVwIHlvdXIgZmVldCwgdGhlcmXi"
        + "gJlzIG5vIGtub3dpbmcgd2hlcmUgeW91IG1pZ2h0IGJlIHN3ZXB0IG9m"
        + "ZiB0by4\","
        + "\"protected\": \"eyJhbGciOiJQUzM4NCIsImtpZCI6ImJpbGJvLmJhZ2dpbn"
        + "NAaG9iYml0b24uZXhhbXBsZSJ9\","
        + "\"signature\": \"cu22eBqkYDKgIlTpzDXGvaFfz6WGoz7fUDcfT0kkOy42mi"
        + "Ah2qyBzk1xEsnk2IpN6-tPid6VrklHkqsGqDqHCdP6O8TTB5dDDItllV"
        + "o6_1OLPpcbUrhiUSMxbbXUvdvWXzg-UD8biiReQFlfz28zGWVsdiNAUf"
        + "8ZnyPEgVFn442ZdNqiVJRmBqrYRXe8P_ijQ7p8Vdz0TTrxUeT3lm8d9s"
        + "hnr2lfJT8ImUjvAA2Xez2Mlp8cBE5awDzT0qI0n6uiP1aCN_2_jLAeQT"
        + "lqRHtfa64QQSUmFAAjVKPbByi7xho0uTOcbH510a6GYmJUAfmWjwZ6oD"
        + "4ifKo8DYM-X72Eaw\""
        + "}").replace(" ", "");
    private static final String ECDSA_KID_VALUE = "bilbo.baggins@hobbiton.example";
    private static final String ECDSA_SIGNATURE_PROTECTED_HEADER_JSON = ("{"
        + "\"alg\": \"ES512\","
        + "\"kid\": \"bilbo.baggins@hobbiton.example\""
        + "}").replace(" ", "");
    private static final String ECSDA_SIGNATURE_PROTECTED_HEADER =
              "eyJhbGciOiJFUzUxMiIsImtpZCI6ImJpbGJvLmJhZ2dpbnNAaG9iYml0b24uZX"
            + "hhbXBsZSJ9";
    
    @Test
    public void testEncodedPayload() throws Exception {
        assertEquals(Base64UrlUtility.encode(PAYLOAD), ENCODED_PAYLOAD);
    }
    @Test
    public void testRSAv15Signature() throws Exception {
        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
        compactProducer.getJoseHeaders().setAlgorithm(JoseConstants.RS_SHA_256_ALGO);
        compactProducer.getJoseHeaders().setKeyId(RSA_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJoseHeaders().asMap()), RSA_V1_5_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                RSA_V1_5_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey rsaKey = keys.get(1);
        compactProducer.signWith(rsaKey);
        assertEquals(compactProducer.getSignedEncodedJws(),
                RSA_V1_5_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD + "." + RSA_V1_5_SIGNATURE_VALUE);
        JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
        JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> publicKeys = publicJwks.getKeys();
        JsonWebKey rsaPublicKey = publicKeys.get(1);
        assertTrue(compactConsumer.verifySignatureWith(rsaPublicKey, JoseConstants.RS_SHA_256_ALGO));

        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JoseHeaders joseHeaders = new JoseHeaders();
        joseHeaders.setAlgorithm(JoseConstants.RS_SHA_256_ALGO);
        joseHeaders.setKeyId(RSA_KID_VALUE);
        JwsJsonProtectedHeader protectedHeader = new JwsJsonProtectedHeader(joseHeaders);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, JoseConstants.RS_SHA_256_ALGO), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(), RSA_V1_5_JSON_GENERAL_SERIALIZATION);
        JwsJsonConsumer jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, JoseConstants.RS_SHA_256_ALGO));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, JoseConstants.RS_SHA_256_ALGO), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument(), RSA_V1_5_JSON_FLATTENED_SERIALIZATION);
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, JoseConstants.RS_SHA_256_ALGO));
    }
    @Test
    public void testRSAPSSSignature() throws Exception {
        try {
            Cipher.getInstance(Algorithm.PS_SHA_384_JAVA);
        } catch (Throwable t) {
            Security.addProvider(new BouncyCastleProvider());
        }

        JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
        compactProducer.getJoseHeaders().setAlgorithm(JoseConstants.PS_SHA_384_ALGO);
        compactProducer.getJoseHeaders().setKeyId(RSA_KID_VALUE);
        JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
        assertEquals(reader.toJson(compactProducer.getJoseHeaders().asMap()), RSA_PSS_SIGNATURE_PROTECTED_HEADER_JSON);
        assertEquals(compactProducer.getUnsignedEncodedJws(),
                RSA_PSS_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
        JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
        List<JsonWebKey> keys = jwks.getKeys();
        JsonWebKey rsaKey = keys.get(1);
        compactProducer.signWith(rsaKey);
        assertEquals(compactProducer.getSignedEncodedJws().length(),
                (RSA_PSS_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD + "." + RSA_PSS_SIGNATURE_VALUE).length());
        JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
        JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
        List<JsonWebKey> publicKeys = publicJwks.getKeys();
        JsonWebKey rsaPublicKey = publicKeys.get(1);
        assertTrue(compactConsumer.verifySignatureWith(rsaPublicKey, JoseConstants.PS_SHA_384_ALGO));

        JwsJsonProducer jsonProducer = new JwsJsonProducer(PAYLOAD);
        assertEquals(jsonProducer.getPlainPayload(), PAYLOAD);
        assertEquals(jsonProducer.getUnsignedEncodedPayload(), ENCODED_PAYLOAD);
        JoseHeaders joseHeaders = new JoseHeaders();
        joseHeaders.setAlgorithm(JoseConstants.PS_SHA_384_ALGO);
        joseHeaders.setKeyId(RSA_KID_VALUE);
        JwsJsonProtectedHeader protectedHeader = new JwsJsonProtectedHeader(joseHeaders);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, JoseConstants.PS_SHA_384_ALGO), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument().length(), RSA_PSS_JSON_GENERAL_SERIALIZATION.length());
        JwsJsonConsumer jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, JoseConstants.PS_SHA_384_ALGO));

        jsonProducer = new JwsJsonProducer(PAYLOAD, true);
        jsonProducer.signWith(JwsUtils.getSignatureProvider(rsaKey, JoseConstants.PS_SHA_384_ALGO), protectedHeader);
        assertEquals(jsonProducer.getJwsJsonSignedDocument().length(), RSA_PSS_JSON_FLATTENED_SERIALIZATION.length());
        jsonConsumer = new JwsJsonConsumer(jsonProducer.getJwsJsonSignedDocument());
        assertTrue(jsonConsumer.verifySignatureWith(rsaPublicKey, JoseConstants.PS_SHA_384_ALGO));

        Security.removeProvider(BouncyCastleProvider.class.getName());
    }
    @Test
    public void testECDSASignature() throws Exception {
        
        try {
            Cipher.getInstance(Algorithm.ES_SHA_512_JAVA);
        } catch (Throwable t) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            JwsCompactProducer compactProducer = new JwsCompactProducer(PAYLOAD);
            compactProducer.getJoseHeaders().setAlgorithm(JoseConstants.ES_SHA_512_ALGO);
            compactProducer.getJoseHeaders().setKeyId(ECDSA_KID_VALUE);
            JsonMapObjectReaderWriter reader = new JsonMapObjectReaderWriter();
            assertEquals(reader.toJson(compactProducer.getJoseHeaders().asMap()), 
                         ECDSA_SIGNATURE_PROTECTED_HEADER_JSON);
            assertEquals(compactProducer.getUnsignedEncodedJws(),
                    ECSDA_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
            JsonWebKeys jwks = readKeySet("cookbookPrivateSet.txt");
            List<JsonWebKey> keys = jwks.getKeys();
            JsonWebKey ecKey = keys.get(0);
            compactProducer.signWith(new EcDsaJwsSignatureProvider(JwkUtils.toECPrivateKey(ecKey),
                    JoseConstants.ES_SHA_512_ALGO));
            assertEquals(compactProducer.getUnsignedEncodedJws(), 
                         ECSDA_SIGNATURE_PROTECTED_HEADER + "." + ENCODED_PAYLOAD);
            assertEquals(132, Base64UrlUtility.decode(compactProducer.getEncodedSignature()).length);
            
            JwsCompactConsumer compactConsumer = new JwsCompactConsumer(compactProducer.getSignedEncodedJws());
            JsonWebKeys publicJwks = readKeySet("cookbookPublicSet.txt");
            List<JsonWebKey> publicKeys = publicJwks.getKeys();
            JsonWebKey ecPublicKey = publicKeys.get(0);
            assertTrue(compactConsumer.verifySignatureWith(ecPublicKey, JoseConstants.ES_SHA_512_ALGO));
        } finally {
            Security.removeProvider(BouncyCastleProvider.class.getName());
        }
    }
    public JsonWebKeys readKeySet(String fileName) throws Exception {
        InputStream is = JwsJoseCookBookTest.class.getResourceAsStream(fileName);
        String s = IOUtils.readStringFromStream(is);
        return JwkUtils.readJwkSet(s);
    }
}