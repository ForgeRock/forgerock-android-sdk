/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.Serializable
import java.io.StringReader
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Domain object for advice. The Advice is received from resource API for Step up authentication
 */
class PolicyAdvice internal constructor(val type: String, val value: String) :
    Serializable {

    override fun toString(): String {
        return "<Advices>" +
                "<AttributeValuePair>" +
                "<Attribute name=\"" + type + "\"/>" +
                "<Value>" + value + "</Value>" +
                "</AttributeValuePair>" +
                "</Advices>"
    }

    fun getType(): Int {
        return when (type) {
            "AuthenticateToServiceConditionAdvice" -> AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE
            "TransactionConditionAdvice" -> TRANSACTION_CONDITION_ADVICE
            else -> UNKNOWN
        }
    }

    class PolicyAdviceBuilder internal constructor() {
        private var type: String = ""
        private var value: String = ""
        fun type(type: String): PolicyAdviceBuilder {
            this.type = type
            return this
        }

        fun value(value: String): PolicyAdviceBuilder {
            this.value = value
            return this
        }

        fun build(): PolicyAdvice {
            return PolicyAdvice(type, value)
        }

        override fun toString(): String {
            return "PolicyAdvice.PolicyAdviceBuilder(type=$type, value=$value)"
        }
    }

    companion object {
        const val AUTH_LEVEL_CONDITION_ADVICE = 1
        const val AUTH_SCHEME_CONDITION_ADVICE = 2
        const val AUTHENTICATE_TO_REALM_CONDITION_ADVICE = 3
        const val AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE = 4
        const val SESSION_CONDITION_ADVICE = 5
        const val TRANSACTION_CONDITION_ADVICE = 6
        const val UNKNOWN = -1

        @JvmStatic
        fun builder(): PolicyAdviceBuilder {
            return PolicyAdviceBuilder()
        }

        /**
         * Parse the advice xml
         *
         * @param advice The Advice in xml form
         * @return The parsed Policy Advice
         */
        @JvmStatic
        fun parse(advice: String): PolicyAdvice {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(InputSource(StringReader(advice)))
            val attributes = document.getElementsByTagName("AttributeValuePair")
            for (i in 0 until attributes.length) {
                val node = attributes.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    val element2 = node as Element
                    val attribute = element2.getElementsByTagName("Attribute")
                        .item(0).attributes.getNamedItem("name").nodeValue
                    val value =
                        element2.getElementsByTagName("Value").item(0).firstChild.nodeValue
                    return builder()
                        .type(attribute)
                        .value(value).build()
                }
            }
            throw IllegalArgumentException("Advice Not Found: $advice")
        }

        /**
         * Parse the advice as base64 encoded xml String
         * @param Base64 encoded xml String
         * @return The parsed Policy Advice
         */
        fun parseAsBase64XML(base64XML: String): PolicyAdvice {
            return parse(String(Base64.decode(base64XML,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP), StandardCharsets.UTF_8))
        }


        /**
         * Parse the advice as base64 encoded json String
         * @param Base64 encoded json String
         * @return The parsed Policy Advice
         */
        fun parseAsBase64(advice: String?): PolicyAdvice {
            val jsonObject = JSONObject(String(Base64.decode(advice,
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)))
            return builder()
                .type("TransactionConditionAdvice")
                .value(jsonObject.getJSONArray("TransactionConditionAdvice").getString(0))
                .build()
        }
    }
}