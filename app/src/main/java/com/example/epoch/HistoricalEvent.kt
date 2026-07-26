package com.example.epoch

data class HistoryResponse(val events: List<HistoricalEvent>)

data class HistoricalEvent(
    val year: Int,
    val text: String,
    val pages: List<WikiPage>
)

data class WikiPage(
    val thumbnail: Thumbnail?,
    val originalimage: Thumbnail?,
    val extract: String?,
    val content_urls: ContentUrls
)

data class Thumbnail(val source: String)

data class ContentUrls(val desktop: DesktopUrl)

data class DesktopUrl(val page: String)