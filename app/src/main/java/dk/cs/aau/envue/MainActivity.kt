package dk.cs.aau.envue

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.facebook.AccessToken
import dk.cs.aau.envue.pager.BottomBarAdapter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE_BROADCAST = 42

    private lateinit var mapFragment: MapFragment
    private lateinit var profileFragment: ProfileFragment

    companion object {
        internal const val SET_FILTERS_REQUEST = 57
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        if (!AccessToken.isCurrentAccessTokenActive()) {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))

            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create fragments
        mapFragment = MapFragment()
        profileFragment = ProfileFragment()

        val adapter = BottomBarAdapter(supportFragmentManager)
        adapter.addFragment(mapFragment)
        adapter.addFragment(profileFragment)
        findViewById<ViewPager>(R.id.view_pager)?.apply {
            this.adapter = adapter
        }

        bottom_navigation?.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_profile -> {
                    view_pager?.currentItem = 1
                    true
                }
                R.id.action_map -> {
                    view_pager?.currentItem = 0
                    true
                }
                R.id.action_broadcast -> {
                    if(ensurePermissionsGranted(arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION))) {
                        startActivity(Intent(this, InitializeBroadcastActivity::class.java))
                    }
                    false
                }
                else -> false
            }
        }

        // Update map. Needed to handle orientation changes.
        mapFragment.updateMap()
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
}
