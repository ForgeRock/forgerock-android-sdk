/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback;

import androidx.annotation.Keep;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A callback to collect a user's choice of security question and their answer to that question.
 */
@Getter
public class KbaCreateCallback extends AbstractPromptCallback {

    /**
     * Get the predefined questions available to the user to choose from.
     *
     * @return the list of predefined questions
     */
    private List<String> predefinedQuestions;

    @Keep
    public KbaCreateCallback() {
    }

    @Keep
    public KbaCreateCallback(JSONObject raw, int index) {
        super(raw, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if ("predefinedQuestions".equals(name)) {
            prepareQuestions((JSONArray) value);
        }


    }

    private void prepareQuestions(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.getString(i));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        predefinedQuestions = Collections.unmodifiableList(list);
    }


    /**
     * Set the user's chosen question.
     *
     * @param selectedQuestion the user's chosen question
     */
    public void setSelectedQuestion(String selectedQuestion) {
        setValue(selectedQuestion, 0);
    }

    /**
     * Set the user's chosen response to their chosen question.
     *
     * @param selectedAnswer the user's chosen response
     */
    public void setSelectedAnswer(String selectedAnswer) {
        setValue(selectedAnswer, 1);
    }

    @Override
    public String getType() {
        return "KbaCreateCallback";
    }

}
