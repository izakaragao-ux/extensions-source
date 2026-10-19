package eu.kanade.tachiyomi.extension.pt.mangamillion

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.get
import keiyoushi.network.rateLimit
import keiyoushi.source.KeiSource
import keiyoushi.utils.parseAsProto
import keiyoushi.utils.string
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.seconds

@Source
abstract class MangaMillion : KeiSource() {

    override fun OkHttpClient.Builder.configureClient(): OkHttpClient.Builder = rateLimit(permits = 5, period = 1.seconds)
        .addInterceptor(AccessTokenInterceptor())
        .addInterceptor(ImageInterceptor())

    override suspend fun getPopularManga(page: Int): MangasPage {
        val titles = fetchTitles().sortedByDescending { it.viewCount }
        return MangasPage(titles.map { it.toSManga() }, false)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val titles = fetchTitles().sortedByDescending { it.updatedAt }
        return MangasPage(titles.map { it.toSManga() }, false)
    }

    // The API has no working text search, so the full list (a single request) is filtered locally.
    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        val titles = fetchTitles()
            .filter { it.name.contains(query, ignoreCase = true) || it.author?.contains(query, ignoreCase = true) == true }
            .sortedByDescending { it.viewCount }
        return MangasPage(titles.map { it.toSManga() }, false)
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = coroutineScope {
        val details = async {
            if (fetchDetails) getDetails(manga.url) else manga
        }
        val chapterList = async {
            if (fetchChapters) getChapters(manga.url) else chapters
        }
        SMangaUpdate(details.await(), chapterList.await())
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val url = apiUrl("viewer", "translated_chapter_id" to chapter.url, "quality" to "middle")
        val viewer = client.get(url).parseAsProto<ApiResponse>().viewer!!
        return viewer.pages.mapIndexed { index, page ->
            Page(index, imageUrl = "${page.imageUrl}#${viewer.aesKey}:${viewer.aesIv}")
        }
    }

    override fun getMangaUrl(manga: SManga) = "$baseUrl/$lang/title/${manga.url}"

    override fun getChapterUrl(chapter: SChapter) = "$baseUrl/$lang/title/${chapter.memo["titleId"]!!.string}/chapter/${chapter.url}"

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.host != baseUrl.toHttpUrl().host) return null
        val id = url.pathSegments.getOrNull(url.pathSegments.indexOf("title") + 1)
            ?.takeIf { it.toIntOrNull() != null }
            ?: return null
        return getDetails(id)
    }

    private suspend fun fetchTitles(): List<TitleSummary> {
        val items = client.get(apiUrl("manga_list")).parseAsProto<ApiResponse>().mangaList!!.items
        return items.filter { lang in it.languages }.map { it.title }
    }

    private suspend fun getDetails(id: String): SManga {
        val url = apiUrl("title_detail", "original_title_id" to id)
        return client.get(url).parseAsProto<ApiResponse>().titleDetail!!.title.toSManga(id)
    }

    private suspend fun getChapters(id: String): List<SChapter> {
        val url = apiUrl("chapter_list", "original_title_id" to id, "translated_language" to lang)
        return client.get(url).parseAsProto<ApiResponse>().chapterList!!.groups
            .filter { it.type == GROUP_FREE || it.type == GROUP_SIMUL }
            .flatMap { group -> group.chapters.map { it.toSChapter(id) } }
            .reversed()
    }

    // avif_enable=false makes the API serve WebP, which every Mihon version can decode.
    private fun apiUrl(endpoint: String, vararg params: Pair<String, String>): HttpUrl = "https://${AccessTokenInterceptor.API_HOST}/api/$endpoint".toHttpUrl().newBuilder()
        .addQueryParameter("service_language", lang)
        .addQueryParameter("avif_enable", "false")
        .apply { params.forEach { (name, value) -> addQueryParameter(name, value) } }
        .build()

    companion object {
        private const val GROUP_FREE = 0
        private const val GROUP_SIMUL = 2
    }
}
