package com.example.metaqr

import org.junit.Assert.*
import org.junit.Test

class QrContentClassifierTest {
    @Test fun recognizesSecureImages() {
        assertEquals("https://example.com/a.png?x=1", QrContentClassifier.directImageUrl("https://example.com/a.png?x=1"))
        assertNull(QrContentClassifier.directImageUrl("http://example.com/a.png"))
        assertNull(QrContentClassifier.directImageUrl("https://example.com/document.pdf"))
    }
    @Test fun preservesOtherTypesAsText() {
        assertFalse(QrContentClassifier.isWebUrl("WIFI:T:WPA;S:test;P:secret;;"))
        assertTrue(QrContentClassifier.isWebUrl("https://example.com"))
    }
}
