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
 * Callback to retrieve the selected choice(s) from a list of choices
 */
@Getter
public class ChoiceCallback extends AbstractPromptCallback {

    /**
     * Get the list of choices.
     *
     * <p>
     *
     * @return the list of choices.
     */
    private List<String> choices;

    /**
     * Get the defaultChoice.
     *
     * <p>
     *
     * @return the defaultChoice, represented as an index into
     *          the {@code choices} list.
     */
    private int defaultChoice;

    @Keep
    public ChoiceCallback() {
    }

    @Keep
    public ChoiceCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        switch (name) {
            case "choices":
                prepareChoices((JSONArray) value);
                break;
            case "defaultChoice":
                this.defaultChoice = (int) value;
                break;

            default:
                //ignore
        }
    }

    /**
     * Set the selected choice.
     * <p>
     *
     * @param selection the selection represented as an index into the
     *                  {@code choices} list.
     */
    public void setSelectedIndex(int selection) {
        setValue(selection);
    }

    /**
     * Populate the {@link ChoiceCallback#choices} attribute
     * @param array The data source
     */
    private void prepareChoices(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.getString(i));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        choices = Collections.unmodifiableList(list);
    }

    @Override
    public String getType() {
        return "ChoiceCallback";
    }
}
