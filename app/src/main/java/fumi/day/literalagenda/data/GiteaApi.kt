package fumi.day.literalagenda.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GiteaApi(host: String) : GitForgeApi {

    private val baseUrl = "${host.trimEnd('/')}/api/v1"

    private suspend fun request(
        method: String,
        url: String,
        token: String,
        body: String? = null
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                OutputStreamWriter(connection.outputStream).use { it.write(body) }
            }
            val code = connection.responseCode
            val text = try {
                connection.inputStream.bufferedReader().use(BufferedReader::readText)
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
            }
            Pair(code, text)
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun listFiles(token: String, repo: String, dir: String): List<GitFile> {
        return try {
            val (code, body) = request("GET", "$baseUrl/repos/$repo/contents/$dir", token)
            if (code != 200) return emptyList()
            val array = JSONArray(body)
            (0 until array.length())
                .map { array.getJSONObject(it) }
                .filter { it.getString("name").endsWith(".md") }
                .map { GitFile(it.getString("name"), it.getString("path"), it.getString("sha")) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getFileContent(token: String, repo: String, path: String): String? {
        return try {
            val (code, body) = request("GET", "$baseUrl/repos/$repo/contents/$path", token)
            if (code != 200) return null
            val encoded = JSONObject(body).getString("content").replace("\n", "")
            String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun putFile(token: String, repo: String, path: String, content: String, sha: String?): String? {
        return try {
            val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val bodyObj = JSONObject().apply {
                put("message", if (sha == null) "Create $path" else "Update $path")
                put("content", encoded)
                if (sha != null) put("sha", sha)
            }
            val (code, body) = request("PUT", "$baseUrl/repos/$repo/contents/$path", token, bodyObj.toString())
            if (code == 200 || code == 201) JSONObject(body).getJSONObject("content").getString("sha") else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteFile(token: String, repo: String, path: String, sha: String): Boolean {
        return try {
            val bodyObj = JSONObject().apply {
                put("message", "Delete $path")
                put("sha", sha)
            }
            val (code, _) = request("DELETE", "$baseUrl/repos/$repo/contents/$path", token, bodyObj.toString())
            code == 200
        } catch (e: Exception) {
            false
        }
    }
}
