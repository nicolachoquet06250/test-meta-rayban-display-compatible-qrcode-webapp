package com.example.metaqr

import android.graphics.ImageFormat
import android.media.Image
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import java.nio.ByteBuffer

/** VideoFrame default decoded format is a packed YUV 4:2:0 buffer.
 * Only the luminance plane is required for QR decoding; padding is rejected.
 */
object QrFrameDecoder {
    fun luminance(buffer: ByteBuffer, width: Int, height: Int): ByteArray {
        require(width > 0 && height > 0 && width.toLong() * height <= 16_777_216L)
        val count = width * height
        val copy = buffer.asReadOnlyBuffer()
        copy.rewind()
        require(copy.remaining() >= count) { "Frame buffer shorter than luminance plane" }
        return ByteArray(count).also { copy.get(it) }
    }

    // Synchronous decoding is deliberately executed on Dispatchers.Default, never the UI thread.
    fun scan(buffer: ByteBuffer, width: Int, height: Int): String? {
        val y = luminance(buffer, width, height)
        // NV21 layout: Y plane followed by interleaved chroma, neutral UV since QR is luminance-only.
        val nv21 = ByteArray(width * height * 3 / 2) { 128.toByte() }
        y.copyInto(nv21)
        val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        val scanner = BarcodeScanning.getClient(options)
        return try {
            val image = InputImage.fromByteArray(nv21, width, height, 0, ImageFormat.NV21)
            Tasks.await(scanner.process(image)).firstNotNullOfOrNull { it.rawValue }
        } finally { scanner.close() }
    }
}
