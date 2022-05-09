/*
 *
 *  * Copyright (c) 2022 ForgeRock. All rights reserved.
 *  *
 *  * This software may be modified and distributed under the terms
 *  * of the MIT license. See the LICENSE file for details.
 *
 *
 */

package org.forgerock.android.auth

import com.appmattus.certificatetransparency.BasicAndroidCTLogger
import com.appmattus.certificatetransparency.CTLogger
import com.appmattus.certificatetransparency.VerificationResult
import com.appmattus.certificatetransparency.certificateTransparencyInterceptor
import okhttp3.Interceptor
import org.forgerock.android.core.BuildConfig

class CTInterceptor {
     fun getCTInterceptor(
         includeHosts: List<String>,
         excludeHosts: List<String>,
     ): Interceptor? {
         if(includeHosts.isEmpty() && excludeHosts.isEmpty()) {
             return null
         }

         val defaultLogger = object : CTLogger {
             override fun log(host: String, result: VerificationResult) {
                Logger.debug("$host ----> $result", result.toString(), "")
             }
         }

        val networkInterceptor = certificateTransparencyInterceptor {
            includeHosts.forEach {
                +it
            }
            excludeHosts.forEach {
                -it
            }
            failOnError = true
            logger = defaultLogger

        }
         return networkInterceptor
    }
}