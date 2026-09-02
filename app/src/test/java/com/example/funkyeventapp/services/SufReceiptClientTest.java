package com.example.funkyeventapp.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URI;

public class SufReceiptClientTest {
    @Test public void acceptsOnlyExactHttpsSufHost() throws Exception {
        assertTrue(SufReceiptClient.isTrustedSufUrl(
                new URI("https://suf.purs.gov.rs/v/?vl=receipt-data")));
        assertTrue(SufReceiptClient.isTrustedSufUrl(
                new URI("https://SUF.PURS.GOV.RS:443/v/?vl=receipt-data")));

        assertFalse(SufReceiptClient.isTrustedSufUrl(
                new URI("http://suf.purs.gov.rs/v/?vl=receipt-data")));
        assertFalse(SufReceiptClient.isTrustedSufUrl(
                new URI("https://suf.purs.gov.rs.evil.example/v/?vl=receipt-data")));
        assertFalse(SufReceiptClient.isTrustedSufUrl(
                new URI("https://user@suf.purs.gov.rs/v/?vl=receipt-data")));
        assertFalse(SufReceiptClient.isTrustedSufUrl(
                new URI("https://suf.purs.gov.rs:8443/v/?vl=receipt-data")));
    }
}
