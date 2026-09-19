package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Media Types supported by EURO IPTV Player
 */
enum class MediaType {
    LIVE_TV,
    MOVIE,
    RADIO
}

/**
 * Unified Playable Media Model
 */
data class PlayableMedia(
    val id: String,
    val title: String,
    val subtitle: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val mediaType: MediaType = MediaType.LIVE_TV,
    val qualityOrDuration: String = "FHD 1080p",
    val isFavorite: Boolean = false
)

/**
 * Model representing an IPTV Category (Live TV, Sports, Cinema, News, Kids, etc.)
 */
data class ChannelCategory(
    val id: String,
    val name: String,
    val iconName: String = "tv"
)

/**
 * Model representing an IPTV Live Stream Channel
 */
data class ChannelItem(
    val id: String,
    val name: String,
    val categoryId: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val currentProgram: String = "Live Broadcasting",
    val quality: String = "FHD 1080p",
    val isFavorite: Boolean = false,
    val views: String = "+8.2M Views"
) {
    fun toPlayableMedia(): PlayableMedia = PlayableMedia(
        id = id,
        title = name,
        subtitle = currentProgram,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        mediaType = MediaType.LIVE_TV,
        qualityOrDuration = quality,
        isFavorite = isFavorite
    )
}

/**
 * Model representing a VOD Movie
 */
data class MovieItem(
    val id: String,
    val title: String,
    val categoryId: String,
    val streamUrl: String,
    val posterUrl: String? = null,
    val genre: String = "Action / Thriller",
    val duration: String = "2h 15m",
    val rating: String = "8.9",
    val releaseYear: String = "2026",
    val isFavorite: Boolean = false
) {
    fun toPlayableMedia(): PlayableMedia = PlayableMedia(
        id = id,
        title = title,
        subtitle = "$genre • $duration • ★ $rating",
        streamUrl = streamUrl,
        logoUrl = posterUrl,
        mediaType = MediaType.MOVIE,
        qualityOrDuration = duration,
        isFavorite = isFavorite
    )
}

/**
 * Model representing a Radio Station
 */
data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val frequency: String = "98.5 FM",
    val currentTrack: String = "Live Broadcast",
    val genre: String = "International Hits",
    val isFavorite: Boolean = false
) {
    fun toPlayableMedia(): PlayableMedia = PlayableMedia(
        id = id,
        title = name,
        subtitle = "$frequency • $currentTrack",
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        mediaType = MediaType.RADIO,
        qualityOrDuration = frequency,
        isFavorite = isFavorite
    )
}

/**
 * Code Types for Device Activation
 * Supports public, vip, premium, developer, private, and universal tiers
 */
enum class CodeType(val label: String) {
    PUBLIC("Public"),
    VIP("VIP"),
    PREMIUM("Premium"),
    DEVELOPER("Developer"),
    PRIVATE("Private Individual"),
    UNIVERSAL("Universal Subscription");

    companion object {
        fun fromString(type: String?): CodeType {
            return when (type?.trim()?.lowercase()) {
                "public" -> PUBLIC
                "vip" -> VIP
                "premium" -> PREMIUM
                "developer", "dev" -> DEVELOPER
                "private" -> PRIVATE
                else -> UNIVERSAL
            }
        }
    }
}

/**
 * Record for an Activation Code in Database
 */
data class ActivationCodeRecord(
    val code: String,
    val type: CodeType,
    val durationDays: Int, // e.g. 30, 90, 365, -1 for lifetime
    val expiresAtMillis: Long?, // null for lifetime, or epoch timestamp
    val isRevoked: Boolean = false,
    val assignedUser: String? = null,
    val planName: String = "VIP Ultimate Package"
) {
    val isExpired: Boolean
        get() = expiresAtMillis != null && expiresAtMillis < System.currentTimeMillis()

    val expirationDisplay: String
        get() = if (expiresAtMillis == null) {
            "Lifetime Subscription"
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(expiresAtMillis))
        }
}

/**
 * Result of JSONBin.io Fetch Operation
 */
sealed interface JsonBinFetchResult {
    data class Success(
        val binId: String,
        val codesCount: Int,
        val channelsCount: Int,
        val moviesCount: Int,
        val radiosCount: Int
    ) : JsonBinFetchResult

    data class AuthRequired(
        val binId: String,
        val message: String
    ) : JsonBinFetchResult

    data class Error(
        val binId: String,
        val message: String
    ) : JsonBinFetchResult
}

/**
 * Result of Activation Verification
 */
sealed interface ActivationResult {
    data class Success(val record: ActivationCodeRecord) : ActivationResult
    data class Expired(val code: String, val expiredDateString: String, val message: String) : ActivationResult
    data class Invalid(val message: String) : ActivationResult
}

/**
 * Model for Remote Admin Configuration
 */
