plugins {
    id("com.android.library")
    id("kotlin-android")
    id("cloudstream")
}

cloudstream {
    setEnglishName("Akwam")
    description = "إضافة أكوام للأفلام والمسلسلات والأنمي"
    authors = ["Noor & AI"]
    tvTypes = listOf("Movie", "TvSeries", "Anime")
    language = "ar"
    iconUrl = "https://akwam.cx/assets/images/logo.png"
}

dependencies {
    implementation(kotlin("stdlib"))
}
