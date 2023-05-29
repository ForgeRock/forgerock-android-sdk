/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;

import static org.forgerock.android.auth.Encryptor.getEncryptor;

/**
 * An implementation of {@link SharedPreferences} that encrypts values.
 */
public class SecuredSharedPreferences implements SharedPreferences {
    public static final String TAG = SecuredSharedPreferences.class.getName();

    private static final int STRING_TYPE = 0;
    private static final int STRING_SET_TYPE = 1;
    private static final int INT_TYPE = 2;
    private static final int LONG_TYPE = 3;
    private static final int FLOAT_TYPE = 4;
    private static final int BOOLEAN_TYPE = 5;
    public static final String VALUE = "value";
    private static final String TYPE = "type";

    @Getter
    private final SharedPreferences sharedPreferences;
    private final List<OnSharedPreferenceChangeListener> listeners;
    private final Encryptor encryptor;
    @Getter
    private final String keyAlias;

    SecuredSharedPreferences(Context context, String fileName, String keyAlias) {
        this(context, fileName, keyAlias, null);
    }

    SecuredSharedPreferences(Context context, String fileName, String keyAlias, Encryptor encryptor) {
        this.sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        this.listeners = new ArrayList<>();
        this.keyAlias = keyAlias;
        if (encryptor == null) {
            this.encryptor = getEncryptor(context,
                    keyAlias);
        } else {
            this.encryptor = encryptor;
        }
    }


