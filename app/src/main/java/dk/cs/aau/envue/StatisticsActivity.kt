package dk.cs.aau.envue

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class StatisticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        if (savedInstanceState == null) {
            // Get score
            val score = intent.extras?.getInt("score")
            score?.let { findViewById<TextView>(R.id.score_increased)?.text = getString(R.string.score_increased, it) }

            // Pass on viewership data to the ViewershipFragment
            val viewership = ViewershipFragment()
            viewership.arguments = intent.extras
            supportFragmentManager
                .beginTransaction()
                .add(R.id.viewership_fragment, viewership)
                .commit()
        }

        findViewById<FloatingActionButton>(R.id.close_statistics_button).setOnClickListener {
            onBackPressed()  // TODO: Do something more like calculate score first :-)
        }
    }
}
