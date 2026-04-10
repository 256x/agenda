package fumi.day.literalagenda.data

enum class GitForge { GITHUB, GITEA }

data class GitFile(
    val name: String,
    val path: String,
    val sha: String
)

interface GitForgeApi {
    suspend fun listFiles(token: String, repo: String, dir: String): List<GitFile>
    suspend fun getFileContent(token: String, repo: String, path: String): String?
    suspend fun putFile(token: String, repo: String, path: String, content: String, sha: String?): String?
    suspend fun deleteFile(token: String, repo: String, path: String, sha: String): Boolean
}
