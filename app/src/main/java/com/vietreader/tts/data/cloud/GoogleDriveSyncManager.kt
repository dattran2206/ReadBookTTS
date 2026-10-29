package com.vietreader.tts.data.cloud

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Handles seamless one-tap background synchronization with Google Drive using
 * the Google Drive REST API v3 and Google Play Services Auth.
 * Backups are stored safely and privately in the application's hidden appDataFolder.
 */
class GoogleDriveSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "GoogleDriveSyncMgr"
        private const val BACKUP_FILENAME = "wordwave_backup.json"
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val OAUTH_SCOPE_STRING = "oauth2:$DRIVE_APPDATA_SCOPE $DRIVE_FILE_SCOPE"
    }

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(DRIVE_APPDATA_SCOPE), Scope(DRIVE_FILE_SCOPE))
        .build()

    private val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val hasAppData = GoogleSignIn.hasPermissions(account, Scope(DRIVE_APPDATA_SCOPE))
        val hasFile = GoogleSignIn.hasPermissions(account, Scope(DRIVE_FILE_SCOPE))
        return if (hasAppData || hasFile) account else null
    }

    fun getSignInIntent(): Intent {
        return signInClient.signInIntent
    }

    fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return runCatching {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java)
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        signInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }

    suspend fun uploadBackupToDrive(backupJson: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount()
                ?: throw IllegalStateException("Chưa đăng nhập tài khoản Google.")
            val androidAccount = account.account
                ?: throw IllegalStateException("Không tìm thấy thông tin tài khoản Google.")

            var token = fetchOAuthToken(androidAccount)
            try {
                performUpload(token, backupJson)
            } catch (e: Exception) {
                // If 401 or token issue, invalidate token and retry once
                Log.w(TAG, "Upload failed, invalidating token and retrying: ${e.message}")
                GoogleAuthUtil.clearToken(context, token)
                token = fetchOAuthToken(androidAccount)
                performUpload(token, backupJson)
            }
        }
    }

    suspend fun downloadBackupFromDrive(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val account = getSignedInAccount()
                ?: throw IllegalStateException("Chưa đăng nhập tài khoản Google.")
            val androidAccount = account.account
                ?: throw IllegalStateException("Không tìm thấy thông tin tài khoản Google.")

            var token = fetchOAuthToken(androidAccount)
            try {
                performDownload(token)
            } catch (e: Exception) {
                Log.w(TAG, "Download failed, invalidating token and retrying: ${e.message}")
                GoogleAuthUtil.clearToken(context, token)
                token = fetchOAuthToken(androidAccount)
                performDownload(token)
            }
        }
    }

    private fun fetchOAuthToken(account: android.accounts.Account): String {
        return GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_STRING)
    }

    private fun findExistingBackupFileId(token: String): String? {
        val query = "name = '$BACKUP_FILENAME' and trashed = false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlStr = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$encodedQuery&fields=files(id,name,modifiedTime)"
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(responseText)
                val files = root.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    return files.getJSONObject(0).optString("id").takeIf { it.isNotBlank() }
                }
            } else {
                val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                Log.w(TAG, "Search files error ($responseCode): $err")
            }
        } finally {
            conn.disconnect()
        }
        return null
    }

    private fun performUpload(token: String, backupJson: String): String {
        val existingId = findExistingBackupFileId(token)

        if (existingId != null) {
            // Update existing backup file content via PATCH
            val urlStr = "https://www.googleapis.com/upload/drive/v3/files/$existingId?uploadType=media"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "PATCH"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                val bytes = backupJson.toByteArray(Charsets.UTF_8)
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes) }

                val code = conn.responseCode
                if (code in 200..299) {
                    return existingId
                } else {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    throw IllegalStateException("Lỗi tải lên Google Drive ($code): $err")
                }
            } finally {
                conn.disconnect()
            }
        } else {
            // Create new backup file in appDataFolder via multipart POST
            val boundary = "=======WordwaveDriveBoundary" + System.currentTimeMillis() + "======="
            val urlStr = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                val metadataJson = JSONObject().apply {
                    put("name", BACKUP_FILENAME)
                    put("parents", org.json.JSONArray().apply { put("appDataFolder") })
                }.toString()

                val lineEnd = "\r\n"
                val twoDashes = "--"

                val bodyHeader = buildString {
                    append(twoDashes).append(boundary).append(lineEnd)
                    append("Content-Type: application/json; charset=UTF-8").append(lineEnd).append(lineEnd)
                    append(metadataJson).append(lineEnd)
                    append(twoDashes).append(boundary).append(lineEnd)
                    append("Content-Type: application/json; charset=UTF-8").append(lineEnd).append(lineEnd)
                }.toByteArray(Charsets.UTF_8)

                val contentBytes = backupJson.toByteArray(Charsets.UTF_8)
                val bodyFooter = (lineEnd + twoDashes + boundary + twoDashes + lineEnd).toByteArray(Charsets.UTF_8)

                val totalLength = bodyHeader.size + contentBytes.size + bodyFooter.size
                conn.setFixedLengthStreamingMode(totalLength)

                conn.outputStream.use { out ->
                    out.write(bodyHeader)
                    out.write(contentBytes)
                    out.write(bodyFooter)
                    out.flush()
                }

                val code = conn.responseCode
                if (code in 200..299) {
                    val resp = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val fileObj = JSONObject(resp)
                    return fileObj.optString("id")
                } else {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    throw IllegalStateException("Lỗi tạo bản sao lưu trên Google Drive ($code): $err")
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun performDownload(token: String): String {
        val existingId = findExistingBackupFileId(token)
            ?: throw IllegalStateException("Không tìm thấy bản sao lưu nào trên Google Drive của tài khoản này.")

        val urlStr = "https://www.googleapis.com/drive/v3/files/$existingId?alt=media"
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 20000
            conn.readTimeout = 20000

            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                throw IllegalStateException("Lỗi tải bản sao lưu từ Google Drive ($code): $err")
            }
        } finally {
            conn.disconnect()
        }
    }
}
