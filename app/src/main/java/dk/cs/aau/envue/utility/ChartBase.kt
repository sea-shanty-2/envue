package dk.cs.aau.envue.utility

import android.Manifest
import android.R
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity

import android.view.View
import android.widget.Toast

import com.github.mikephil.charting.charts.Chart

/**
 * Base class of all Activities of the Demo Application.
 *
 * @author Philipp Jahoda
 */
abstract class ChartBase : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    protected val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec")

    protected val parties = arrayOf(
        "Party A",
        "Party B",
        "Party C",
        "Party D",
        "Party E",
        "Party F",
        "Party G",
        "Party H",
        "Party I",
        "Party J",
        "Party K",
        "Party L",
        "Party M",
        "Party N",
        "Party O",
        "Party P",
        "Party Q",
        "Party R",
        "Party S",
        "Party T",
        "Party U",
        "Party V",
        "Party W",
        "Party X",
        "Party Y",
        "Party Z"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun getRandom(range: Float, start: Float): Float {
        return (Math.random() * range).toFloat() + start
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveToGallery()
            } else {
                Toast.makeText(context?.applicationContext, "Saving FAILED!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    protected fun requestStoragePermission(view: View) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this@ChartBase.activity?.parent!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(view, "Write permission is required to save image to gallery", Snackbar.LENGTH_INDEFINITE)
                .setAction(
                    android.R.string.ok
                ) {
                    ActivityCompat.requestPermissions(
                        this@ChartBase.activity?.parent!!,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_STORAGE
                    )
                }
                .show()
        } else {
            Toast.makeText(context?.applicationContext, "Permission Required!", Toast.LENGTH_SHORT)
                .show()
            ActivityCompat.requestPermissions(
                this@ChartBase.activity?.parent!!,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_STORAGE
            )
        }
    }

    protected fun saveToGallery(chart: Chart<*>, name: String) {
        if (chart.saveToGallery(name + "_" + System.currentTimeMillis(), 70))
            Toast.makeText(
                context?.applicationContext, "Saving SUCCESSFUL!",
                Toast.LENGTH_SHORT
            ).show()
        else
            Toast.makeText(context?.applicationContext, "Saving FAILED!", Toast.LENGTH_SHORT)
                .show()
    }

    protected abstract fun saveToGallery()

    companion object {

        private val PERMISSION_STORAGE = 0
    }
}