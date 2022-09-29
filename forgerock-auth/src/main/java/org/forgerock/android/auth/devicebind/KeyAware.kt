/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
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
import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.*


/**
 * Helper class to generate and sign the keys
 */
internal class KeyAware(private var userId: String) {

    constructor() : this("")

    private val hashingAlgorithm = "SHA-256"
    private val keySize = 2048
    val timeout = 60
    private val androidKeyStore = "AndroidKeyStore"
    private val encryptionBlockMode = KeyProperties.BLOCK_MODE_ECB
    private val encryptionPadding = KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    private val encryptionAlgorithm = KeyProperties.KEY_ALGORITHM_RSA
    private val signaturePadding = KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
    private val purpose = KeyProperties.PURPOSE_SIGN or
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    private val key = getKeyAlias(userId)

    /**
     * Builder to create a keypair
     */
    fun keyBuilder(): KeyGenParameterSpec.Builder {
        return KeyGenParameterSpec.Builder(
            key,
            purpose
        ).setDigests(KeyProperties.DIGEST_SHA512)
            .setKeySize(keySize)
            .setSignaturePaddings(signaturePadding)
            .setBlockModes(encryptionBlockMode)
            .setEncryptionPaddings(encryptionPadding)
    }

    /**
     * Creates Keypair for the given builder
     * @param builder keygen parameter as input to get the keypair
     */
    fun createKeyPair(builder: KeyGenParameterSpec.Builder, keyStoreType: String = androidKeyStore): KeyPair {
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
            encryptionAlgorithm, keyStoreType
        )
        keyPairGenerator.initialize(builder.build())
        keyPairGenerator.generateKeyPair()

        val keyStore = getKeyStore()

        val publicKey = keyStore.getCertificate(key).publicKey as RSAPublicKey
        val privateKey = keyStore.getKey(key, null) as PrivateKey

        return KeyPair(publicKey, privateKey, key)
    }


    /**
     * Get the private key from the Keypair for the given builder
     * @param keyAlias key hash of the user
     */
    fun getSecureKey(keyAlias: String = key): PrivateKey? {
        val keyStore: KeyStore = getKeyStore()
        return keyStore.getKey(keyAlias, null) as? PrivateKey
    }

    /**
     * get hash for the given user
     * @param keyName username as a key
     */
    private fun getKeyAlias(keyName: String = userId): String {
        return try {
            val digest: MessageDigest = MessageDigest.getInstance(hashingAlgorithm)
            val hash: ByteArray? = digest.digest(keyName.toByteArray())
            Base64.encodeToString(hash, Base64.DEFAULT)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
    /**
     * Creates centralised SecretKey using the KeyStore
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getKeyStore(type: String = androidKeyStore): KeyStore {
        val keyStore = KeyStore.getInstance(type)
        keyStore.load(null)
        return keyStore
    }

    @Throws(java.lang.Exception::class)
    fun generateCertificate(keyPair: java.security.KeyPair): X509Certificate? {
        // yesterday
        val validityBeginDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        // in 2 years
        val validityEndDate = Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000)
        val owner = X500Name("cn=self signed")
        val sigAlgId: AlgorithmIdentifier =
            DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA")
        val digAlgId: AlgorithmIdentifier = DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
        val privateKeyAsymKeyParam: AsymmetricKeyParameter =
            PrivateKeyFactory.createKey(keyPair.private.encoded)
        val sigGen = BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam)
        val builder = X509v1CertificateBuilder(
            owner,
            BigInteger.valueOf(System.currentTimeMillis()),
            validityBeginDate,
            validityEndDate,
            owner,
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        )
        return JcaX509CertificateConverter().getCertificate(builder.build(sigGen))
    }


    /**
     * Creates centralised SecretKey using the KeyStore
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    fun setApplicationKeyWithPassword(context: Context): KeyPair {
        var keyStore: KeyStore? = null
        try {
            // Storing
            val password: CharArray = charArrayOf('J', 'e', 'Y')

            val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)

            val keyPair = keyPairGenerator.generateKeyPair()
            val certificate: X509Certificate? = generateCertificate(keyPair)

            val file = File(context.filesDir, "secretdata1")

            val defaultKeyStore = KeyStore.getInstance("PKCS12")


            val publicKey = keyPair.public
            val privateKey = keyPair.private

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                 masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            certificate?.let {

                defaultKeyStore.load(null)
                defaultKeyStore.setKeyEntry(key, keyPair.private, password, arrayOf(certificate))

                val fileOutStream = encryptedFile.openFileOutput()
                val objectOutputStream = ObjectOutputStream(fileOutStream)
                defaultKeyStore.store(objectOutputStream, password)

                objectOutputStream.close()
                fileOutStream.flush()
                fileOutStream.close()

            }

            return KeyPair(publicKey as RSAPublicKey, privateKey, key)

        }
        catch (e: Exception) {
          throw e
        }
    }

    fun getSecureKey(keyAlias: String = key, context: Context): PrivateKey {
        val file = File(context.filesDir, "secretdata1")

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val encryptedFile = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val password: CharArray = charArrayOf('J', 'e', 'Y')
        val foo = KeyStore.getInstance("PKCS12")
        val encryptedInputStream = encryptedFile.openFileInput()
        val objectInputStream = ObjectInputStream(encryptedInputStream)
        foo.load(objectInputStream, password)

        val protParam: KeyStore.ProtectionParameter = KeyStore.PasswordProtection(password)
        val pkEntry = foo.getEntry(keyAlias, protParam) as KeyStore.PrivateKeyEntry
        return pkEntry.privateKey
    }

}