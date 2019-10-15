/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.collector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import org.forgerock.android.auth.FRListener;
import org.forgerock.android.auth.Listener;
import org.json.JSONException;
import org.json.JSONObject;

import static android.telephony.TelephonyManager.*;

/**
 * Collector to collect Telephony information
 */
public class TelephonyCollector implements DeviceCollector {

    private static final int NETWORK_TYPE_GSM = 16;
    private static final int NETWORK_TYPE_TD_SCDMA = 17;
    private static final int NETWORK_TYPE_IWLAN = 18;
    private static final int NETWORK_TYPE_LTE_CA = 19;

    @Override
    public String getName() {
        return "telephony";
    }

    @Override
    public void collect(Context context, FRListener<JSONObject> listener) {
        try {
            Listener.onSuccess(listener, collect(context));
        } catch (JSONException e) {
            Listener.onException(listener, e);
        }
    }

    @SuppressLint("MissingPermission")
    public JSONObject collect(Context context) throws JSONException {

        JSONObject o = new JSONObject();

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            o.put("networkCountryIso", telephonyManager.getNetworkCountryIso());
            o.put("carrierName", telephonyManager.getNetworkOperatorName());

            /*
            o.put("networkOperator", telephonyManager.getNetworkOperator());
            o.put("simOperatorName", telephonyManager.getSimOperatorName());

            o.put("networkType", getNetworkType(telephonyManager));
            o.put("phoneType", getPhoneType(telephonyManager));
            o.put("simIsoCountryCode", telephonyManager.getSimCountryIso());
            try {
                o.put("simSerialNumber", telephonyManager.getSimSerialNumber());
                o.put("subscriberId", telephonyManager.getSubscriberId());
            } catch (SecurityException e) {
                //ignore
            }
            o.put("isRoamingNetwork", telephonyManager.isNetworkRoaming());
            */

        }

        return o;
    }

    /*
    private String getPhoneType(TelephonyManager telephonyManager) {
        return getPhoneTypeName(telephonyManager.getPhoneType());
    }

    public String getPhoneTypeName(int type) {
        switch (type) {
            case PHONE_TYPE_NONE:
                return "NONE";
            case PHONE_TYPE_CDMA:
                return "CDMA";
            case PHONE_TYPE_GSM:
                return "GSM";
            case PHONE_TYPE_SIP:
                return "SIP";
            default:
                return "UNKNOWN";
        }
    }


    private String getNetworkType(TelephonyManager telephonyManager) {
        return getNetworkTypeName(telephonyManager.getNetworkType());
    }

    private String getNetworkTypeName(int type) {
        switch (type) {
            case NETWORK_TYPE_GPRS:
                return "GPRS";
            case NETWORK_TYPE_EDGE:
                return "EDGE";
            case NETWORK_TYPE_UMTS:
                return "UMTS";
            case NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case NETWORK_TYPE_HSPA:
                return "HSPA";
            case NETWORK_TYPE_CDMA:
                return "CDMA";
            case NETWORK_TYPE_EVDO_0:
                return "CDMA - EvDo rev. 0";
            case NETWORK_TYPE_EVDO_A:
                return "CDMA - EvDo rev. A";
            case NETWORK_TYPE_EVDO_B:
                return "CDMA - EvDo rev. B";
            case NETWORK_TYPE_1xRTT:
                return "CDMA - 1xRTT";
            case NETWORK_TYPE_LTE:
                return "LTE";
            case NETWORK_TYPE_EHRPD:
                return "CDMA - eHRPD";
            case NETWORK_TYPE_IDEN:
                return "iDEN";
            case NETWORK_TYPE_HSPAP:
                return "HSPA+";
            case NETWORK_TYPE_GSM:
                return "GSM";
            case NETWORK_TYPE_TD_SCDMA:
                return "TD_SCDMA";
            case NETWORK_TYPE_IWLAN:
                return "IWLAN";
            case NETWORK_TYPE_LTE_CA:
                return "LTE_CA";
            default:
                return "UNKNOWN";
        }
    }
    */

}
