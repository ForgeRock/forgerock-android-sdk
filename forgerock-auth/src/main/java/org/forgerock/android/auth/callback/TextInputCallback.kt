/*
 * Copyright (c) 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import androidx.annotation.Keep
import org.json.JSONObject

/**
 * Callback for collection of a single text input attribute from a user.
 *
 *
 */
class TextInputCallback : AbstractPromptCallback {

    /**
     * TextInputCallback sample.
     *
     *         {
     *             "type": "TextInputCallback",
     *             "output": [
     *                 {
     *                     "name": "prompt",
     *                     "value": "Text input"
     *                 },
     *                 {
     *                     "name": "defaultText",
     *                     "value": ""
     *                 }
     *             ],
     *             "input": [
     *                 {
     *                     "name": "IDToken1",
     *                     "value": ""
     *                 }
     *             ]
     *         }
     *
     */

    /**
     * The text to be used as the default text displayed with the prompt.
     */
    var defaultText: String? = null
        private set

    @Keep
    @JvmOverloads
    constructor() : super()

    @Keep
    @JvmOverloads
    constructor(raw: JSONObject?, index: Int) : super(raw, index)

    override fun setAttribute(name: String, value: Any) {
        super.setAttribute(name, value)
        when (name) {
            "defaultText" -> defaultText = value as String
            else -> {}
        }
    }

    /**
     * Set the text.
     * @param value the text, which may be null.
     */
    fun setValue(value: String?) {
        super.setValue(value)
    }

    override fun getType(): String {
        return "TextInputCallback"
    }
}