package eu.kanade.tachiyomi.extension.pt.mangamillion

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class ApiResponse(
    @ProtoNumber(22) val mangaList: MangaListResponse?,
    @ProtoNumber(50) val titleDetail: TitleDetailResponse?,
    @ProtoNumber(60) val chapterList: ChapterListResponse?,
    @ProtoNumber(70) val viewer: ViewerResponse?,
    @ProtoNumber(170) val deviceToken: DeviceTokenResponse?,
)

@Serializable
class DeviceTokenResponse(
    @ProtoNumber(1) val token: String,
)

@Serializable
class MangaListResponse(
    @ProtoNumber(1) val items: List<MangaListItem>,
)

@Serializable
class MangaListItem(
    @ProtoNumber(1) val title: TitleSummary,
    @ProtoNumber(2) val languages: List<String>,
)

@Serializable
class TitleSummary(
    @ProtoNumber(1) private val id: Int,
    @ProtoNumber(2) private val thumbnailUrl: String?,
    @ProtoNumber(3) val name: String,
    @ProtoNumber(4) val author: String?,
    @ProtoNumber(7) val viewCount: Int = 0,
    @ProtoNumber(9) val updatedAt: Long = 0,
) {
    fun toSManga() = SManga.create().apply {
        url = id.toString()
        title = name
        author = this@TitleSummary.author
        thumbnail_url = thumbnailUrl
    }
}

@Serializable
class TitleDetailResponse(
    @ProtoNumber(1) val title: ServiceTitle,
)

@Serializable
class ServiceTitle(
    @ProtoNumber(1) private val coverUrl: String?,
    @ProtoNumber(2) private val name: String,
    @ProtoNumber(3) private val authorName: String?,
    @ProtoNumber(4) private val tags: List<Tag>,
    @ProtoNumber(7) private val summary: String?,
) {
    fun toSManga(id: String) = SManga.create().apply {
        url = id
        title = name
        author = authorName
        description = summary
        genre = tags.joinToString { it.name }
        thumbnail_url = coverUrl
    }
}

@Serializable
class Tag(
    @ProtoNumber(2) val name: String,
)

@Serializable
class ChapterListResponse(
    @ProtoNumber(2) val groups: List<ChapterGroup>,
)

@Serializable
class ChapterGroup(
    @ProtoNumber(1) val type: Int = 0,
    @ProtoNumber(2) val chapters: List<ChapterInfo>,
)

@Serializable
class ChapterInfo(
    @ProtoNumber(1) private val number: String,
    @ProtoNumber(2) private val name: String,
    @ProtoNumber(3) private val id: Int,
) {
    fun toSChapter(titleId: String) = SChapter.create().apply {
        url = id.toString()
        name = this@ChapterInfo.name
        chapter_number = number.removePrefix("#").toFloatOrNull() ?: -1f
        memo = buildJsonObject {
            put("titleId", titleId)
        }
    }
}

@Serializable
class ViewerResponse(
    @ProtoNumber(1) val pages: List<ViewerPage>,
    @ProtoNumber(7) val aesKey: String,
    @ProtoNumber(8) val aesIv: String,
)

@Serializable
class ViewerPage(
    @ProtoNumber(1) val imageUrl: String,
)
