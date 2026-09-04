package com.akwam

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class Akwam : MainAPI() {
    override var mainUrl = "https://akwam.cx"
    override var name = "أكوام - Akwam"
    override val hasMainPage = true
    override var lang = "ar"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/movies?page=" to "الأفلام",
        "$mainUrl/series?page=" to "المسلسلات",
        "$mainUrl/shows?page=" to "برامج تلفزيونية"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("${request.data}$page").document
        val home = document.select("div.entry-box").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val titleElement = this.selectFirst("h3.entry-title a, h2.entry-title a") ?: return null
        val title = titleElement.text() ?: return null
        val href = fixUrl(titleElement.attr("href"))
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src"))
        
        val quality = this.selectFirst("span.quality")?.text()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.addQuality(quality)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search?q=$query").document
        return document.select("div.entry-box").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text() ?: "بدون عنوان"
        val poster = fixUrl(document.selectFirst("div.entry-image img")?.attr("src"))
        val description = document.selectFirst("div.story div.content")?.text()
        val tags = document.select("div.entry-tags a").map { it.text() }
        
        val episodes = document.select("div.episodes-list a, div.season-list a").map {
            val epHref = fixUrl(it.attr("href"))
            val epName = it.text()
            Episode(epHref, name = epName)
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val watchLink = document.selectFirst("a.btn-custom.download, a.watch-btn")?.attr("href") ?: data
        val watchDoc = app.get(watchLink).document
        
        watchDoc.select("source, iframe").forEach { element ->
            val videoUrl = element.attr("src") ?: element.attr("data-src")
            if (videoUrl.isNotEmpty()) {
                callback.addConnection(
                    name = "Akwam Direct",
                    url = fixUrl(videoUrl),
                    isM3u8 = videoUrl.contains(".m3u8")
                )
            }
        }
        return true
    }
}
