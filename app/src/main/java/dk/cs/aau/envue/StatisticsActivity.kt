package dk.cs.aau.envue

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.FloatingActionButton

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {

            // Pass on viewership data to the ViewershipFragment
            val viewership = ViewershipFragment()
            viewership.arguments = intent.extras
            supportFragmentManager
                .beginTransaction()
                .add(R.id.viewership_fragment, viewership)
                .commit()
        }

        setContentView(R.layout.activity_statistics)
    }
}