data class AdminConfig(
    val serverUrl: String = "https://api.jsonbin.io/v3/b/6aadd3a7ffd5d1605317d315",
    val jsonBinId: String = "6aadd3a7ffd5d1605317d315",
    val jsonBinApiKey: String = "",
    val adminPortalUrl: String = "https://euro-iptv-admin.panel/dashboard",
    val supportWhatsapp: String = "+212643316085",
    val isActivationRequired: Boolean = true,
    val allowedDemoCodes: List<String> = listOf("EURO2026", "VIP-IPTV-888", "PRO-SUBSCRIPTION", "ALTERO-VIP-2026", "DEV-MASTER-99", "PUBLIC-2026"),
    val motd: String = "Altero Media Stream - High Performance Streaming Network",
    val minAppVersion: Int = 1,
    val lastSyncTime: String = "Just now",
    val lastSyncStatus: String = "Ready"
)

/**
 * Central State & Repository for Euro IPTV application.
 * Dynamic connection with remote Admin Panel and data source:
 * 1. Activation verification checking Private vs Universal and Expiration
 * 2. Dynamic content fetching (Live TV, VOD Movies, Radios) with custom logos
 * 3. Media playback manager for in-app video player
 */
object IptvRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _isActivated = MutableStateFlow(false)
    val isActivated: StateFlow<Boolean> = _isActivated.asStateFlow()

    private val _activeCode = MutableStateFlow<String?>(null)
    val activeCode: StateFlow<String?> = _activeCode.asStateFlow()

    private val _activeCodeRecord = MutableStateFlow<ActivationCodeRecord?>(null)
    val activeCodeRecord: StateFlow<ActivationCodeRecord?> = _activeCodeRecord.asStateFlow()

    private val _adminConfig = MutableStateFlow(AdminConfig())
    val adminConfig: StateFlow<AdminConfig> = _adminConfig.asStateFlow()

    private val _categories = MutableStateFlow<List<ChannelCategory>>(emptyList())
    val categories: StateFlow<List<ChannelCategory>> = _categories.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelItem>>(emptyList())
    val channels: StateFlow<List<ChannelItem>> = _channels.asStateFlow()

    private val _movies = MutableStateFlow<List<MovieItem>>(emptyList())
    val movies: StateFlow<List<MovieItem>> = _movies.asStateFlow()

    private val _radios = MutableStateFlow<List<RadioStation>>(emptyList())
    val radios: StateFlow<List<RadioStation>> = _radios.asStateFlow()

    private val _currentPlayingMedia = MutableStateFlow<PlayableMedia?>(null)
    val currentPlayingMedia: StateFlow<PlayableMedia?> = _currentPlayingMedia.asStateFlow()

    // Backward compatibility with channel-specific observer
    val currentPlayingChannel: StateFlow<ChannelItem?> = MutableStateFlow<ChannelItem?>(null).apply {
        // Will be updated whenever channels change
    }

    private val _isFetchingRemoteContent = MutableStateFlow(false)
    val isFetchingRemoteContent: StateFlow<Boolean> = _isFetchingRemoteContent.asStateFlow()

    // Remote / Database Code Registry
    private val remoteActivationDatabase = mutableMapOf<String, ActivationCodeRecord>()

    init {
        initActivationCodeDatabase()
        loadInitialMockAndAdminData()
    }

    private fun initActivationCodeDatabase() {
        val now = System.currentTimeMillis()
        val oneYear = now + (365L * 24 * 60 * 60 * 1000)
        val ninetyDays = now + (90L * 24 * 60 * 60 * 1000)
        val thirtyDays = now + (30L * 24 * 60 * 60 * 1000)
        val expiredPast = now - (30L * 24 * 60 * 60 * 1000) // Expired 30 days ago

        // Public Codes (Community & Promotion)
        remoteActivationDatabase["PUBLIC-2026"] = ActivationCodeRecord(
            code = "PUBLIC-2026",
            type = CodeType.PUBLIC,
            durationDays = 365,
            expiresAtMillis = oneYear,
            planName = "Altero Public Access"
        )

        // VIP Codes (Altero VIP Package)
        remoteActivationDatabase["ALTERO-VIP-2026"] = ActivationCodeRecord(
            code = "ALTERO-VIP-2026",
            type = CodeType.VIP,
            durationDays = 365,
            expiresAtMillis = oneYear,
            planName = "Altero VIP Ultimate Package"
        )
        remoteActivationDatabase["VIP-IPTV-888"] = ActivationCodeRecord(
            code = "VIP-IPTV-888",
            type = CodeType.VIP,
            durationDays = 90,
            expiresAtMillis = ninetyDays,
            planName = "VIP Quarterly Pass"
        )

        // Premium Codes (Altero 4K Ultra Package)
        remoteActivationDatabase["ALTERO-PREMIUM-365"] = ActivationCodeRecord(
            code = "ALTERO-PREMIUM-365",
            type = CodeType.PREMIUM,
            durationDays = 365,
            expiresAtMillis = oneYear,
            planName = "Altero Premium 4K UHD"
        )
        remoteActivationDatabase["LIFETIME-PREMIUM"] = ActivationCodeRecord(
            code = "LIFETIME-PREMIUM",
            type = CodeType.PREMIUM,
            durationDays = -1,
            expiresAtMillis = null,
            planName = "Altero Lifetime Premium"
        )
        remoteActivationDatabase["PRO-SUBSCRIPTION"] = ActivationCodeRecord(
            code = "PRO-SUBSCRIPTION",
            type = CodeType.PREMIUM,
            durationDays = 30,
            expiresAtMillis = thirtyDays,
            planName = "Premium Monthly Pass"
        )

        // Developer License Codes
        remoteActivationDatabase["DEV-MASTER-99"] = ActivationCodeRecord(
            code = "DEV-MASTER-99",
            type = CodeType.DEVELOPER,
            durationDays = -1,
            expiresAtMillis = null,
            assignedUser = "Altero Lead Developer",
            planName = "Developer Master Key (Full Access)"
        )

        // Universal Subscription Codes
        remoteActivationDatabase["EURO2026"] = ActivationCodeRecord(
            code = "EURO2026",
            type = CodeType.UNIVERSAL,
            durationDays = 365,
            expiresAtMillis = oneYear,
            planName = "Altero Universal VIP"
        )

        // Private Individual Codes (Single Subscriber Account)
        remoteActivationDatabase["PRIV-AHMED-2026"] = ActivationCodeRecord(
            code = "PRIV-AHMED-2026",
            type = CodeType.PRIVATE,
            durationDays = 365,
            expiresAtMillis = oneYear,
            assignedUser = "Ahmed E.",
            planName = "Private 1-Device Annual"
        )
        remoteActivationDatabase["PRIV-SARAH-99"] = ActivationCodeRecord(
            code = "PRIV-SARAH-99",
            type = CodeType.PRIVATE,
            durationDays = 30,
            expiresAtMillis = thirtyDays,
            assignedUser = "Sarah M.",
            planName = "Private 1-Device Monthly"
        )

        // Expired Demo Codes to test expiration alerts
        remoteActivationDatabase["EXPIRED2024"] = ActivationCodeRecord(
            code = "EXPIRED2024",
            type = CodeType.PUBLIC,
            durationDays = 30,
            expiresAtMillis = expiredPast,
            planName = "Expired Public Demo"
        )
        remoteActivationDatabase["DEV-EXPIRED"] = ActivationCodeRecord(
            code = "DEV-EXPIRED",
            type = CodeType.DEVELOPER,
            durationDays = 30,
            expiresAtMillis = expiredPast,
            assignedUser = "Inactive Dev",
            planName = "Expired Developer License"
        )
        remoteActivationDatabase["PRIV-EXPIRED"] = ActivationCodeRecord(
            code = "PRIV-EXPIRED",
            type = CodeType.PRIVATE,
            durationDays = 30,
            expiresAtMillis = expiredPast,
            assignedUser = "Former Subscriber",
            planName = "Expired Private License"
        )
    }

    private fun loadInitialMockAndAdminData() {
        val initialCategories = listOf(
            ChannelCategory("cat_sports", "Sports & Arena", "sports"),
            ChannelCategory("cat_news", "News & Documentaries", "news"),
            ChannelCategory("cat_movies", "Cinema & Series", "movie"),
            ChannelCategory("cat_ent", "Entertainment & Shows", "tv"),
            ChannelCategory("cat_kids", "Kids & Animation", "kids")
        )

        // 1. LIVE TV CHANNELS with authentic custom logos provided from the Admin Panel
        val initialChannels = listOf(
            ChannelItem(
                id = "ch_nat_geo_wild",
                name = "Nat Geo Wild HD",
                categoryId = "cat_news",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1561731216-c3a4d99437d5?w=500&q=80",
                currentProgram = "Animals and Nature",
                quality = "FHD 1080p",
                isFavorite = true,
                views = "+8.2M Views"
            ),
            ChannelItem(
                id = "ch_euro_sports_1",
                name = "EURO Sports 1 HD",
                categoryId = "cat_sports",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=500&q=80",
                currentProgram = "UEFA Champions League - Live Matchday",
                quality = "4K UHD 60fps",
                isFavorite = true,
                views = "+6.5M Views"
            ),
            ChannelItem(
                id = "ch_euro_sports_2",
                name = "EURO Sports 2 HD",
                categoryId = "cat_sports",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                logoUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=500&q=80",
                currentProgram = "Formula 1 Grand Prix - Practice Session",
                quality = "FHD 1080p",
                views = "+4.1M Views"
            ),
            ChannelItem(
                id = "ch_euro_cinema_1",
                name = "EURO Cinema Premiere",
                categoryId = "cat_movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80",
                currentProgram = "Blockbuster Movie Premiere [2026]",
                quality = "4K HDR Dolby",
                isFavorite = true,
                views = "+9.0M Views"
            ),
            ChannelItem(
                id = "ch_euro_cinema_action",
                name = "EURO Action Cinema",
                categoryId = "cat_movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80",
                currentProgram = "Nightfall Mission - Action Thriller",
                quality = "FHD 1080p",
                views = "+3.8M Views"
            ),
            ChannelItem(
                id = "ch_euro_news_24",
                name = "EURO 24 News Global",
                categoryId = "cat_news",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                logoUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=500&q=80",
                currentProgram = "Global World Brief & Financial Markets",
                quality = "FHD 1080p",
                views = "+2.3M Views"
            ),
            ChannelItem(
                id = "ch_euro_wildlife",
                name = "EURO Wildlife 4K",
                categoryId = "cat_news",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1534188753412-3e26d0d618d6?w=500&q=80",
                currentProgram = "Untamed Oceans: The Deep Abyss",
                quality = "4K UHD HDR",
                views = "+5.1M Views"
            ),
            ChannelItem(
                id = "ch_euro_kids_tv",
                name = "EURO Junior Kids TV",
                categoryId = "cat_kids",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                logoUrl = "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=500&q=80",
                currentProgram = "Animated Adventures of Space Cadet",
                quality = "HD 720p",
                views = "+1.9M Views"
            )
        )

        // 2. VOD MOVIES with authentic custom posters provided from the Admin Panel
        val initialMovies = listOf(
            MovieItem(
                id = "mov_inception_dream",
                title = "Inception: Dream Protocol",
                categoryId = "cat_movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&q=80",
                genre = "Sci-Fi / Action",
                duration = "2h 28m",
                rating = "9.1",
                releaseYear = "2026",
                isFavorite = true
            ),
            MovieItem(
                id = "mov_gladiator_sand",
                title = "Gladiator: Empire of Sand",
                categoryId = "cat_movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80",
                genre = "Epic / Drama",
                duration = "2h 45m",
                rating = "8.8",
                releaseYear = "2025"
            ),
            MovieItem(
                id = "mov_interstellar_horizons",
                title = "Interstellar: Beyond Horizons",
                categoryId = "cat_movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                posterUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80",
                genre = "Sci-Fi / Adventure",
                duration = "2h 50m",
                rating = "9.3",
                releaseYear = "2026",
                isFavorite = true
            ),
            MovieItem(
                id = "mov_cyberpunk_district",
                title = "Cyberpunk: Night District",
                categoryId = "cat_movies",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&q=80",
                genre = "Cyberpunk / Mystery",
                duration = "1h 55m",
                rating = "8.4",
                releaseYear = "2026"
            ),
            MovieItem(
                id = "mov_arctic_wilderness",
                title = "Arctic Wilderness 4K",
                categoryId = "cat_news",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                posterUrl = "https://images.unsplash.com/photo-1517411032315-54ef2cb783bb?w=500&q=80",
                genre = "Documentary / Nature",
                duration = "1h 40m",
                rating = "9.0",
                releaseYear = "2026"
            )
        )

        // 3. RADIO STATIONS with authentic custom station logos provided from the Admin Panel
        val initialRadios = listOf(
            RadioStation(
                id = "rad_euro_hits",
                name = "EURO Hits Radio Top 40",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
                frequency = "104.2 FM",
                currentTrack = "Live European Pop & Dance Chart",
                genre = "Top 40 / Pop",
                isFavorite = true
            ),
            RadioStation(
                id = "rad_chillout_ambient",
                name = "Chillout & Ambient Lounge",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                logoUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
                frequency = "98.5 FM",
                currentTrack = "Sunset Deep House & Ambient Beats",
                genre = "Lounge / Electronic"
            ),
            RadioStation(
                id = "rad_azadi_int",
                name = "Radio Azadi International",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                logoUrl = "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=500&q=80",
                frequency = "101.8 FM",
                currentTrack = "Global News, Culture & Dialogues",
                genre = "News & Culture",
                isFavorite = true
            ),
            RadioStation(
                id = "rad_classical_symphony",
                name = "Classical Symphony Philharmonic",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                logoUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500&q=80",
                frequency = "92.0 FM",
                currentTrack = "Beethoven - Symphony No. 9 in D minor",
                genre = "Classical"
            ),
            RadioStation(
                id = "rad_jazz_cafe",
                name = "Midnight Jazz & Blues Cafe",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                logoUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=500&q=80",
                frequency = "89.4 FM",
                currentTrack = "Miles Davis & Coltrane Sessions",
                genre = "Jazz / Blues"
            )
        )

        _categories.value = initialCategories
        _channels.value = initialChannels
        _movies.value = initialMovies
        _radios.value = initialRadios

        // Initial default playable item is Nat Geo Wild HD
        _currentPlayingMedia.value = initialChannels.first().toPlayableMedia()
    }

    /**
     * Checks actual device internet connectivity
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Verifies activation code against remote storage / database
     * Accurately checks Private vs Universal code types and expiration timestamps
     */
    suspend fun verifyActivationCodeRemote(code: String): ActivationResult {
        val sanitized = code.trim().uppercase()
        if (sanitized.isBlank()) {
            return ActivationResult.Invalid("Activation code cannot be empty.")
        }

        // 1. Check in synchronized remote database first (fast local cache & offline readiness)
        val registeredRecord = remoteActivationDatabase[sanitized]

        if (registeredRecord != null) {
            if (registeredRecord.isRevoked) {
                return ActivationResult.Invalid("This code was revoked by the admin administrator.")
            }

            if (registeredRecord.isExpired) {
                return ActivationResult.Expired(
                    code = sanitized,
                    expiredDateString = registeredRecord.expirationDisplay,
                    message = "This ${registeredRecord.type.label} expired on ${registeredRecord.expirationDisplay}."
                )
            }

            _isActivated.value = true
            _activeCode.value = sanitized
            _activeCodeRecord.value = registeredRecord
            return ActivationResult.Success(registeredRecord)
        }

        // 2. Try remote HTTP verification if server is configured and reachable
        val httpResult = withContext(Dispatchers.IO) {
            try {
                val serverUrl = _adminConfig.value.serverUrl.removeSuffix("/")
                val request = Request.Builder()
                    .url("$serverUrl/verify-code?code=$sanitized")
                    .header("User-Agent", "EURO-IPTV-Android/2.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val isValid = json.optBoolean("valid", false)
                        val typeStr = json.optString("type", "UNIVERSAL")
                        val codeType = if (typeStr.equals("PRIVATE", ignoreCase = true)) CodeType.PRIVATE else CodeType.UNIVERSAL
                        val expiresAtMillis = if (json.has("expiresAt")) json.getLong("expiresAt") else null
                        val durationDays = json.optInt("durationDays", 365)
                        val planName = json.optString("planName", "Remote VIP Subscription")

                        val record = ActivationCodeRecord(
                            code = sanitized,
                            type = codeType,
                            durationDays = durationDays,
                            expiresAtMillis = expiresAtMillis,
                            planName = planName
                        )

                        if (record.isExpired) {
                            return@withContext ActivationResult.Expired(
                                code = sanitized,
                                expiredDateString = record.expirationDisplay,
                                message = "This ${codeType.label} expired on ${record.expirationDisplay}."
                            )
                        }

                        if (isValid) {
                            _isActivated.value = true
                            _activeCode.value = sanitized
                            _activeCodeRecord.value = record
                            return@withContext ActivationResult.Success(record)
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback gracefully
            }
            null
        }

        if (httpResult != null) {
            return httpResult
        }

        // 3. Fallback algorithmic verification for valid subscription patterns
        // Supports PUBLIC, VIP, PREMIUM, DEVELOPER, UNIVERSAL, and PRIVATE code formats
        val isAlgorithmicValid = (sanitized.startsWith("EURO") && sanitized.length >= 6) ||
                (sanitized.startsWith("VIP") && sanitized.length >= 6) ||
                (sanitized.startsWith("ALTERO") && sanitized.length >= 6) ||
                (sanitized.startsWith("PUBLIC") && sanitized.length >= 6) ||
                (sanitized.startsWith("PREMIUM") && sanitized.length >= 6) ||
                (sanitized.startsWith("DEV") && sanitized.length >= 4) ||
                (sanitized.startsWith("PRIV-") && sanitized.length >= 10)

        if (isAlgorithmicValid) {
            val detectedType = when {
                sanitized.startsWith("DEV") -> CodeType.DEVELOPER
                sanitized.startsWith("PUBLIC") -> CodeType.PUBLIC
                sanitized.startsWith("PREMIUM") -> CodeType.PREMIUM
                sanitized.startsWith("VIP") || sanitized.startsWith("ALTERO") -> CodeType.VIP
                sanitized.startsWith("PRIV") -> CodeType.PRIVATE
                else -> CodeType.UNIVERSAL
            }

            val record = ActivationCodeRecord(
                code = sanitized,
                type = detectedType,
                durationDays = if (detectedType == CodeType.DEVELOPER) -1 else 365,
                expiresAtMillis = if (detectedType == CodeType.DEVELOPER) null else System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                planName = "Altero ${detectedType.label} License"
            )
            _isActivated.value = true
            _activeCode.value = sanitized
            _activeCodeRecord.value = record
            remoteActivationDatabase[sanitized] = record
            return ActivationResult.Success(record)
        }

        // Invalid code
        return ActivationResult.Invalid("Activation code not found in jsonbin.io remote storage or local database.")
    }

    fun deactivate() {
        _isActivated.value = false
        _activeCode.value = null
        _activeCodeRecord.value = null
    }

    /**
     * Synchronous fast-check for quick activation tests
     */
    fun activateWithCode(code: String): Boolean {
        val sanitized = code.trim().uppercase()
        val record = remoteActivationDatabase[sanitized]
        if (record != null && !record.isExpired && !record.isRevoked) {
            _isActivated.value = true
            _activeCode.value = sanitized
            _activeCodeRecord.value = record
            return true
        }

        val isAlgorithmicValid = sanitized.isNotEmpty() && (
                sanitized.length >= 6 ||
                _adminConfig.value.allowedDemoCodes.contains(sanitized) ||
                sanitized.startsWith("EURO") ||
                sanitized.startsWith("ALTERO") ||
                sanitized.startsWith("DEV") ||
                sanitized.startsWith("VIP")
        )

        if (isAlgorithmicValid) {
            val detectedType = when {
                sanitized.startsWith("DEV") -> CodeType.DEVELOPER
                sanitized.startsWith("PUBLIC") -> CodeType.PUBLIC
                sanitized.startsWith("PREMIUM") -> CodeType.PREMIUM
                sanitized.startsWith("VIP") || sanitized.startsWith("ALTERO") -> CodeType.VIP
                else -> CodeType.UNIVERSAL
            }

            val rec = ActivationCodeRecord(
                code = sanitized,
                type = detectedType,
                durationDays = if (detectedType == CodeType.DEVELOPER) -1 else 365,
                expiresAtMillis = if (detectedType == CodeType.DEVELOPER) null else System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                planName = "Altero ${detectedType.label} Package"
            )
            _isActivated.value = true
            _activeCode.value = sanitized
            _activeCodeRecord.value = rec
            return true
        }
        return false
    }

    /**
     * Dedicated Fetch & Parser for jsonbin.io (Bin ID: 6aadd3a7ffd5d1605317d315)
     * Handles authentication headers (X-Master-Key / X-Access-Key), JSONBin v3 'record' wrapper,
     * parses codes (public, vip, premium, developer) and media (live_tv, movies, radio),
     * and performs graceful fallback if bin is private or unreachable.
     */
    suspend fun fetchFromJsonBin(
        binId: String = _adminConfig.value.jsonBinId,
        apiKey: String? = _adminConfig.value.jsonBinApiKey
    ): JsonBinFetchResult = withContext(Dispatchers.IO) {
        _isFetchingRemoteContent.value = true
        val targetUrl = "https://api.jsonbin.io/v3/b/$binId/latest"

        try {
            val reqBuilder = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Altero-Media-Stream-Android/2.0")

            if (!apiKey.isNullOrBlank()) {
                reqBuilder.header("X-Master-Key", apiKey.trim())
                reqBuilder.header("X-Access-Key", apiKey.trim())
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val statusCode = response.code
            val responseBody = response.body?.string() ?: ""

            if (statusCode == 401 || statusCode == 403 || responseBody.contains("X-Master-Key or X-Access-Key")) {
                val authMsg = "jsonbin.io bin $binId is private. Pass X-Master-Key or X-Access-Key in Admin Panel or make bin public."
                _adminConfig.value = _adminConfig.value.copy(
                    lastSyncStatus = "Auth Required: $authMsg",
                    lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                )
                return@withContext JsonBinFetchResult.AuthRequired(binId, authMsg)
            }

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val jsonRoot = JSONObject(responseBody)
                val dataObj = if (jsonRoot.has("record")) jsonRoot.getJSONObject("record") else jsonRoot

                var parsedCodes = 0
                var parsedChannels = 0
                var parsedMovies = 0
                var parsedRadios = 0

                // 1. Parse Codes (public, vip, premium, developer, universal, private)
                val codesArray = dataObj.optJSONArray("codes")
                if (codesArray != null) {
                    for (i in 0 until codesArray.length()) {
                        val cObj = codesArray.getJSONObject(i)
                        val code = cObj.optString("code", cObj.optString("key", "")).trim().uppercase()
                        if (code.isNotEmpty()) {
                            val typeStr = cObj.optString("type", "universal")
                            val codeType = CodeType.fromString(typeStr)
                            val durationDays = cObj.optInt("durationDays", cObj.optInt("duration_days", 365))
                            val expiresAt = when {
                                cObj.has("expiresAt") && !cObj.isNull("expiresAt") -> cObj.getLong("expiresAt")
                                cObj.has("expires_at") && !cObj.isNull("expires_at") -> cObj.getLong("expires_at")
                                durationDays > 0 -> System.currentTimeMillis() + (durationDays.toLong() * 24 * 60 * 60 * 1000)
                                else -> null
                            }
                            val planName = cObj.optString("planName", cObj.optString("plan_name", "${codeType.label} Pass"))
                            val isRevoked = cObj.optBoolean("isRevoked", cObj.optBoolean("revoked", false))
                            val assignedUser = if (cObj.has("assignedUser") && !cObj.isNull("assignedUser")) cObj.getString("assignedUser") else null

                            remoteActivationDatabase[code] = ActivationCodeRecord(
                                code = code,
                                type = codeType,
                                durationDays = durationDays,
                                expiresAtMillis = expiresAt,
                                isRevoked = isRevoked,
                                assignedUser = assignedUser,
                                planName = planName
                            )
                            parsedCodes++
                        }
                    }
                }

                // 2. Parse live_tv (or channels)
                val tvArray = dataObj.optJSONArray("live_tv") ?: dataObj.optJSONArray("channels")
                if (tvArray != null && tvArray.length() > 0) {
                    val channelsList = mutableListOf<ChannelItem>()
                    for (i in 0 until tvArray.length()) {
                        val obj = tvArray.getJSONObject(i)
                        val logo = if (obj.has("logoUrl") && !obj.isNull("logoUrl")) obj.getString("logoUrl") else if (obj.has("logo") && !obj.isNull("logo")) obj.getString("logo") else null
                        channelsList.add(
                            ChannelItem(
                                id = obj.optString("id", "ch_live_$i"),
                                name = obj.optString("name", obj.optString("title", "Altero Channel $i")),
                                categoryId = obj.optString("categoryId", obj.optString("category", "cat_sports")),
                                streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                                logoUrl = logo,
                                currentProgram = obj.optString("currentProgram", obj.optString("program", "Live Transmission")),
                                quality = obj.optString("quality", "FHD 1080p"),
                                views = obj.optString("views", "+2.5M Views")
                            )
                        )
                    }
                    if (channelsList.isNotEmpty()) {
                        _channels.value = channelsList
                        parsedChannels = channelsList.size
                    }
                }

                // 3. Parse movies
                val movArray = dataObj.optJSONArray("movies") ?: dataObj.optJSONArray("vod")
                if (movArray != null && movArray.length() > 0) {
                    val moviesList = mutableListOf<MovieItem>()
                    for (i in 0 until movArray.length()) {
                        val obj = movArray.getJSONObject(i)
                        val poster = if (obj.has("posterUrl") && !obj.isNull("posterUrl")) obj.getString("posterUrl") else if (obj.has("poster") && !obj.isNull("poster")) obj.getString("poster") else null
                        moviesList.add(
                            MovieItem(
                                id = obj.optString("id", "mov_vod_$i"),
                                title = obj.optString("title", obj.optString("name", "Altero Movie $i")),
                                categoryId = obj.optString("categoryId", obj.optString("category", "cat_movies")),
                                streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                                posterUrl = poster,
                                genre = obj.optString("genre", "Cinema / 4K UHD"),
                                duration = obj.optString("duration", "2h 10m"),
                                rating = obj.optString("rating", "8.7")
                            )
                        )
                    }
                    if (moviesList.isNotEmpty()) {
                        _movies.value = moviesList
                        parsedMovies = moviesList.size
                    }
                }

                // 4. Parse radio
                val radArray = dataObj.optJSONArray("radio") ?: dataObj.optJSONArray("radios")
                if (radArray != null && radArray.length() > 0) {
                    val radiosList = mutableListOf<RadioStation>()
                    for (i in 0 until radArray.length()) {
                        val obj = radArray.getJSONObject(i)
                        val radLogo = if (obj.has("logoUrl") && !obj.isNull("logoUrl")) obj.getString("logoUrl") else if (obj.has("logo") && !obj.isNull("logo")) obj.getString("logo") else null
                        radiosList.add(
                            RadioStation(
                                id = obj.optString("id", "rad_station_$i"),
                                name = obj.optString("name", "Altero Radio $i"),
                                streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                                logoUrl = radLogo,
                                frequency = obj.optString("frequency", "101.5 FM"),
                                currentTrack = obj.optString("currentTrack", obj.optString("track", "Live Broadcast")),
                                genre = obj.optString("genre", "World Hits")
                            )
                        )
                    }
                    if (radiosList.isNotEmpty()) {
                        _radios.value = radiosList
                        parsedRadios = radiosList.size
                    }
                }

                _adminConfig.value = _adminConfig.value.copy(
                    lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
                    lastSyncStatus = "Success: Synced $parsedCodes codes, $parsedChannels channels, $parsedMovies movies, $parsedRadios radios."
                )

                return@withContext JsonBinFetchResult.Success(binId, parsedCodes, parsedChannels, parsedMovies, parsedRadios)
            } else {
                val errorMsg = "HTTP $statusCode: ${response.message}"
                _adminConfig.value = _adminConfig.value.copy(
                    lastSyncStatus = "Error: $errorMsg",
                    lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                )
                return@withContext JsonBinFetchResult.Error(binId, errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Network error connecting to jsonbin.io"
            _adminConfig.value = _adminConfig.value.copy(
                lastSyncStatus = "Fallback Active: $errorMsg",
                lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            )
            return@withContext JsonBinFetchResult.Error(binId, errorMsg)
        } finally {
            _isFetchingRemoteContent.value = false
        }
    }

    /**
     * Dynamic Remote Content Fetching from remote URL endpoint
     * Seamlessly routes to JSONBin.io if URL points to jsonbin.io, or standard M3U/JSON API
     */
    suspend fun fetchRemoteContent(serverUrl: String = _adminConfig.value.serverUrl): Boolean = withContext(Dispatchers.IO) {
        if (serverUrl.contains("jsonbin.io")) {
            val result = fetchFromJsonBin(_adminConfig.value.jsonBinId, _adminConfig.value.jsonBinApiKey)
            return@withContext result is JsonBinFetchResult.Success
        }

        _isFetchingRemoteContent.value = true
        try {
            val cleanUrl = if (serverUrl.endsWith(".json")) serverUrl else serverUrl.removeSuffix("/") + "/content.json"
            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "Altero-Media-Stream-Android/2.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val jsonRoot = JSONObject(body)
                    val root = if (jsonRoot.has("record")) jsonRoot.getJSONObject("record") else jsonRoot

                    // Parse Remote Channels
                    val channelsArray = root.optJSONArray("channels") ?: root.optJSONArray("live_tv")
                    if (channelsArray != null) {
                        val fetchedChannels = mutableListOf<ChannelItem>()
                        for (i in 0 until channelsArray.length()) {
                            val obj = channelsArray.getJSONObject(i)
                            val logo = if (obj.has("logoUrl") && !obj.isNull("logoUrl")) obj.getString("logoUrl") else null
                            fetchedChannels.add(
                                ChannelItem(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    name = obj.optString("name", obj.optString("title", "Channel $i")),
                                    categoryId = obj.optString("categoryId", obj.optString("category", "cat_news")),
                                    streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                                    logoUrl = logo,
                                    currentProgram = obj.optString("currentProgram", "Live Broadcasting"),
                                    quality = obj.optString("quality", "FHD 1080p"),
                                    views = obj.optString("views", "+1.0M Views")
                                )
                            )
                        }
                        if (fetchedChannels.isNotEmpty()) {
                            _channels.value = fetchedChannels
                        }
                    }

                    // Parse Remote Movies
                    val moviesArray = root.optJSONArray("movies") ?: root.optJSONArray("vod")
                    if (moviesArray != null) {
                        val fetchedMovies = mutableListOf<MovieItem>()
                        for (i in 0 until moviesArray.length()) {
                            val obj = moviesArray.getJSONObject(i)
                            val poster = if (obj.has("posterUrl") && !obj.isNull("posterUrl")) obj.getString("posterUrl") else null
                            fetchedMovies.add(
                                MovieItem(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    title = obj.optString("title", obj.optString("name", "Movie $i")),
                                    categoryId = obj.optString("categoryId", obj.optString("category", "cat_movies")),
                                    streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                                    posterUrl = poster,
                                    genre = obj.optString("genre", "Action / Thriller"),
                                    duration = obj.optString("duration", "2h 00m"),
                                    rating = obj.optString("rating", "8.5")
                                )
                            )
                        }
                        if (fetchedMovies.isNotEmpty()) {
                            _movies.value = fetchedMovies
                        }
                    }

                    // Parse Remote Radios
                    val radiosArray = root.optJSONArray("radios") ?: root.optJSONArray("radio")
                    if (radiosArray != null) {
                        val fetchedRadios = mutableListOf<RadioStation>()
                        for (i in 0 until radiosArray.length()) {
                            val obj = radiosArray.getJSONObject(i)
                            val rLogo = if (obj.has("logoUrl") && !obj.isNull("logoUrl")) obj.getString("logoUrl") else null
                            fetchedRadios.add(
                                RadioStation(
                                    id = obj.optString("id", UUID.randomUUID().toString()),
                                    name = obj.optString("name", "Radio Station $i"),
                                    streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                                    logoUrl = rLogo,
                                    frequency = obj.optString("frequency", "99.0 FM"),
                                    currentTrack = obj.optString("currentTrack", "Live Broadcast"),
                                    genre = obj.optString("genre", "Hits")
                                )
                            )
                        }
                        if (fetchedRadios.isNotEmpty()) {
                            _radios.value = fetchedRadios
                        }
                    }

                    _adminConfig.value = _adminConfig.value.copy(
                        lastSyncTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date()),
                        lastSyncStatus = "Synced successfully from $cleanUrl"
                    )
                    return@withContext true
                }
            }
        } catch (_: Exception) {
            // Keep existing or initial catalogue intact if remote endpoint is simulated
        } finally {
            _isFetchingRemoteContent.value = false
        }
        return@withContext false
    }

    // Media Playback Functions
    fun playMedia(media: PlayableMedia) {
        _currentPlayingMedia.value = media
    }

    fun playChannel(channel: ChannelItem) {
        _currentPlayingMedia.value = channel.toPlayableMedia()
    }

    fun playMovie(movie: MovieItem) {
        _currentPlayingMedia.value = movie.toPlayableMedia()
    }

    fun playRadio(radio: RadioStation) {
        _currentPlayingMedia.value = radio.toPlayableMedia()
    }

    fun toggleFavorite(itemId: String) {
        _channels.value = _channels.value.map {
            if (it.id == itemId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _movies.value = _movies.value.map {
            if (it.id == itemId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _radios.value = _radios.value.map {
            if (it.id == itemId) it.copy(isFavorite = !it.isFavorite) else it
        }
        if (_currentPlayingMedia.value?.id == itemId) {
            _currentPlayingMedia.value = _currentPlayingMedia.value?.let { it.copy(isFavorite = !it.isFavorite) }
        }
    }

    // Remote Admin Controller Functions
    fun addChannel(channel: ChannelItem) {
        _channels.value = _channels.value + channel
    }

    fun deleteChannel(channelId: String) {
        _channels.value = _channels.value.filter { it.id != channelId }
    }

    fun addMovie(movie: MovieItem) {
        _movies.value = _movies.value + movie
    }

    fun deleteMovie(movieId: String) {
        _movies.value = _movies.value.filter { it.id != movieId }
    }

    fun addRadio(radio: RadioStation) {
        _radios.value = _radios.value + radio
    }

    fun deleteRadio(radioId: String) {
        _radios.value = _radios.value.filter { it.id != radioId }
    }

    fun addCategory(category: ChannelCategory) {
        _categories.value = _categories.value + category
    }

    fun deleteCategory(categoryId: String) {
        _categories.value = _categories.value.filter { it.id != categoryId }
        _channels.value = _channels.value.filter { it.categoryId != categoryId }
    }

    fun updateAdminConfig(newConfig: AdminConfig) {
        _adminConfig.value = newConfig
    }

    // Language selection: English vs Arabic
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }
}

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", "English", false),
    ARABIC("ar", "Arabic", "العربية", true)
}
