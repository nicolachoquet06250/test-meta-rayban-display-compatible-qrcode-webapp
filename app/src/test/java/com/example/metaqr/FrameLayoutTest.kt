package com.example.metaqr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer

class FrameLayoutTest {
    @Test fun copiesLumaWithoutConsumingInput() {
        val bytes = ByteBuffer.wrap(byteArrayOf(12, 34, 56, 78, 9, 9))
        assertEquals(listOf(12, 34, 56, 78), QrFrameDecoder.luminance(bytes, 2, 2).map { it.toInt() })
        assertEquals(0, bytes.position())
    }
    @Test fun rejectsTruncatedFrame() {
        assertThrows(IllegalArgumentException::class.java) { QrFrameDecoder.luminance(ByteBuffer.allocate(2), 2, 2) }
    }
}
