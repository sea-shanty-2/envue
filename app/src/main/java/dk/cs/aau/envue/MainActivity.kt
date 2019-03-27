package dk.cs.aau.envue

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.facebook.AccessToken
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 42
    private val BASE_URL="http://envue.me:8000/graphql"

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!AccessToken.isCurrentAccessTokenActive()) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }

        try {
            val info = packageManager.getPackageInfo("dk.cs.aau.envue", PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: PackageManager.NameNotFoundException) {

        } catch (e: NoSuchAlgorithmException) {

        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(my_toolbar)

        /*
        ///
        /// Working API example
        ///

        val okHttpClient = OkHttpClient.Builder().build()
        val apolloCient = ApolloClient.builder().serverUrl(BASE_URL).okHttpClient(okHttpClient).build()

        val accountsQuery = AccountsQuery.Builder().first(5).build()
        apolloCient.query(accountsQuery).enqueue(object: ApolloCall.Callback<AccountsQuery.Data>() {

            override fun onResponse(response: Response<AccountsQuery.Data>) {
               runOnUiThread {

                   response.data()
                       ?.accounts()
                       ?.page()
                       ?.items()
                       ?.forEach {
                           AlertDialog.Builder(this@MainActivity)
                               .setMessage(it.fullName())
                               .create()
                               .show()
                       }


               }
            }

            override fun onFailure(e: ApolloException) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })*/

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_profile -> {
            // User chose the "Settings" item, show the app settings UI...
            startActivity(Intent(this, ProfileActivity::class.java))
            true
        }
        R.id.action_broadcast -> {
            if (ensurePermissionsGranted(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))) {
                startActivity(Intent(this, BroadcastActivity::class.java))
            }

            true
        }
        R.id.action_player -> {
            startActivity(Intent(this, PlayerActivity::class.java))

            true
        }
        R.id.action_map -> {
            startActivity(Intent(this, MapActivity::class.java))

            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun ensurePermissionsGranted(permissions: Array<String>): Boolean {
        if (!arePermissionsGranted(permissions)) {
            requestPermissions(permissions)
        }

        return arePermissionsGranted(permissions)
    }

    private fun arePermissionsGranted(permissions: Array<String>): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PermissionChecker.PERMISSION_GRANTED
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

}
