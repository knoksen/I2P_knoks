package no.knoksen.i2pbrowser.i2p

object SafePreviewSanitizer {
    fun sanitize(
        contentType: String?,
        body: String?,
        maxChars: Int = MAX_BODY_PREVIEW_CHARS
    ): String {
        if (body.isNullOrBlank()) return ""
        if (!isPreviewSupported(contentType)) {
            return "Preview skipped: unsupported or binary content type."
        }

        val cleaned = if (isHtml(contentType)) {
            body
                .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
                .replace(Regex("\\son[a-z]+\\s*=\\s*(['\"]).*?\\1", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<[^>]+>"), " ")
                .decodeBasicHtmlEntities()
                .collapseWhitespace()
        } else {
            body.decodeBasicHtmlEntities().trim()
        }

        return cleaned.take(maxChars)
    }

    fun isPreviewSupported(contentType: String?): Boolean {
        val normalized = contentType?.substringBefore(";")?.trim()?.lowercase().orEmpty()
        return normalized.startsWith("text/") ||
            normalized == "application/json" ||
            normalized == "application/xml" ||
            normalized.endsWith("+json") ||
            normalized.endsWith("+xml")
    }

    private fun isHtml(contentType: String?): Boolean {
        return contentType
            ?.substringBefore(";")
            ?.trim()
            ?.equals("text/html", ignoreCase = true) == true
    }

    private fun String.decodeBasicHtmlEntities(): String {
        return replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }

    private fun String.collapseWhitespace(): String {
        return replace(Regex("\\s+"), " ").trim()
    }
}
