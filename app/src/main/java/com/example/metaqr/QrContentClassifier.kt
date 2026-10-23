package com.example.metaqr

/** Only HTTPS image URLs are embedded on the glasses. Unknown content stays as text. */
object QrContentClassifier {
    fun isWebUrl(value: String): Boolean = value.startsWith("https://", true) || value.startsWith("http://", true)
    fun directImageUrl(value: String): String? {
        if (!value.startsWith("https://", true)) return null
        val path = value.substringBefore('?').substringBefore('#').lowercase()
        return value.takeIf { path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.endsWith(".webp") || path.endsWith(".gif") }
    }
}
