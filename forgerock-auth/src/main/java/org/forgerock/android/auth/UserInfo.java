/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import lombok.Builder;
import lombok.Getter;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class hold the response of OpenId Connect userinfo endpoint
 */
@Builder
@Getter
public class UserInfo {

    //profile scope
    private String name;
    private String familyName;
    private String givenName;
    private String middleName;
    private String nickName;
    private String preferredUsername;
    private URL profile;
    private URL picture;
    private URL website;
    private String gender;
    private Date birthDate;
    private String zoneInfo;
    private String locale;
    private Long updateAt;
    private String sub;
    //email scope
    private String email;
    private Boolean emailVerified;

    //phone scope
    private String phoneNumber;
    private Boolean phoneNumberVerified;

    //address scope
    private Address address;

    private JSONObject raw;

    @Getter
    @Builder
    public static class Address {
        private String formatted;
        private String streetAddress;
        private String locality;
        private String region;
        private String postalCode;
        private String country;
    }

    static UserInfo unmarshal(JSONObject jsonObject) {

        UserInfo.UserInfoBuilder builder = UserInfo.builder()
                .sub(jsonObject.optString("sub"))
                .name(jsonObject.optString("name"))
                .familyName(jsonObject.optString("family_name"))
                .givenName(jsonObject.optString("given_name"))
                .middleName(jsonObject.optString("middle_name"))
                .nickName(jsonObject.optString("nickname"))
                .preferredUsername(jsonObject.optString("preferred_username"))
                .profile(toURL(jsonObject.optString("profile")))
                .picture(toURL(jsonObject.optString("picture")))
                .website(toURL(jsonObject.optString("website")))
                .gender(jsonObject.optString("gender"))
                .birthDate(toDate(jsonObject.optString("birthdate")))
                .zoneInfo(jsonObject.optString("zoneinfo"))
                .locale(jsonObject.optString("locale"))
                .updateAt(jsonObject.optLong("updated_at"))
                .email(jsonObject.optString("email"))
                .emailVerified(jsonObject.optBoolean("email_verified"))
                .phoneNumber(jsonObject.optString("phone_number"))
                .phoneNumberVerified(jsonObject.optBoolean("phone_number_verified"))
                .raw(jsonObject);


        JSONObject addressObj = jsonObject.optJSONObject("address");
        if (addressObj != null) {
            Address address = Address.builder()
                    .formatted(addressObj.optString("formatted"))
                    .streetAddress(addressObj.optString("street_address"))
                    .locality(addressObj.optString("locality"))
                    .region(addressObj.optString("region"))
                    .postalCode(addressObj.optString("postal_code"))
                    .country(addressObj.optString("country"))
                    .formatted(addressObj.optString("formatted"))
                    .build();
            builder.address(address);
        }
        return builder.build();
    }

    private static URL toURL(String url) {
        if (url != null) {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return null;
    }

    private static Date toDate(String date) {

        if (date != null) {
            String pattern = "yyyy-MM-dd";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            try {
                return simpleDateFormat.parse(date);
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

}

