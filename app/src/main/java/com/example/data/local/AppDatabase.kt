package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AppUpdateDao
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.EventDao
import com.example.data.local.dao.MemberDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.dao.UserAccountDao
import com.example.data.local.entity.AppUpdateEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.UserAccountEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemberEntity::class,
        EventEntity::class,
        AttendanceEntity::class,
        AuditLogEntity::class,
        AppUpdateEntity::class,
        UserAccountEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun eventDao(): EventDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun appUpdateDao(): AppUpdateDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "roster_track_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        if (database.memberDao().count() == 0) {
                            populateInitialData(database)
                        }
                    }
                }
            }
        }

        // Exact clan roster as provided by the user without modifying or translating any name
        val EXACT_CLAN_ROSTER = listOf(
            Pair("TeddyGirl", "R4/R5"),
            Pair("Sagittarius", "R4/R5"),
            Pair("JustineValentina", "R4/R5"),
            Pair("THE WOLVERINE", "R4"),
            Pair("willow", "R4"),
            Pair("Johana O TED O", "R4"),
            Pair("Warden NY Falcon", "R4"),
            Pair("Disturbed1", "R4"),
            Pair("Hans worst", "R3"),
            Pair("TEDOMagellan", "R3"),
            Pair("Shakey", "R3"),
            Pair("minou", "R3"),
            Pair("XxQueenTillyxX", "R3"),
            Pair("MadMark", "R3"),
            Pair("Gemini", "R3"),
            Pair("mathieu", "R3"),
            Pair("XxTillymintxX", "R3"),
            Pair("John Wick79", "R3"),
            Pair("Terminator", "R3"),
            Pair("Eddypiler", "R3"),
            Pair("KMP", "R3"),
            Pair("Kos", "R3"),
            Pair("VaStoner", "R3"),
            Pair("Eagle", "R3"),
            Pair("Xx Baby Tilly xX", "R3"),
            Pair("Xx Tilly 2 xX", "R3"),
            Pair("ThorBjörn", "R3"),
            Pair("Bandit77", "R3"),
            Pair("aras", "R3"),
            Pair("Clamslamma", "R3"),
            Pair("Jer", "R3"),
            Pair("Libra Rising", "R3"),
            Pair("khiim", "R3"),
            Pair("Yano", "R3"),
            Pair("Aries Descending", "R3"),
            Pair("MynameisEarl", "R3"),
            Pair("Clyde Barrow", "R3"),
            Pair("WOOD", "R3"),
            Pair("Jarek II", "R3"),
            Pair("katherine3008v2", "R3"),
            Pair("GRYGOR", "R3"),
            Pair("Ivy laiwill", "R3"),
            Pair("SunflowerBabe", "R3"),
            Pair("WeeWeeJosh", "R3"),
            Pair("RheaMamacita", "R3"),
            Pair("MissValxOd", "R3"),
            Pair("RMalo18", "R3"),
            Pair("ADYLER", "R3"),
            Pair("Wangchuchree", "R3"),
            Pair("K eem", "R3"),
            Pair("Kilroy", "R3"),
            Pair("seb B", "R3"),
            Pair("Bonnie Parker", "R3"),
            Pair("The Watcher", "R2"),
            Pair("BigHulk", "R2"),
            Pair("NiA", "R2"),
            Pair("Spiderman", "R2"),
            Pair("LuCiFeRx13", "R2"),
            Pair("ElizaJane", "R2"),
            Pair("walk185", "R2"),
            Pair("GaryCallahan", "R2"),
            Pair("CERRIES", "R2"),
            Pair("Ej2", "R2"),
            Pair("Bear_Bear", "R2"),
            Pair("walkie", "R2"),
            Pair("Luuuuuavks", "R2"),
            Pair("Irgat", "R2"),
            Pair("FIRE 8", "R2"),
            Pair("warden919065361", "R2")
        )

        suspend fun populateInitialData(db: AppDatabase) {
            val memberDao = db.memberDao()
            val eventDao = db.eventDao()
            val attendanceDao = db.attendanceDao()
            val auditLogDao = db.auditLogDao()
            val appUpdateDao = db.appUpdateDao()
            val userAccountDao = db.userAccountDao()

            val now = System.currentTimeMillis()
            val oneDay = 24 * 60 * 60 * 1000L

            // 1. Insert all 69 exact clan members
            val members = EXACT_CLAN_ROSTER.mapIndexed { index, (name, rank) ->
                val role = when (rank) {
                    "R4/R5" -> "Officer / Lead"
                    "R4" -> "Core Raid"
                    "R3" -> "Veteran"
                    else -> "Member"
                }
                MemberEntity(
                    id = (index + 1).toLong(),
                    name = name,
                    handle = "@${name.replace(" ", "").lowercase()}",
                    rank = rank,
                    groupRole = role,
                    contact = "",
                    notes = ""
                )
            }
            memberDao.insertAll(members)

            // 2. Seed User Accounts (JustineValentina as Admin)
            val accounts = listOf(
                UserAccountEntity(
                    id = 1,
                    username = "JustineValentina",
                    displayName = "JustineValentina",
                    emailOrDiscord = "justine@discord.gg",
                    pinOrPassword = "",
                    role = "ADMIN",
                    memberId = 3,
                    lastLoginAt = now,
                    loginCount = 25
                ),
                UserAccountEntity(
                    id = 2,
                    username = "teddy",
                    displayName = "TeddyGirl",
                    emailOrDiscord = "teddy@discord.gg",
                    pinOrPassword = "",
                    role = "OFFICER",
                    memberId = 1,
                    lastLoginAt = now - 3600000 * 2,
                    loginCount = 9
                ),
                UserAccountEntity(
                    id = 3,
                    username = "sagittarius",
                    displayName = "Sagittarius",
                    emailOrDiscord = "sagit@discord.gg",
                    pinOrPassword = "",
                    role = "OFFICER",
                    memberId = 2,
                    lastLoginAt = now - 3600000 * 5,
                    loginCount = 6
                ),
                UserAccountEntity(
                    id = 5,
                    username = "wolverine",
                    displayName = "THE WOLVERINE",
                    emailOrDiscord = "wolverine@discord.gg",
                    pinOrPassword = "1234",
                    role = "MEMBER",
                    memberId = 4,
                    lastLoginAt = now - oneDay,
                    loginCount = 4
                ),
                UserAccountEntity(
                    id = 6,
                    username = "willow",
                    displayName = "willow",
                    emailOrDiscord = "willow@discord.gg",
                    pinOrPassword = "1234",
                    role = "MEMBER",
                    memberId = 5,
                    lastLoginAt = now - (oneDay * 2),
                    loginCount = 3
                ),
                UserAccountEntity(
                    id = 7,
                    username = "johana",
                    displayName = "Johana O TED O",
                    emailOrDiscord = "johana@discord.gg",
                    pinOrPassword = "1234",
                    role = "MEMBER",
                    memberId = 6,
                    lastLoginAt = 0L,
                    loginCount = 0
                )
            )
            userAccountDao.insertAll(accounts)

            // 3. Initial Land of Jails Events
            val event1 = EventEntity(
                id = 1,
                title = "Ultimate Cage",
                category = "Ultimate Cage",
                description = "Land of Jails: Ultimate Cage arena combat & clan defense.",
                dateEpochMillis = now - (oneDay * 3),
                targetGroup = "All",
                status = "COMPLETED",
                locationOrChannel = "Discord Voice HQ",
                createdBy = "JustineValentina"
            )
            val event2 = EventEntity(
                id = 2,
                title = "Turmoil",
                category = "Turmoil",
                description = "Land of Jails: Turmoil alliance territory contest & strategic push.",
                dateEpochMillis = now - (oneDay * 2),
                targetGroup = "All",
                status = "COMPLETED",
                locationOrChannel = "Discord War Room",
                createdBy = "JustineValentina"
            )
            val event3 = EventEntity(
                id = 3,
                title = "Irons Holds",
                category = "Irons Holds",
                description = "Land of Jails: Irons Holds fortress defense and siege control.",
                dateEpochMillis = now - oneDay,
                targetGroup = "All",
                status = "COMPLETED",
                locationOrChannel = "Discord Stage HQ",
                createdBy = "JustineValentina"
            )
            val event4 = EventEntity(
                id = 4,
                title = "Cristal Clash",
                category = "Cristal Clash",
                description = "Land of Jails: Cristal Clash crystal mines battle & extraction.",
                dateEpochMillis = now,
                targetGroup = "All",
                status = "ACTIVE",
                locationOrChannel = "Discord Voice Room 1",
                createdBy = "JustineValentina"
            )
            val event5 = EventEntity(
                id = 5,
                title = "Sand Sea",
                category = "Sand Sea",
                description = "Land of Jails: Sand Sea desert exploration and outpost control.",
                dateEpochMillis = now + (oneDay * 2),
                targetGroup = "All",
                status = "UPCOMING",
                locationOrChannel = "Discord Voice Room 2",
                createdBy = "JustineValentina"
            )
            val event6 = EventEntity(
                id = 6,
                title = "SVS",
                category = "SVS",
                description = "Land of Jails: Server vs Server supreme realm confrontation.",
                dateEpochMillis = now + (oneDay * 4),
                targetGroup = "All",
                status = "UPCOMING",
                locationOrChannel = "Discord Main Stage",
                createdBy = "JustineValentina"
            )
            val event7 = EventEntity(
                id = 7,
                title = "Supreme",
                category = "Supreme",
                description = "Land of Jails: Supreme championship tournament & guild glory.",
                dateEpochMillis = now + (oneDay * 6),
                targetGroup = "All",
                status = "UPCOMING",
                locationOrChannel = "Discord War Room",
                createdBy = "JustineValentina"
            )

            eventDao.insert(event1)
            eventDao.insert(event2)
            eventDao.insert(event3)
            eventDao.insert(event4)
            eventDao.insert(event5)
            eventDao.insert(event6)
            eventDao.insert(event7)

            // 4. Initial Attendance records for members
            val attendances1 = members.mapIndexed { idx, m ->
                val status = when {
                    idx < 48 -> "PRESENT"
                    idx in 48..56 -> "LATE"
                    idx in 57..62 -> "EXCUSED"
                    else -> "ABSENT"
                }
                val note = when {
                    status == "LATE" -> "Joined 15m delay"
                    status == "EXCUSED" -> "Prior notice given"
                    idx < 3 -> "Event lead"
                    else -> ""
                }
                AttendanceEntity(
                    eventId = 1,
                    memberId = m.id,
                    status = status,
                    notes = note,
                    updatedBy = "JustineValentina"
                )
            }
            attendanceDao.upsertAll(attendances1)

            val attendances2 = members.mapIndexed { idx, m ->
                val status = when {
                    idx < 52 -> "PRESENT"
                    idx in 52..58 -> "LATE"
                    idx in 59..63 -> "EXCUSED"
                    else -> "ABSENT"
                }
                AttendanceEntity(
                    eventId = 2,
                    memberId = m.id,
                    status = status,
                    notes = if (status == "LATE") "Delay" else "",
                    updatedBy = "JustineValentina"
                )
            }
            attendanceDao.upsertAll(attendances2)

            val attendances3 = members.mapIndexed { idx, m ->
                val status = when {
                    idx < 50 -> "PRESENT"
                    idx in 50..55 -> "LATE"
                    idx in 56..60 -> "EXCUSED"
                    else -> "ABSENT"
                }
                AttendanceEntity(
                    eventId = 3,
                    memberId = m.id,
                    status = status,
                    notes = "",
                    updatedBy = "JustineValentina"
                )
            }
            attendanceDao.upsertAll(attendances3)

            // 5. Initial Audit Logs
            auditLogDao.insert(
                AuditLogEntity(
                    timestamp = now - (oneDay * 2),
                    authorName = "JustineValentina",
                    actionType = "ROSTER_INIT",
                    entityTarget = "Land of Jails Clan",
                    details = "Initialized TED PRIVATE roster for Land of Jails with 66 members across ranks R4/R5, R4, R3, and R2."
                )
            )

            // 6. Initial App Updates
            appUpdateDao.insert(
                AppUpdateEntity(
                    title = "Land of Jails Clan Roster Live",
                    versionOrTag = "v2.8.0",
                    message = "TED PRIVATE is geconfigureerd voor Land of Jails met de 7 hoofdevents: Ultimate Cage, Turmoil, Irons Holds, Cristal Clash, Sand Sea, SVS en Supreme. Admin: JustineValentina.",
                    publishedBy = "JustineValentina",
                    timestamp = now - 3600000,
                    isPriority = true
                )
            )
        }
    }
}
