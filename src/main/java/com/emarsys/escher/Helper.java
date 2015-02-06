package com.emarsys.escher;

import org.apache.http.client.utils.URLEncodedUtils;

import javax.xml.bind.DatatypeConverter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BinaryOperator;

class Helper {

    private static final char NEW_LINE = '\n';


    public static String canonicalize(Request request) throws EscherException {
        return request.getHttpMethod() + NEW_LINE +
                request.getURI().getPath() + NEW_LINE +
                canonicalizeQueryParameters(request) + NEW_LINE +
                canonicalizeHeaders(request.getRequestHeaders()) + NEW_LINE +
                NEW_LINE +
                signedHeaders(request.getRequestHeaders()) + NEW_LINE +
                Hmac.hash(request.getBody());
    }


    private static String canonicalizeQueryParameters(Request request) {
        return URLEncodedUtils.parse(request.getURI(), "utf-8")
                .stream()
                .map(entry -> entry.getName() + "=" + URLEncoder.encode(entry.getValue()))
                .sorted()
                .reduce(byJoiningWith('&'))
                .orElseGet(() -> "");
    }


    private static String canonicalizeHeaders(List<Request.Header> headers) {
        return headers
                .stream()
                .map(header -> header.getFieldName().toLowerCase() + ":" + header.getFieldValue().trim())
                .sorted()
                .reduce(byJoiningWith(NEW_LINE))
                .orElseGet(() -> "");
    }


    private static String signedHeaders(List<Request.Header> headers) {
        return headers
                .stream()
                .map(header -> header.getFieldName().toLowerCase())
                .sorted()
                .reduce(byJoiningWith(';'))
                .orElseGet(() -> "");
    }


    private static BinaryOperator<String> byJoiningWith(char separator) {
        return (s1, s2) -> s1 + separator + s2;
    }


    public static String calculateStringToSign(String credentialScope, String canonicalizedRequest, Date date, String hashAlgo, String algoPrefix) throws EscherException{
        return algorithm(algoPrefix, hashAlgo) + NEW_LINE
                + longDate(date) + NEW_LINE
                + shortDate(date) + "/" + credentialScope + NEW_LINE
                + Hmac.hash(canonicalizedRequest);
    }


    public static byte[] calculateSigningKey(String secret, Date date, String credentialScope, String hashAlgo, String algoPrefix) throws EscherException{
        byte[] key = Hmac.sign(hashAlgo, (algoPrefix + secret), shortDate(date));

        for (String credentialPart : credentialScope.split("/")) {
            key = Hmac.sign(hashAlgo, key, credentialPart);
        }

        return key;
    }


    public static String longDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }


    private static String shortDate(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }


    public static String calculateAuthHeader(String accessKeyId, Date date, String credentialScope, String hashAlgo, String algoPrefix, List<String> signedHeaders, String signature) {
        return algorithm(algoPrefix, hashAlgo) +
                " Credential=" + credentials(accessKeyId, date, credentialScope) +
                ", SignedHeaders=" + signedHeaders.stream().reduce((s1, s2) -> s1 + ";" + s2).get().toLowerCase() +
                ", Signature=" + signature;
    }


    public static String calculateSignature(String hashAlgo, byte[] signingKey, String stringToSign) throws EscherException {
        return DatatypeConverter.printHexBinary(Hmac.sign(hashAlgo, signingKey, stringToSign)).toLowerCase();
    }


    private static String credentials(String accessKeyId, Date date, String credentialScope) {
        return accessKeyId + "/" + shortDate(date) + "/" + credentialScope;
    }


    public static Map<String, String> calculateSigningParams(String algoPrefix, String hashAlgo, String accessKeyId, Date date, String credentialScope, int expires) {
        Map<String, String> params = new TreeMap<>();
        params.put("SignedHeaders", "host");
        params.put("Expires", Integer.toString(expires));
        params.put("Algorithm", algorithm(algoPrefix, hashAlgo));
        params.put("Credentials", credentials(accessKeyId, date, credentialScope));
        params.put("Date", longDate(date));

        return params;
    }


    private static String algorithm(String algoPrefix, String hashAlgo) {
        return algoPrefix + "-HMAC-" + hashAlgo;
    }

}
