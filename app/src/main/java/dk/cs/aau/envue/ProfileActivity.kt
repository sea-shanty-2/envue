package dk.cs.aau.envue

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.facebook.Profile
import com.facebook.login.LoginManager
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import dk.cs.aau.envue.transformers.CircleTransform

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val profile = Profile.getCurrentProfile()

        // register log out button listener
        logOutButton.setOnClickListener { this.onLogOut() }

        val profilePicture = profile.getProfilePictureUri(1024, 1024)
        val profileName = profile.name

        Picasso
            .get()
            .load(profilePicture)
            .placeholder(R.drawable.ic_profile_picture_placeholder)
            .error(R.drawable.ic_profile_picture_placeholder)
            .resize(256, 256)
            .transform(CircleTransform())
            .into(profilePictureView)

        profileNameView.text = profileName
    }



    private fun onLogOut() {
        val profile = Profile.getCurrentProfile()

        AlertDialog.Builder(this)
            .setMessage( resources.getString(R.string.com_facebook_loginview_logged_in_as, profile.name))
            .setCancelable(true)
            .setPositiveButton(R.string.com_facebook_loginview_log_out_button) { _: DialogInterface, _: Int -> run{
                LoginManager.getInstance().logOut()
                startActivity(Intent(this, LoginActivity::class.java))
            }}
            .create()
            .show()
    }

}