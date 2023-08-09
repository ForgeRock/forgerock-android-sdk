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
 * Callback to retrieve the selected option from a list of options
 */
@Getter
public class ConfirmationCallback extends AbstractPromptCallback {

    //Option Type
    public static final int UNSPECIFIED_OPTION = -1;
    public static final int YES_NO_OPTION = 0;
    public static final int YES_NO_CANCEL_OPTION = 1;
    public static final int OK_CANCEL_OPTION = 2;

    //Option
    public static final int YES = 0;
    public static final int NO = 1;
    public static final int CANCEL = 2;
    public static final int OK = 3;

    //Message Type
    public static final int INFORMATION = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    /**
     * Get the list of options.
     *
     * <p>
     *
     * @return the list of options.
     */
    private List<String> options;

    /**
     * Get the defaultChoice.
     *
     * <p>
     *
     * @return the default option, represented as an index into
     * the {@code options} list.
     */
    private int defaultOption;

    private int optionType;

    private int messageType;

    private int selectedIndex;

    @Keep
    public ConfirmationCallback() {
    }

    @Keep
    public ConfirmationCallback(JSONObject jsonObject, int index) throws JSONException {
        super(jsonObject, index);
    }

    @Override
    protected void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        switch (name) {
            case "optionType":
                this.optionType = (int) value;
                break;
            case "defaultOption":
                this.defaultOption = (int) value;
                this.selectedIndex = this.defaultOption;
                break;
            case "messageType":
                this.messageType = (int) value;
                break;
            case "options":
                prepareOptions((JSONArray) value);
                break;
            default:
                //ignore

        }
    }

    /**
     * Populate the {@link ConfirmationCallback#options} attribute
     *
     * @param array The data source
     */
    private void prepareOptions(JSONArray array) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                list.add(array.getString(i));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        options = Collections.unmodifiableList(list);
    }

    /**
     * Set the selected choice.
     * <p>
     *
     * @param selection the selection represented as an index into the
     *                  {@code options} list.
     */
    public void setSelectedIndex(int selection) {
        selectedIndex = selection;
        setValue(selection);
    }


    @Override
    public String getType() {
        return "ConfirmationCallback";
    }
}
