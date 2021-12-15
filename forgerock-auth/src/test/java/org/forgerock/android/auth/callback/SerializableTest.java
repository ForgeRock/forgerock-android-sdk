/*
 * Copyright (c) 2021 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

//Test to make sure all Callback class are serializable
@RunWith(RobolectricTestRunner.class)
public class SerializableTest {

    @Test
    public void testCallbackSerializable() throws IOException {
        //We generate String for Object type.
        EasyRandomParameters parameters = new EasyRandomParameters();
        parameters.randomize(Object.class, new Randomizer<Object>() {
            @Override
            public Object getRandomValue() {
                return new StringRandomizer().getRandomValue();
            }
        });
        parameters.stringLengthRange(3, 3);
        EasyRandom generator = new EasyRandom(parameters);
        for (Map.Entry<String, Class<? extends Callback>> entry : CallbackFactory.getInstance().getCallbacks().entrySet()) {
            Callback callback = generator.nextObject(entry.getValue());
            convertToBytes(callback);
        }
    }

    private byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }
}
