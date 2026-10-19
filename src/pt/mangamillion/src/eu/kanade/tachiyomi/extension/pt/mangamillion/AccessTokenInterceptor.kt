package eu.kanade.tachiyomi.extension.pt.mangamillion

import keiyoushi.utils.parseAsProto
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class AccessTokenInterceptor : Interceptor {
    @Volatile
    private var token: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != API_HOST || request.url.encodedPath == REGISTER_PATH) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request.withToken(getToken(chain)))
        if (response.code !in EXPIRED_TOKEN_CODES) return response

        response.close()
        token = null
        return chain.proceed(request.withToken(getToken(chain)))
    }

    private fun getToken(chain: Interceptor.Chain): String = token ?: synchronized(this) {
        token ?: register(chain.request(), chain).also { token = it }
    }

    private fun register(request: Request, chain: Interceptor.Chain): String {
        val url = request.url.newBuilder()
            .encodedPath(REGISTER_PATH)
            .query(null)
            .addQueryParameter("service_language", request.url.queryParameter("service_language"))
            .build()
        val registerRequest = request.newBuilder()
            .url(url)
            .removeHeader(TOKEN_HEADER)
            .post(ByteArray(0).toRequestBody())
            .build()

        val response = chain.proceed(registerRequest)
        if (!response.isSuccessful) {
            response.close()
            throw IOException("Failed to get an access token (HTTP ${response.code})")
        }
        return response.parseAsProto<ApiResponse>().deviceToken!!.token
    }

    private fun Request.withToken(token: String) = newBuilder().header(TOKEN_HEADER, token).build()

    companion object {
        const val API_HOST = "api.mangamillion.shueisha.co.jp"
        private const val REGISTER_PATH = "/api/register"
        private const val TOKEN_HEADER = "Access-Token"
        private val EXPIRED_TOKEN_CODES = setOf(401, 403, 422)
    }
}
