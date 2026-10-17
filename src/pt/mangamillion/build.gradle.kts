import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "MangaMillion"
    versionCode = 1
    contentWarning = ContentWarning.SAFE 

    source {
        lang = "pt-BR"
        className = "eu.kanade.tachiyomi.extension.pt.mangamillion.MangaMillion"
        baseUrl = "https://mangamillion.shueisha.co.jp/pt-BR"
    }
}
