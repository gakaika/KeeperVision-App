package com.keepervision.ui.metrics

import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.keepervision.R
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.squareup.picasso.Picasso
import java.util.*

class SessionExpandedFragment : Fragment() {
    private lateinit var radarChart: RadarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_session_expanded, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fill in arguments to views
        view.findViewById<TextView>(R.id.session_expanded_dateTimestamp).text = arguments?.getString("dateTimestamp");
        view.findViewById<TextView>(R.id.textSessionRating).text = "Rating: " + arguments?.getString("rating");
        view.findViewById<TextView>(R.id.textDirectivesIssued).text = "Total Directives Issued: " + arguments?.getString("totalDirectives");

        view.findViewById<TextView>(R.id.textSessionAnalysisMessage).text = arguments?.getString("analysis");

        view.findViewById<TextView>(R.id.textFront).text = "Front: " + arguments?.getString("front");
        view.findViewById<TextView>(R.id.textBack).text = "Back: " + arguments?.getString("back");
        view.findViewById<TextView>(R.id.textFrontRight).text = "Front Right: " + arguments?.getString("frontRight");
        view.findViewById<TextView>(R.id.textFrontLeft).text = "Front Left: " + arguments?.getString("frontLeft");
        view.findViewById<TextView>(R.id.textBackLeft).text = "Back Left: " + arguments?.getString("backLeft");
        view.findViewById<TextView>(R.id.textBackRight).text = "Back Right: " + arguments?.getString("backRight");
        view.findViewById<TextView>(R.id.textLeft).text = "Left: " + arguments?.getString("left");
        view.findViewById<TextView>(R.id.textRight).text = "Right: " + arguments?.getString("right");

        val intialImageView: ImageView = view.findViewById(R.id.initialImageView)
        Picasso.get().load(arguments?.getString("initialImage")).into(intialImageView)
        val finalImageView: ImageView = view.findViewById(R.id.finalImageView)
        Picasso.get().load(arguments?.getString("finalImage")).into(finalImageView)

        radarChart = view.findViewById(R.id.radarChart)
        radarChart.setRotationEnabled(false)
        setupRadarChart()
        setData()
    }

    private fun setupRadarChart() {
        radarChart.isHighlightPerTapEnabled = false
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val desiredWidth = (0.9 * screenWidth).toInt()

        val legend = radarChart.legend
        legend.isEnabled = false

        val layoutParams = radarChart.layoutParams
        layoutParams.width = desiredWidth
        layoutParams.height = desiredWidth
        radarChart.layoutParams = layoutParams

        radarChart.description.isEnabled = false
        radarChart.webLineWidth = 1f
        radarChart.webColor = Color.LTGRAY
        radarChart.webLineWidthInner = 1f
        radarChart.webColorInner = Color.LTGRAY
        radarChart.webAlpha = 100

        val yAxis: YAxis = radarChart.yAxis
        yAxis.axisMinimum = 0f
        yAxis.setDrawLabels(true)
        yAxis.isGranularityEnabled = true
        yAxis.granularity = 1.0f
        yAxis.textColor = ContextCompat.getColor(requireContext(), R.color.dark_green)

        val labels = arrayOf("F", "FR", "R", "BR", "B", "BL", "L", "FL")
        val xAxis: XAxis = radarChart.xAxis
        xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.dark_green)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt() % labels.size
                return labels[index]
            }
        }
    }
    private fun setData() {
        val entries = ArrayList<RadarEntry>()
        entries.add(RadarEntry(arguments?.getString("front")?.toFloat() ?: 0f, "F"))
        entries.add(RadarEntry(arguments?.getString("frontRight")?.toFloat()!! ?: 0f, "FR"))
        entries.add(RadarEntry(arguments?.getString("right")?.toFloat()!! ?: 0f, "R"))
        entries.add(RadarEntry(arguments?.getString("backRight")?.toFloat()!! ?: 0f, "BR"))
        entries.add(RadarEntry(arguments?.getString("back")?.toFloat()!! ?: 0f, "B"))
        entries.add(RadarEntry(arguments?.getString("backLeft")?.toFloat()!! ?: 0f, "BL"))
        entries.add(RadarEntry(arguments?.getString("left")?.toFloat()!! ?: 0f, "L"))
        entries.add(RadarEntry(arguments?.getString("frontLeft")?.toFloat()!! ?: 0f, "FL"))

        val dataSet = RadarDataSet(entries, "Direction Data")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.dark_green)
        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.green)
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 180
        dataSet.lineWidth = 4f
        dataSet.valueTextSize = 12f
        dataSet.isDrawHighlightCircleEnabled = true
        val data = RadarData(dataSet)
        data.setDrawValues(false)
        radarChart.data = data
        radarChart.invalidate()

        radarChart.animateXY(1400, 1400, Easing.EaseInOutQuad)
    }
}