    @Override
    @NonNull
    public Map<String, ?> getAll() {
        Map<String, ? super Object> entries = new HashMap<>();
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            if (!isKeyAlias(entry.getKey())) {
                Object decryptedValue = get(entry.getKey());
                entries.put(entry.getKey(), decryptedValue);
            }
        }
        return entries;
    }

    private Set<String> keys() {
        return sharedPreferences.getAll().keySet();
    }

    @Nullable
    @Override
    public String getString(@Nullable String key, @Nullable String defValue) {
        Object value = get(key);
        return (value instanceof String ? (String) value : defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(@lombok.NonNull String key, @Nullable Set<String> defValues) {
        Set<String> returnValues;
        Object value = get(key);
        if (value instanceof Set) {
            returnValues = (Set<String>) value;
        } else {
            returnValues = new HashSet<>();
        }
        return !returnValues.isEmpty() ? returnValues : defValues;
    }

    @Override
    public int getInt(@lombok.NonNull String key, int defValue) {
        Object value = get(key);
        return (value instanceof Integer ? (Integer) value : defValue);
    }

    @Override
    public long getLong(@lombok.NonNull String key, long defValue) {
        Object value = get(key);
        return (value instanceof Long ? (Long) value : defValue);
    }

    @Override
    public float getFloat(@lombok.NonNull String key, float defValue) {
        Object value = get(key);
        return (value instanceof Float ? (Float) value : defValue);
    }

    @Override
    public boolean getBoolean(@lombok.NonNull String key, boolean defValue) {
        Object value = get(key);
        return (value instanceof Boolean ? (Boolean) value : defValue);
    }

    @Override
    public boolean contains(@lombok.NonNull String key) {
        return sharedPreferences.contains(key);
    }

    @Override
    @NonNull
    public SharedPreferences.Editor edit() {
        return new SecuredSharedPreferences.Editor(this, sharedPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        listeners.remove(listener);
    }

    private Object get(String key) {
        return get(key, true);
    }

    private Object get(@lombok.NonNull String key, boolean retry) {
        Reject.ifTrue(isKeyAlias(key), "Extract key is not allowed!");
        try {
            String encryptedValue = sharedPreferences.getString(key, null);
            if (encryptedValue != null) {

                String decryptedValue = decrypt(encryptedValue);
                if (decryptedValue == null) {
                    return null;
                }
                JSONObject value = new JSONObject(decryptedValue);
                int type = value.getInt("type");

                switch (type) {
                    case STRING_TYPE:
                        return value.getString(VALUE);
                    case INT_TYPE:
                        return value.getInt(VALUE);
                    case LONG_TYPE:
                        return value.getLong(VALUE);
                    case FLOAT_TYPE:
                        return value.getDouble(VALUE);
                    case BOOLEAN_TYPE:
                        return value.getBoolean(VALUE);
                    case STRING_SET_TYPE:
                        Set<String> stringSet = new HashSet<>();
                        JSONArray jsonArray = value.getJSONArray(VALUE);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            stringSet.add(jsonArray.getString(i));
                        }
                        return stringSet;
                    default:
                        throw new IllegalArgumentException("Invalid Data Type");
                }
            } else {
                return null;
            }
        } catch (Exception ex) {
            if (retry) {
                get(key, false);
            }
            throw new RuntimeException(ex);
        }
    }

    private boolean isKeyAlias(String key) {
        return keyAlias.equals(key);
    }

    private String decrypt(@lombok.NonNull String data) {
        try {
            return new String(encryptor.decrypt(Base64.decode(data, Base64.DEFAULT)));
        } catch (EncryptionException e) {
            //Failed to decrypt the data, reset the encryptor
            Logger.warn(TAG, "Failed to decrypt the data.");
            edit().clear().commit();
            return null;
        }
    }

    private String encrypt(byte[] value, boolean retry) {
        try {
            return Base64.encodeToString(encryptor.encrypt(value), Base64.DEFAULT);
        } catch (Exception e) {
            try {
                encryptor.reset();
                if (retry) {
                    return encrypt(value, false);
                } else {
                    throw new RuntimeException(e);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    private static final class Editor implements SharedPreferences.Editor {
        private final SecuredSharedPreferences securedSharedPreferences;
        private final SharedPreferences.Editor editor;
        private final List<String> keysChanged;
        private AtomicBoolean clearRequest = new AtomicBoolean(false);

        Editor(SecuredSharedPreferences securedSharedPreferences,
               SharedPreferences.Editor editor) {
            this.securedSharedPreferences = securedSharedPreferences;
            this.editor = editor;
            keysChanged = new CopyOnWriteArrayList<>();
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putString(@Nullable String key, @Nullable String value) {
            put(key, value, STRING_TYPE);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putStringSet(@Nullable String key,
                                                     @Nullable Set<String> values) {
            put(key, values);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putInt(@Nullable String key, int value) {
            put(key, value, INT_TYPE);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putLong(@Nullable String key, long value) {
            put(key, value, LONG_TYPE);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putFloat(@Nullable String key, float value) {
            put(key, value, FLOAT_TYPE);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putBoolean(@Nullable String key, boolean value) {
            put(key, value, BOOLEAN_TYPE);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor remove(@lombok.NonNull String key) {
            Reject.ifTrue(securedSharedPreferences.isKeyAlias(key), "Remove SecretKey is not allowed!");
            editor.remove(key);
            keysChanged.remove(key);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor clear() {
            clearRequest.set(true);
            return this;
        }

        private void preClear() {
            if (clearRequest.getAndSet(false)) {
                for (String key : securedSharedPreferences.keys()) {
                    if (!keysChanged.contains(key)
                            && !securedSharedPreferences.isKeyAlias(key)) {
                        editor.remove(key);
                    }
                }
            }
        }

        @Override
        public boolean commit() {
            preClear();
            try {
                return editor.commit();
            } finally {
                notifyListeners();
                keysChanged.clear();
            }
        }

        @Override
        public void apply() {
            preClear();
            editor.apply();
            notifyListeners();
            keysChanged.clear();
        }

        private void put(@lombok.NonNull String key, Object value, int type) {
            Reject.ifTrue(securedSharedPreferences.isKeyAlias(key), "Update SecretKey is not allowed!");
            if (value == null) {
                remove(key);
                return;
            }
            JSONObject data = new JSONObject();
            try {
                data.put(TYPE, type);
                data.put(VALUE, value);
                put(key, data.toString().getBytes());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        private void put(@lombok.NonNull String key, Set<String> value) {
            Reject.ifTrue(securedSharedPreferences.isKeyAlias(key), "Update SecretKey is not allowed!");
            if (value == null) {
                remove(key);
                return;
            }
            JSONObject data = new JSONObject();
            JSONArray content = new JSONArray();
            try {
                data.put(TYPE, STRING_SET_TYPE);
                for (String s : value) {
                    content.put(s);
                }
                data.put(VALUE, content);
                put(key, data.toString().getBytes());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        private void put(String key, byte[] value) {
            keysChanged.add(key);
            String v = securedSharedPreferences.encrypt(value, true);
            editor.putString(key, v);
        }

        private void notifyListeners() {
            for (OnSharedPreferenceChangeListener listener :
                    securedSharedPreferences.listeners) {
                for (String key : keysChanged) {
                    listener.onSharedPreferenceChanged(securedSharedPreferences, key);
                }
            }
        }
    }
}


