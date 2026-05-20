package com.spokeneasy.app.core.scorer;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * XunfeiScorer unit tests.
 *
 * NOTE: parseAndCallback() relies on the ISE SDK (Msc.jar) which requires
 * Android native libraries. Tests that verify ISE XML parsing must run as
 * Android Instrumentation tests (androidTest/) on a device/emulator.
 * This class only tests logic that doesn't need the native SDK.
 */
public class XunfeiScorerTest {

    @Test
    public void getPhonemeTip_knownCodes_returnsChineseTips() {
        XunfeiScorer scorer = new XunfeiScorer();

        assertNotNull(scorer.getPhonemeTip("th", null));
        assertTrue(scorer.getPhonemeTip("th", null).contains("θ"));

        assertNotNull(scorer.getPhonemeTip("dh", null));
        assertTrue(scorer.getPhonemeTip("dh", null).contains("ð"));

        assertNotNull(scorer.getPhonemeTip("r", null));
        assertTrue(scorer.getPhonemeTip("r", null).contains("r"));

        assertNotNull(scorer.getPhonemeTip("v", null));
        assertTrue(scorer.getPhonemeTip("v", null).contains("v"));

        assertNotNull(scorer.getPhonemeTip("iy", null));
        assertTrue(scorer.getPhonemeTip("iy", null).contains("i:"));
    }

    @Test
    public void getPhonemeTip_unknownCode_returnsNull() {
        XunfeiScorer scorer = new XunfeiScorer();

        assertNull(scorer.getPhonemeTip("zzz", null));
        assertNull(scorer.getPhonemeTip("xyz", null));
        assertNull(scorer.getPhonemeTip("", null));
    }
}
