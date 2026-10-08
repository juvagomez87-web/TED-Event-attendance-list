package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppUpdateEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.UserAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupFileInfo(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val isAutomated: Boolean
)

object BackupManager {
    private const val MAGIC_HEADER = "TED_ENC_V1"
    private const val AES_KEY_BIT_LENGTH = 256
    private const val PBKDF2_ITERATIONS = 10000
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val SALT_LENGTH = 16

    private val secureRandom = SecureRandom()

    private fun getBackupsDir(context: Context): File {
        val dir = File(context.filesDir, "secure_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Creates an encrypted full database backup using AES-256-GCM.
     */
    suspend fun createEncryptedBackup(
        context: Context,
        db: AppDatabase,
        passphrase: String,
        isAutomated: Boolean = false,
        author: String = "Admin"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val keyPassphrase = passphrase.ifBlank { "TED_PRIVATE_MASTER_KEY_2026" }

            // 1. Gather all entity records into a pristine JSON snapshot
            val events = db.eventDao().getAllEvents().first()
            val members = db.memberDao().getAllMembers().first()
            val attendances = db.attendanceDao().getAllAttendance().first()
            val auditLogs = db.auditLogDao().getAllLogs().first()
            val updates = db.appUpdateDao().getAllUpdates().first()
            val accounts = db.userAccountDao().getAllAccounts().first()

            val snapshot = JSONObject().apply {
                put("app", "TED PRIVATE")
                put("version", "2.6.0")
                put("backupTimestamp", System.currentTimeMillis())
                put("isAutomated", isAutomated)
                put("author", author)

                // Events
                val eventsArr = JSONArray()
                events.forEach { ev ->
                    eventsArr.put(JSONObject().apply {
                        put("id", ev.id)
                        put("title", ev.title)
                        put("category", ev.category)
                        put("description", ev.description)
                        put("dateEpochMillis", ev.dateEpochMillis)
                        put("targetGroup", ev.targetGroup)
                        put("status", ev.status)
                        put("locationOrChannel", ev.locationOrChannel)
                        put("createdBy", ev.createdBy)
                        put("createdAt", ev.createdAt)
                    })
                }
                put("events", eventsArr)

                // Members
                val membersArr = JSONArray()
                members.forEach { m ->
                    membersArr.put(JSONObject().apply {
                        put("id", m.id)
                        put("name", m.name)
                        put("handle", m.handle)
                        put("rank", m.rank)
                        put("groupRole", m.groupRole)
                        put("contact", m.contact)
                        put("notes", m.notes)
                        put("active", m.active)
                        put("createdAt", m.createdAt)
                    })
                }
                put("members", membersArr)

                // Attendances
                val attendancesArr = JSONArray()
                attendances.forEach { att ->
                    attendancesArr.put(JSONObject().apply {
                        put("id", att.id)
                        put("eventId", att.eventId)
                        put("memberId", att.memberId)
                        put("status", att.status)
                        put("notes", att.notes)
                        put("updatedBy", att.updatedBy)
                        put("updatedAt", att.updatedAt)
                    })
                }
                put("attendances", attendancesArr)

                // Audit Logs
                val auditArr = JSONArray()
                auditLogs.forEach { log ->
                    auditArr.put(JSONObject().apply {
                        put("id", log.id)
                        put("timestamp", log.timestamp)
                        put("authorName", log.authorName)
                        put("actionType", log.actionType)
                        put("entityTarget", log.entityTarget)
                        put("details", log.details)
                    })
                }
                put("auditLogs", auditArr)

                // User Accounts
                val userArr = JSONArray()
                accounts.forEach { acc ->
                    userArr.put(JSONObject().apply {
                        put("id", acc.id)
                        put("username", acc.username)
                        put("displayName", acc.displayName)
                        put("emailOrDiscord", acc.emailOrDiscord)
                        put("pinOrPassword", acc.pinOrPassword)
                        put("role", acc.role)
                        put("memberId", acc.memberId ?: JSONObject.NULL)
                        put("lastLoginAt", acc.lastLoginAt)
                        put("loginCount", acc.loginCount)
                        put("isActive", acc.isActive)
                    })
                }
                put("accounts", userArr)
            }

            val rawDataBytes = snapshot.toString(2).toByteArray(Charsets.UTF_8)

            // 2. Key derivation via PBKDF2WithHmacSHA256
            val salt = ByteArray(SALT_LENGTH)
            secureRandom.nextBytes(salt)

            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)

            val keySpec = PBEKeySpec(keyPassphrase.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BIT_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val secretKeyBytes = factory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(secretKeyBytes, "AES")

            // 3. Encrypt using AES/GCM/NoPadding
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val cipherText = cipher.doFinal(rawDataBytes)

            // 4. Write into encrypted backup file
            val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val prefix = if (isAutomated) "auto_backup" else "manual_backup"
            val backupFile = File(getBackupsDir(context), "${prefix}_${timeTag}.tedbackup")

            FileOutputStream(backupFile).use { fos ->
                fos.write(MAGIC_HEADER.toByteArray(Charsets.UTF_8)) // 10 bytes
                fos.write(salt) // 16 bytes
                fos.write(iv)   // 12 bytes
                fos.write(cipherText)
            }

            // Clean up old automated backups if more than 5 exist
            cleanupOldAutomatedBackups(context)

            // Log in Audit Log
            db.auditLogDao().insert(
                AuditLogEntity(
                    authorName = author,
                    actionType = "BACKUP_EXPORT",
                    entityTarget = backupFile.name,
                    details = "Exported AES-256 encrypted database backup (${backupFile.length() / 1024} KB). Mode: ${if (isAutomated) "Automated" else "Manual"}."
                )
            )

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts and restores full database from a .tedbackup file.
     */
    suspend fun restoreEncryptedBackup(
        context: Context,
        db: AppDatabase,
        backupFile: File,
        passphrase: String,
        author: String = "Admin"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val keyPassphrase = passphrase.ifBlank { "TED_PRIVATE_MASTER_KEY_2026" }

            FileInputStream(backupFile).use { fis ->
                val magicBytes = ByteArray(MAGIC_HEADER.length)
                val readMagic = fis.read(magicBytes)
                if (readMagic != MAGIC_HEADER.length || String(magicBytes) != MAGIC_HEADER) {
                    return@withContext Result.failure(Exception("Ongeldig backup bestand (ontbrekende TED versleutelingsheader)"))
                }

                val salt = ByteArray(SALT_LENGTH)
                fis.read(salt)

                val iv = ByteArray(GCM_IV_LENGTH)
                fis.read(iv)

                val cipherText = fis.readBytes()

                // Derive key
                val keySpec = PBEKeySpec(keyPassphrase.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BIT_LENGTH)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val secretKeyBytes = factory.generateSecret(keySpec).encoded
                val secretKey = SecretKeySpec(secretKeyBytes, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
                val plainBytes = cipher.doFinal(cipherText)

                val json = JSONObject(String(plainBytes, Charsets.UTF_8))

                // Restore Entities
                var restoredCount = 0

                // Events
                val eventsArr = json.optJSONArray("events") ?: JSONArray()
                for (i in 0 until eventsArr.length()) {
                    val obj = eventsArr.getJSONObject(i)
                    db.eventDao().insert(
                        EventEntity(
                            id = obj.optLong("id", 0),
                            title = obj.getString("title"),
                            category = obj.getString("category"),
                            description = obj.optString("description", ""),
                            dateEpochMillis = obj.optLong("dateEpochMillis", System.currentTimeMillis()),
                            targetGroup = obj.optString("targetGroup", "All"),
                            status = obj.optString("status", "ACTIVE"),
                            locationOrChannel = obj.optString("locationOrChannel", "Discord HQ"),
                            createdBy = obj.optString("createdBy", "Admin")
                        )
                    )
                    restoredCount++
                }

                // Members
                val membersArr = json.optJSONArray("members") ?: JSONArray()
                for (i in 0 until membersArr.length()) {
                    val obj = membersArr.getJSONObject(i)
                    db.memberDao().insert(
                        MemberEntity(
                            id = obj.optLong("id", 0),
                            name = obj.getString("name"),
                            handle = obj.getString("handle"),
                            rank = obj.optString("rank", "R4"),
                            groupRole = obj.optString("groupRole", "Member"),
                            contact = obj.optString("contact", ""),
                            notes = obj.optString("notes", ""),
                            active = obj.optBoolean("active", true)
                        )
                    )
                    restoredCount++
                }

                // Attendance
                val attendancesArr = json.optJSONArray("attendances") ?: JSONArray()
                for (i in 0 until attendancesArr.length()) {
                    val obj = attendancesArr.getJSONObject(i)
                    db.attendanceDao().upsert(
                        AttendanceEntity(
                            id = obj.optLong("id", 0),
                            eventId = obj.getLong("eventId"),
                            memberId = obj.getLong("memberId"),
                            status = obj.getString("status"),
                            notes = obj.optString("notes", ""),
                            updatedBy = obj.optString("updatedBy", author),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            syncStatus = "SYNCED"
                        )
                    )
                    restoredCount++
                }

                // User accounts
                val userArr = json.optJSONArray("accounts") ?: JSONArray()
                for (i in 0 until userArr.length()) {
                    val obj = userArr.getJSONObject(i)
                    val mId = if (obj.isNull("memberId")) null else obj.optLong("memberId")
                    db.userAccountDao().insert(
                        UserAccountEntity(
                            id = obj.optLong("id", 0),
                            username = obj.getString("username"),
                            displayName = obj.getString("displayName"),
                            emailOrDiscord = obj.getString("emailOrDiscord"),
                            pinOrPassword = obj.getString("pinOrPassword"),
                            role = obj.optString("role", "MEMBER"),
                            memberId = mId,
                            lastLoginAt = obj.optLong("lastLoginAt", 0),
                            loginCount = obj.optInt("loginCount", 0),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                    restoredCount++
                }

                db.auditLogDao().insert(
                    AuditLogEntity(
                        authorName = author,
                        actionType = "BACKUP_RESTORE",
                        entityTarget = backupFile.name,
                        details = "Restored database from AES-256 encrypted backup ($restoredCount total records recovered)."
                    )
                )

                Result.success(restoredCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listBackups(context: Context): List<BackupFileInfo> {
        val dir = getBackupsDir(context)
        val files = dir.listFiles { f -> f.extension == "tedbackup" } ?: emptyArray()
        return files.map { file ->
            BackupFileInfo(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                timestamp = file.lastModified(),
                isAutomated = file.name.startsWith("auto_")
            )
        }.sortedByDescending { it.timestamp }
    }

    private fun cleanupOldAutomatedBackups(context: Context) {
        val dir = getBackupsDir(context)
        val autoBackups = dir.listFiles { f -> f.name.startsWith("auto_") && f.extension == "tedbackup" } ?: return
        if (autoBackups.size > 5) {
            autoBackups.sortedBy { it.lastModified() }
                .take(autoBackups.size - 5)
                .forEach { it.delete() }
        }
    }

    /**
     * Shares or exports the encrypted backup file via Android's native share/storage intent.
     */
    fun shareBackupToStorageOrCloud(context: Context, backupFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "TED PRIVATE Beveiligde Database Backup")
            putExtra(Intent.EXTRA_TEXT, "Versleuteld backup bestand voor TED PRIVATE (${backupFile.name}). Bewaar dit veilig op Google Drive of cloudopslag.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Exporteer Beveiligde Backup naar Drive of Opslag").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
