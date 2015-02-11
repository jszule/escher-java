package com.emarsys.escher;


import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Escher {

    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private String credentialScope;
    private String algoPrefix = "ESR";
    private String vendorKey = "Escher";
    private String hashAlgo = "SHA256";
    private Date currentTime = new Date();
    private String authHeaderName = "X-Escher-Auth";
    private String dateHeaderName = "X-Escher-Date";
    private int clockSkew = 900;

    public Escher(String credentialScope) {
        this.credentialScope = credentialScope;
    }


    public EscherRequest signRequest(EscherRequest request, String accessKeyId, String secret, List<String> signedHeaders) throws EscherException {
        Config config = createConfig();
        Helper helper = new Helper(config);

        helper.addDateHeader(request);

        String signature = calculateSignature(request, helper, secret);
        String authHeader = helper.calculateAuthHeader(accessKeyId, credentialScope, signedHeaders, signature);

        helper.addAuthHeader(request, authHeader);

        return request;
    }


    public String presignUrl(String url, String accessKeyId, String secret, int expires) throws EscherException{
        try {
            Config config = createConfig();
            Helper helper = new Helper(config);

            URI uri = new URI(url);
            URIBuilder uriBuilder = new URIBuilder(uri);

            Map<String, String> params = helper.calculateSigningParams(accessKeyId, credentialScope, expires);
            params.forEach((key, value) -> uriBuilder.addParameter("X-" + vendorKey + "-" + key, value));

            EscherRequest request = new PresignUrlDummyEscherRequest(uriBuilder.build());

            String signature = calculateSignature(request, helper, secret);

            uriBuilder.addParameter("X-" + vendorKey + "-" + "Signature", signature);

            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new EscherException(e);
        }
    }


    private String calculateSignature(EscherRequest request, Helper helper, String secret) throws EscherException {
        String canonicalizedRequest = helper.canonicalize(request);
        String stringToSign = helper.calculateStringToSign(credentialScope, canonicalizedRequest);
        byte[] signingKey = helper.calculateSigningKey(secret, credentialScope);
        return helper.calculateSignature(signingKey, stringToSign);
    }


    private Config createConfig() {
        return Config.create()
                .setAlgoPrefix(algoPrefix)
                .setHashAlgo(hashAlgo)
                .setDateHeaderName(dateHeaderName)
                .setAuthHeaderName(authHeaderName)
                .setDate(currentTime);
    }


    public String authenticate(EscherRequest request, Map<String, String> keyDb) throws EscherException {
        EscherRequest.Header hostHeader = null;
        EscherRequest.Header authHeader = null;
        EscherRequest.Header dateHeader = null;

        for (EscherRequest.Header header : request.getRequestHeaders()) {
            String fieldName = header.getFieldName().replace('_', '-');
            if (fieldName.equalsIgnoreCase("host")) hostHeader = header;
            if (fieldName.equalsIgnoreCase(authHeaderName)) authHeader = header;
            if (fieldName.equalsIgnoreCase(dateHeaderName)) dateHeader = header;
        }

        if (hostHeader == null) {
            throw new EscherException("Missing header: host");
        }

        if (dateHeader == null) {
            throw new EscherException("Missing header: " + dateHeaderName);
        }

        if (authHeader == null) {
            throw new EscherException("Missing header: " + authHeaderName);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat(Config.LONG_DATE_FORMAT);
        dateFormat.setTimeZone(Config.TIMEZONE);
        try {
            dateFormat.parse(dateHeader.getFieldValue());
        } catch (ParseException e) {
            throw new EscherException("Invalid date format");
        }

        AuthHeader authHeader1 = AuthHeader.parse(authHeader.getFieldValue());
        return authHeader1.getAccessKeyId();
    }


    public Escher setAlgoPrefix(String algoPrefix) {
        this.algoPrefix = algoPrefix;
        return this;
    }


    public Escher setVendorKey(String vendorKey) {
        this.vendorKey = vendorKey;
        return this;
    }


    public Escher setHashAlgo(String hashAlgo) {
        this.hashAlgo = hashAlgo;
        return this;
    }


    public Escher setCurrentTime(Date currentTime) {
        this.currentTime = currentTime;
        return this;
    }


    public Escher setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
        return this;
    }


    public Escher setDateHeaderName(String dateHeaderName) {
        this.dateHeaderName = dateHeaderName;
        return this;
    }


    public Escher setClockSkew(int clockSkew) {
        this.clockSkew = clockSkew;
        return this;
    }
}
