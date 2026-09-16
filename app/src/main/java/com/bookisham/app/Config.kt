package com.bookisham.app

/** The few constants the whole app shares. Mirrors src/lib/config.ts on the server. */
object Config {
    const val APP_NAME = "Bookisham"

    /** Where the server lives. Set bookisham.baseUrl in gradle.properties or local.properties. */
    val BASE_URL: String = BuildConfig.BASE_URL.trimEnd('/')

    /** Public contact details for visitors who have no account yet. */
    val WHATSAPP: String = BuildConfig.WHATSAPP
    val CONTACT_EMAIL: String = BuildConfig.CONTACT_EMAIL

    const val SUPPORT_EMAIL = "krishnavijay.gkv@gmail.com"
    const val POWERED_BY_NAME = "arkhins.com"
    const val POWERED_BY_URL = "https://arkhins.com"

    /** The admin panel is the web app; the phone opens it in the browser. */
    val ADMIN_URL: String get() = "$BASE_URL/admin"
}
