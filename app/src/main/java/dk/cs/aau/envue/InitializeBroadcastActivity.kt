package dk.cs.aau.envue

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_initialize_broadcast.*

class InitializeBroadcastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize_broadcast)

        startBroadcastButton.setOnClickListener { view ->
            Snackbar.make(view, "nibber", 2)
                .setAction("Action", null).show()
        }
    }
}
