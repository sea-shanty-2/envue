package dk.cs.aau.envue

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.Button

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {

            // Pass on viewership data to the ViewershipFragment
            val viewership = ViewershipActivity()
            viewership.arguments = intent.extras
            supportFragmentManager
                .beginTransaction()
                .add(R.id.viewership_fragment, viewership)
                .commit()
        }

        setContentView(R.layout.activity_statistics)


        findViewById<FloatingActionButton>(R.id.close_statistics_button).setOnClickListener {
            onBackPressed()  // TODO: Do something more like calculate score first :-)
        }
    }
}
