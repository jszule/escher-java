package com.emarsys.escher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AuthElements {

    private String algoPrefix;
    private String hashAlgo;
    private String accessKeyId;
    private String credentialDate;
    private String credentialScope;
    private List<String> signedHeaders = new ArrayList<>();
    private String signature;


    public static AuthElements parseHeader(String text) throws EscherException {

        Pattern pattern = Pattern.compile("^(?<algoPrefix>\\w+)-HMAC-(?<hashAlgo>[A-Z0-9,]+) Credential=(?<accessKeyId>[\\w\\-]+)/(?<date>\\d{8})/(?<credentialScope>[\\w\\-/]+), SignedHeaders=(?<signedHeaders>[A-Za-z\\-;]+), Signature=(?<signature>[0-9a-f]+)$");
        Matcher matcher = pattern.matcher(text);

        if (matcher.matches()) {
            AuthElements elements = new AuthElements();

            elements.algoPrefix = matcher.group("algoPrefix");
            elements.hashAlgo = matcher.group("hashAlgo");
            elements.accessKeyId = matcher.group("accessKeyId");
            elements.credentialDate = matcher.group("date");
            elements.credentialScope = matcher.group("credentialScope");
            elements.signedHeaders.addAll(Arrays.asList(matcher.group("signedHeaders").split(";")));
            elements.signature = matcher.group("signature");

            return elements;
        } else {
            throw new EscherException("Malformed authorization header");
        }

    }


    public String getAlgoPrefix() {
        return algoPrefix;
    }


    public String getHashAlgo() {
        return hashAlgo;
    }


    public String getAccessKeyId() {
        return accessKeyId;
    }


    public String getCredentialDate() {
        return credentialDate;
    }


    public String getCredentialScope() {
        return credentialScope;
    }


    public List<String> getSignedHeaders() {
        return signedHeaders;
    }


    public String getSignature() {
        return signature;
    }
}