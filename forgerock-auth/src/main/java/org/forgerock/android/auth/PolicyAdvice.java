/*
 * Copyright (c) 2019 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth;

import androidx.annotation.NonNull;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.Serializable;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.Builder;
import lombok.Getter;

/**
 * Domain object for advice. The Advice is received from resource API for Step up authentication
 */
@Builder
@Getter
public class PolicyAdvice implements Serializable {

    public static final int AUTH_LEVEL_CONDITION_ADVICE = 1;
    public static final int AUTH_SCHEME_CONDITION_ADVICE = 2;
    public static final int AUTHENTICATE_TO_REALM_CONDITION_ADVICE = 3;
    public static final int AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE = 4;
    public static final int SESSION_CONDITION_ADVICE = 5;
    public static final int TRANSACTION_CONDITION_ADVICE = 6;
    public static final int UNKNOWN = -1;

    @lombok.NonNull
    private String type;
    @lombok.NonNull
    private String value;

    @NonNull
    @Override
    public String toString() {
        return "<Advices>" +
                "<AttributeValuePair>" +
                "<Attribute name=\"" + type + "\"/>" +
                "<Value>" + value + "</Value>" +
                "</AttributeValuePair>" +
                "</Advices>";
    }

    public int getType() {
        switch (type) {
            case "AuthenticateToServiceConditionAdvice":
                return AUTHENTICATE_TO_SERVICE_CONDITION_ADVICE;
            case "TransactionConditionAdvice":
                return TRANSACTION_CONDITION_ADVICE;
            default:
                return UNKNOWN;
        }
    }


    /**
     * Parse the advice xml
     *
     * @param xml The Advice in xml form
     * @return The parsed Policy Advice
     */
    public static PolicyAdvice parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            NodeList attributes = document.getElementsByTagName("AttributeValuePair");

            for (int i = 0; i < attributes.getLength(); i++) {

                Node node = attributes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element2 = (Element) node;
                    String attribute = element2.getElementsByTagName("Attribute").item(0).getAttributes().getNamedItem("name").getNodeValue();
                    String value = element2.getElementsByTagName("Value").item(0).getFirstChild().getNodeValue();
                    return PolicyAdvice.builder()
                            .type(attribute)
                            .value(value).build();
                }
            }
            throw new IllegalArgumentException("Failed to parse policy advice");
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
