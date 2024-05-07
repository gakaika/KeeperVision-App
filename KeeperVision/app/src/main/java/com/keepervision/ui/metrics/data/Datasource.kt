package com.keepervision.ui.metrics.data

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.keepervision.R
import com.keepervision.ui.metrics.model.SessionInfo
import com.keepervision.utils.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Datasource {
    val TAG = "Datasource"

    fun loadSessions(ctx: Context): List<SessionInfo> {
        val sessionListData: MutableList<SessionInfo> = mutableListOf<SessionInfo>()

        val url = URL(ctx.getString(R.string.server_address) + ctx.getString(R.string.session_route) + "/" + auth.getUserEmail()!!)

        try {
            val client = OkHttpClient()

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    // log the response (either inserted or already there from before) and navigate
                    Log.d(TAG, "Session Get API Response: $responseData")

                    val jsonObject = JsonParser.parseString(responseData).asJsonObject

                    val sessionStats = jsonObject.getAsJsonArray("session_stats").map {
                        JsonParser.parseString(it.asString).asJsonObject
                    }

                    for (i in sessionStats) {
                        sessionListData.add(Gson().fromJson(i, SessionInfo::class.java))
                    }

                    Log.d(TAG, "Session Data: $sessionListData")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Sessions GET Failed")
        }

        return sessionListData
    }
}