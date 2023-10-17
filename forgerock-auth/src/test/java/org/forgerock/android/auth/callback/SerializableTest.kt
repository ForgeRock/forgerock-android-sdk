/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.Node
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.TypePredicates
import org.jeasy.random.randomizers.text.StringRandomizer
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

//Test to make sure all Callback class are serializable
@RunWith(AndroidJUnit4::class)
class SerializableTest {
    @Test
    fun testCallbackSerializable() {
        //We generate String for Object type.
        val parameters = EasyRandomParameters()
        parameters.randomize(Any::class.java) { StringRandomizer().randomValue }
        parameters.randomize(Attestation::class.java) { Attestation.Default("1234".toByteArray()) }
        parameters.excludeType(TypePredicates.ofType(Node::class.java))
        parameters.stringLengthRange(3, 3)
        val generator = EasyRandom(parameters)
        for ((_, value) in CallbackFactory.getInstance().callbacks) {
            val callback = generator.nextObject(value)!!
            if (callback is NodeAware) {
                val node = Node("authId", "", "", "", "", listOf(callback))
                callback.setNode(node)
            }
            convertToBytes(callback)
        }
    }

    private fun convertToBytes(`object`: Any): ByteArray {
        ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { out ->
                out.writeObject(`object`)
                return bos.toByteArray()
            }
        }
    }
}