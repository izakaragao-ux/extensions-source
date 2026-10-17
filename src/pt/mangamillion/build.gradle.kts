import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "MangaMillion"
    versionCode = 1
    contentWarning = ContentWarning.SAFE 
    libVersion = "1.6"
}
