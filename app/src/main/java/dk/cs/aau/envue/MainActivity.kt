package dk.cs.aau.envue

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.facebook.AccessToken
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE_BROADCAST = 42
    companion object {
        internal const val SET_FILTERS_REQUEST = 57
    }

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
        // Update map on button press
        findViewById<FloatingActionButton>(R.id.update_map_button).setOnClickListener {
            (supportFragmentManager.findFragmentById(R.id.map_fragment) as MapActivity).updateMap()
        }

        // Update map. Needed to handle orientation changes.
        (supportFragmentManager.findFragmentById(R.id.map_fragment) as MapActivity).updateMap()

        // Open category selection on button press
        filter_categories_button.setOnClickListener() { this.onFilter()}
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
            if(ensurePermissionsGranted(arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION))) {
                startActivity(Intent(this, InitializeBroadcastActivity::class.java))
            }
            true
        }
        R.id.action_player -> {
            startActivity(Intent(this, BrowseEventsActivity::class.java))
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
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE_BROADCAST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_REQUEST_CODE_BROADCAST -> if(grantResults.contains(PackageManager.PERMISSION_DENIED)){
                //startActivity(Intent(this, MainActivity::class.java))
            }
            else { startActivity(Intent(this, InitializeBroadcastActivity::class.java)) }
        }
    }

    private fun onFilter() {
        val intent = Intent(this, FilterActivity::class.java)
        startActivityForResult(intent, SET_FILTERS_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            SET_FILTERS_REQUEST ->
                if (resultCode == Activity.RESULT_OK) {
                    var categories = data?.getDoubleArrayExtra(resources.getString(R.string.filter_response_key))
                    //if (categories != null) Snackbar.make(findViewById<FloatingActionButton>(R.id.update_map_button),
                    // categories!!.contentToString(),
                    // Snackbar.LENGTH_LONG).show()
                    if (categories != null && !categories.contains(1.0)) categories = null
                    (supportFragmentManager.findFragmentById(R.id.map_fragment) as MapActivity).updateFilters(categories)
                }
        }
    }
}
