/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import org.forgerock.android.auth.Callback
import org.forgerock.android.auth.Logger.Companion.error
import org.forgerock.android.auth.callback.AppIntegrityCallback
import org.forgerock.android.auth.callback.WebAuthnAuthenticationCallback
import org.forgerock.android.auth.callback.WebAuthnRegistrationCallback

/**
 * Factory to manage supported [Callback]
 */
class CallbackFactory private constructor() {

    internal val callbacks: MutableMap<String, Class<out Callback>> = HashMap()

    init {
        register(ChoiceCallback::class.java)
        register(NameCallback::class.java)
        register(PasswordCallback::class.java)
        register(StringAttributeInputCallback::class.java)
        register(NumberAttributeInputCallback::class.java)
        register(BooleanAttributeInputCallback::class.java)
        register(ValidatedPasswordCallback::class.java)
        register(ValidatedUsernameCallback::class.java)
        register(KbaCreateCallback::class.java)
        register(TermsAndConditionsCallback::class.java)
        register(PollingWaitCallback::class.java)
        register(ConfirmationCallback::class.java)
        register(TextOutputCallback::class.java)
        register(SuspendedTextOutputCallback::class.java)
        register(ReCaptchaCallback::class.java)
        register(ConsentMappingCallback::class.java)
        register(HiddenValueCallback::class.java)
        register(DeviceProfileCallback::class.java)
        register(MetadataCallback::class.java)
        register(WebAuthnRegistrationCallback::class.java)
        register(WebAuthnAuthenticationCallback::class.java)
        register(SelectIdPCallback::class.java)
        register(IdPCallback::class.java)
        register(DeviceBindingCallback::class.java)
        register(DeviceSigningVerifierCallback::class.java)
        register(AppIntegrityCallback::class.java)
        registerAnonymous()
    }

    private fun registerAnonymous() {
        val packageName = javaClass.getPackage()?.name
        try {
            listOf("PingOneProtectEvaluationCallback", "PingOneProtectInitCallback").forEach {
                val clazz = Class.forName("$packageName.$it")
                register(clazz.asSubclass(Callback::class.java))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Register new Callback Class
     *
     * @param callback The callback Class
     */
    fun register(callback: Class<out Callback>) {
        try {
            callbacks[getType(callback)] = callback
        } catch (e: Exception) {
            error(TAG, e, e.message)
        }
    }

    @Throws(InstantiationException::class, IllegalAccessException::class)
    fun getType(callback: Class<out Callback>): String {
        return callback.newInstance().type
    }

    fun getCallbacks(): Map<String, Class<out Callback>> {
        return callbacks
    }

    companion object {
        private val TAG = CallbackFactory::class.java.simpleName

        /**
         * Returns a cached instance [CallbackFactory]
         *
         * @return instance of [CallbackFactory]
         */
        @JvmField
        val instance = CallbackFactory()
    }
}
