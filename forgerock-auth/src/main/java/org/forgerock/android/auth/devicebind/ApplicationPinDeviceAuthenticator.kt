/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import org.forgerock.android.auth.InitProvider
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
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.KeyStore.PrivateKeyEntry
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.X509Certificate
import java.security.spec.RSAKeyGenParameterSpec
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Device Authenticator which use Application PIN to secure device cryptography keys
 */
open class ApplicationPinDeviceAuthenticator(private val context: Context) : CryptoAware, DeviceAuthenticator {

    internal lateinit var keyAware: KeyAware

    private val PIN_SUFFIX = "_PIN"

    @VisibleForTesting
    internal val pinRef = AtomicReference<CharArray>()
    private val worker = Executors.newSingleThreadScheduledExecutor()

    override fun generateKeys(callback: (KeyPair) -> Unit) {
        val spec = RSAKeyGenParameterSpec(keyAware.keySize, RSAKeyGenParameterSpec.F4)
        val keyPair = keyAware.createKeyPair(spec, false);
        //This allow a user to have an biometric key + application pin
        keyPair.keyAlias = keyPair.keyAlias + PIN_SUFFIX
        requestForCredentials() { password ->
            pinRef.set(password)
            persist(context,
                keyAware.key,
                java.security.KeyPair(keyPair.publicKey, keyPair.privateKey),
                password)
            //Clean up the password from memory after 1 second
            worker.schedule({
                pinRef.set(null)
            }, 1, TimeUnit.SECONDS)
            callback(keyPair)
        }
    }

    override fun authenticate(timeout: Int,
                              statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit) {
        val pin = pinRef.getAndSet(null)
        pin?.let { getPrivateKey(context, keyAware.key, it, statusResult) } ?: run {
            val startTime = Date().time
            requestForCredentials() {
                val endTime = TimeUnit.MILLISECONDS.toSeconds(Date().time - startTime)
                if (endTime > (timeout.toLong())) {
                    statusResult(Timeout())
                } else {
                    getPrivateKey(context, keyAware.key, it, statusResult)
                }
            }
        }
    }

    override fun isSupported(): Boolean {
        return true
    }

    /**
     * Request for user credential
     * @param fragmentActivity The current [FragmentActivity]
     * @param onCredentialsReceived Function to invoke after receiving the user pin
     */
    open fun requestForCredentials(fragmentActivity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity(),
                                   onCredentialsReceived: (CharArray) -> Unit) {
        ApplicationPinFragment.newInstance().apply {
            this.onPinReceived = {
                onCredentialsReceived(it.toCharArray())
            }
            this.onCancelled = {
                onCredentialsReceived(CharArray(0))
            }
            this.show(fragmentActivity.supportFragmentManager, DeviceBindFragment.TAG)
        }
    }

    /**
     * Retrieve the file input stream to store the [KeyStore]
     * @param context The Application [Context]
     * @param keyAlias The suggested key alias
     */
    open fun getKeystoreFileInputStream(context: Context, keyAlias: String): FileInputStream {
        return getEncryptedFile(context, keyAlias).openFileInput();
    }

    /**
     * Retrieve the file output stream of the [KeyStore]
     * @param context The Application [Context]
     * @param keyAlias The suggested key alias
     */
    open fun getKeystoreFileOutputStream(context: Context, keyAlias: String): FileOutputStream {
        return getEncryptedFile(context, keyAlias).openFileOutput();
    }

    /**
     * Retrieve the Keystore Type, default to [KeyStore.getDefaultType]
     */
    open fun getKeystoreType(): String {
        return KeyStore.getDefaultType()
    }

    private fun persist(context: Context,
                        keyAlias: String,
                        keyPair: java.security.KeyPair,
                        pin: CharArray) {
        val keyStore = KeyStore.getInstance(getKeystoreType())
        keyStore.load(null);
        val privateKeyEntry =
            PrivateKeyEntry(keyPair.private, arrayOf(generateCertificate(keyPair)))
        keyStore.setEntry(keyAlias, privateKeyEntry, PasswordProtection(pin))
        getKeystoreFileOutputStream(context, keyAlias).use {
            it.flush()
            keyStore.store(it, null)
        }

    }

    private fun getPrivateKey(context: Context,
                              keyAlias: String,
                              pin: CharArray,
                              statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit) {
        if (pin.isNotEmpty()) {
            try {
                val keystore = KeyStore.getInstance(getKeystoreType())
                getKeystoreFileInputStream(context, keyAlias).use {
                    keystore.load(it, null);
                }
                val entry =
                    keystore.getEntry(keyAware.key, PasswordProtection(pin)) as PrivateKeyEntry
                statusResult(Success(entry.privateKey))
            } catch (e: FileNotFoundException) {
                statusResult(UnRegister())
                return
            } catch (e: UnrecoverableKeyException) {
                statusResult(UnAuthorize())
            }
        } else {
            statusResult(Abort())
        }
    }

    private fun getEncryptedFile(context: Context, keyAlias: String): EncryptedFile {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val file = File(context.filesDir, keyAlias)
        return EncryptedFile.Builder(file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
    }

    private fun generateCertificate(keyPair: java.security.KeyPair): X509Certificate? {
        val validityBeginDate = Date()
        val owner = X500Name("cn=self signed")
        val sigAlgId: AlgorithmIdentifier =
            DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA")
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

    override fun setKeyAware(keyAware: KeyAware) {
        this.keyAware = keyAware
    }

}