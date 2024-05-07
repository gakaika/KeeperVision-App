package com.keepervision.utils.auth

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.navigation.NavController
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.keepervision.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


object auth {
    private val TAG: String = "Auth"
    private lateinit var account: Auth0
    private var cachedCredentials: Credentials? = null
    private var cachedUserProfile: UserProfile? = null

    fun login(ctx: Context, navController: NavController) {
        account = Auth0(
            ctx.getString(R.string.com_auth0_client_id),
            ctx.getString(R.string.com_auth0_domain)
        )

        WebAuthProvider
            .login(account)
            .withScheme(ctx.getString(R.string.com_auth0_scheme))
            .withScope(ctx.getString(R.string.login_scopes))
            .withAudience(ctx.getString(R.string.login_audience, ctx.getString(R.string.com_auth0_domain)))
            .start(ctx, object : Callback<Credentials, AuthenticationException> {

                override fun onFailure(error: AuthenticationException) {
                    Log.d(TAG, "Login Failed $error")
                }

                override fun onSuccess(result: Credentials) {
                    cachedCredentials = result
                    Log.d(TAG, "Login Success")

                    // fetch user profile as we need email information
                    val client = AuthenticationAPIClient(account)
                    client
                        .userInfo(cachedCredentials!!.accessToken!!)
                        .start(object : Callback<UserProfile, AuthenticationException> {

                            override fun onFailure(error: AuthenticationException) {
                                Log.d(TAG, "Load User Profile Failed $error")
                            }

                            override fun onSuccess(result: UserProfile) {
                                cachedUserProfile = result
                                Log.d(TAG, "Load User Profile Success, email: " + cachedUserProfile!!.email)

                                // now make request to keepervision API to ensure user is marked register there
                                val url = URL(ctx.getString(R.string.server_address) + ctx.getString(R.string.register_route) + "/" + cachedUserProfile!!.email)
                                // POST Request
                                val connection = url.openConnection() as HttpURLConnection
                                GlobalScope.launch(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient()

                                        val requestJsonObject= JSONObject()
                                        requestJsonObject.put("username",cachedUserProfile!!.email)
                                        requestJsonObject.put("email",cachedUserProfile!!.email)

                                        val mediaType = "application/json; charset=utf-8".toMediaType()
                                        val requestBody = requestJsonObject.toString().toRequestBody(mediaType)

                                        val request = Request.Builder()
                                            .url(url)
                                            .post(requestBody)
                                            .build()

                                        client.newCall(request).enqueue(object : okhttp3.Callback {
                                            @RequiresApi(Build.VERSION_CODES.Q)
                                            override fun onResponse(call: Call, response: Response) {
                                                val responseData = response.body?.string()
                                                // log the response (either inserted or already there from before) and navigate
                                                Log.d(TAG, "Register API Response: $responseData")
                                                Handler(Looper.getMainLooper()).post {
                                                    navController.navigate(R.id.action_navigation_login_entry_to_navigation_home)
                                                }
                                            }

                                            override fun onFailure(call: Call, e: IOException) {
                                                Log.e(TAG, "Register API request was unsuccessful ${e.message}")
                                                // navigate in anyway since main keepervision auth is verified anyways, in case API is down
                                                Handler(Looper.getMainLooper()).post {
                                                    navController.navigate(R.id.action_navigation_login_entry_to_navigation_home)
                                                }
                                            }
                                        })
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Exception: ", e)
                                    } finally {
                                        connection.disconnect()
                                    }
                                }
                            }

                        })
                }
            })
    }

    fun getUserEmail() : String? {
         return cachedUserProfile?.email
    }

    fun isLoggedIn(): Boolean {
        return cachedCredentials != null
    }

    fun logout(ctx: Context, navController: NavController) {
        account = Auth0(
            ctx.getString(R.string.com_auth0_client_id),
            ctx.getString(R.string.com_auth0_domain)
        )

        WebAuthProvider
            .logout(account)
            .withScheme(ctx.getString(R.string.com_auth0_scheme))
            .start(ctx, object : Callback<Void?, AuthenticationException> {

                override fun onFailure(error: AuthenticationException) {
                    Log.d(TAG, "Logout Failed $error")
                }

                override fun onSuccess(result: Void?) {
                    cachedCredentials = null
                    cachedUserProfile = null
                    Log.d(TAG, "Logout Success")
                    navController.navigate(R.id.action_navigation_settings_to_navigation_login_entry)
                }

            })
    }


}