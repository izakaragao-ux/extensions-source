package eu.kanade.tachiyomi.extension.pt.mangamillion

import eu.kanade.tachiyomi.annotations.Source
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response

@Source
class MangaMillion : HttpSource() {

    override val name = "Manga Million"
    override val baseUrl = "https://mangamillion.shueisha.co.jp"
    override val lang = "pt-BR"
    override val supportsLatest = true

    private val apiUrl = "https://api.mangamillion.shueisha.co.jp"

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("User-Agent", "Mozilla/5.0 (Android; Mobile) Chrome/120.0.0.0")

    override fun popularMangaRequest(page: Int): Request = GET("$apiUrl/v1/titles?lang=pt-BR&page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val mangas = mutableListOf<SManga>()
        return MangasPage(mangas, hasNextPage = false)
    }

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$apiUrl/v1/title/${manga.url}", headers)

    override fun mangaDetailsParse(response: Response): SManga {
        val manga = SManga.create()
        return manga
    }

    override fun chapterListRequest(manga: SManga): Request = GET("$apiUrl/v1/title/${manga.url}/chapters", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        return chapters
    }

    override fun pageListRequest(chapter: SChapter): Request = GET("$apiUrl/v1/chapter/${chapter.url}/pages", headers)

    override fun pageListParse(response: Response): List<Page> {
        val pages = mutableListOf<Page>()
        return pages
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
    override fun latestUpdatesRequest(page: Int): Request = popularMangaRequest(page)
    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = popularMangaRequest(page)
    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)
}
