package com.datingapp.auth.model;

import lombok.Data;

@Data
public class ApplePublicKey {
    private String kty;
    private String kid;
    private String use;
    private String alg;
    private String n;  // modulus
    private String e;  // exponent
} 