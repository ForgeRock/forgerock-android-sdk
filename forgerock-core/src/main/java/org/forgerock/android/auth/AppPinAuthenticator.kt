/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.content.Context
import java.math.BigInteger
import java.security.KeyPair
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.*
import org.spongycastle.asn1.x500.X500Name
import org.spongycastle.asn1.x509.AlgorithmIdentifier
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.cert.X509v1CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.crypto.params.AsymmetricKeyParameter
import org.spongycastle.crypto.util.PrivateKeyFactory
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder
import org.spongycastle.operator.bc.BcRSAContentSignerBuilder
import java.io.IOException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.security.spec.RSAKeyGenParameterSpec

private val TAG = AppPinAuthenticator::class.java.simpleName
/**
 * An authenticator to authenticate the user with Application Pin
 */
class AppPinAuthenticator(private val cryptoKey: CryptoKey,
                          private val keyStoreRepository: KeyStoreRepository = EncryptedFileKeyStore(cryptoKey.keyAlias)) {

    private val keyStoreType = "PKCS12"
    /**
     * Generate [KeyPair], and persist the [KeyPair] to provided [KeyStoreRepository]
     * @param context The Application Context
     * @param pin The pin to secure the generated Key
     * @return The [KeyPair]
     */
    fun generateKeys(context: Context, pin: CharArray): KeyPair {
        val spec = RSAKeyGenParameterSpec(cryptoKey.keySize, RSAKeyGenParameterSpec.F4)
        val keyPair = cryptoKey.createKeyPair(spec, false);
        persist(context, keyPair, pin)
        return keyPair
    }

    /**
     * Unlock the [PrivateKey] with the provided application pin
     * @param context The Application Context
     * @param pin The pin to unlock the [PrivateKey]
     * @return The [PrivateKey]
     */
    @Throws(IOException::class, UnrecoverableKeyException::class)
    fun getPrivateKey(context: Context, pin: CharArray): PrivateKey? {
        val keyStore = getKeyStore(context, pin)
        val entry = keyStore.takeIf { it.isKeyEntry(cryptoKey.keyAlias) }
            ?.getEntry(cryptoKey.keyAlias,
                KeyStore.PasswordProtection(pin)) as? KeyStore.PrivateKeyEntry
        return entry?.privateKey
    }

    /**
     * Checks if the given alias exists in this keystore.
     * @param context the application context
     * @return true if the file exists, false otherwise
     */
    fun exists(context: Context): Boolean = keyStoreRepository.exist(context)

    private fun getKeyStore(context: Context, pin: CharArray): KeyStore {
        val keystore = KeyStore.getInstance(keyStoreType)

        keyStoreRepository.getInputStream(context).use {
            keystore.load(it, pin)
        }
        return keystore
    }

    /**
     * Return the key alias
     */
    fun getKeyAlias(): String {
        return cryptoKey.keyAlias
    }

    private fun persist(context: Context, keyPair: KeyPair, pin: CharArray) {
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null)
        val privateKeyEntry = KeyStore.PrivateKeyEntry(keyPair.private,
            arrayOf(generateCertificate(keyPair, cryptoKey.keyAlias)))
        keyStore.setEntry(getKeyAlias(), privateKeyEntry, KeyStore.PasswordProtection(pin))
        keyStoreRepository.getOutputStream(context).use {
            it.flush()
            keyStore.store(it, pin)
        }
    }

    private fun generateCertificate(keyPair: KeyPair, subject: String): X509Certificate {
        val validityBeginDate = Date()
        val owner = X500Name("cn=$subject")
        val sigAlgId: AlgorithmIdentifier =
            DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSA")
        val digAlgId: AlgorithmIdentifier = DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
        val privateKeyAsymKeyParam: AsymmetricKeyParameter =
            PrivateKeyFactory.createKey(keyPair.private.encoded)
        val sigGen = BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam)
        val builder = X509v1CertificateBuilder(owner,
            BigInteger.valueOf(System.currentTimeMillis()),
            validityBeginDate,
            validityBeginDate,
            owner,
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded))
        return JcaX509CertificateConverter().getCertificate(builder.build(sigGen))
    }
}