package com.keepervision.ui.metrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.keepervision.R
import com.keepervision.databinding.FragmentMetricsBinding
import com.keepervision.ui.metrics.adapter.ItemAdapter
import com.keepervision.ui.metrics.data.Datasource
import com.keepervision.ui.metrics.model.SessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MetricsFragment : Fragment() {

    private var _binding: FragmentMetricsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_metrics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.metrics_recycler_view)

        GlobalScope.launch(Dispatchers.IO) {
            val myDataset = Datasource().loadSessions(requireContext())
            val adapter = ItemAdapter(requireContext(), myDataset)
            adapter.onCardClick = {sessionEntry ->
                val bundle = Bundle()
                val totalDirectives = sessionEntry.frontCount + sessionEntry.frontLeftCount + sessionEntry.leftCount + sessionEntry.backLeftCount + sessionEntry.backCount + sessionEntry.backRightCount + sessionEntry.rightCount + sessionEntry.frontRightCount;
                bundle.putString("dateTimestamp", sessionEntry.dateTimestamp)
                bundle.putString("rating", calc_rating(totalDirectives))
                bundle.putString("totalDirectives", totalDirectives.toString())
                bundle.putString("front", sessionEntry.frontCount.toString())
                bundle.putString("frontLeft", sessionEntry.frontLeftCount.toString())
                bundle.putString("left", sessionEntry.leftCount.toString())
                bundle.putString("backLeft", sessionEntry.backLeftCount.toString())
                bundle.putString("back", sessionEntry.backCount.toString())
                bundle.putString("backRight", sessionEntry.backRightCount.toString())
                bundle.putString("right", sessionEntry.rightCount.toString())
                bundle.putString("frontRight", sessionEntry.frontRightCount.toString())
                bundle.putString("initialImage", sessionEntry.startPicture)
                bundle.putString("finalImage", sessionEntry.endPicture)
                bundle.putString("analysis", calc_analysis(sessionEntry))
                activity?.runOnUiThread {
                    findNavController().navigate(
                        R.id.navigation_session_expanded_metrics,
                        bundle
                    )
                }
            }

            activity?.runOnUiThread {
                recyclerView.adapter = adapter
            }
        }
    }

    fun calc_rating(totalDirectives: Int): String {
        return when {
            totalDirectives <= 3 -> "Professional"
            totalDirectives <= 6 -> "Expert"
            totalDirectives <= 9 -> "Intermediate"
            totalDirectives <= 12 -> "Novice"
            else -> "Beginner"
        }
    }

    fun calc_analysis(sessionInfo: SessionInfo) :String {
        val directions = listOf(
            "Front", "Back", "Left", "Right", "FrontRight", "FrontLeft", "BackRight", "BackLeft"
        )

        // Find the direction with the highest count
        val maxDirection = directions.maxByOrNull { direction ->
            when (direction) {
                "Front" -> sessionInfo.frontCount
                "Back" -> sessionInfo.backCount
                "Left" -> sessionInfo.leftCount
                "Right" -> sessionInfo.rightCount
                "FrontRight" -> sessionInfo.frontRightCount
                "FrontLeft" -> sessionInfo.frontLeftCount
                "BackRight" -> sessionInfo.backRightCount
                "BackLeft" -> sessionInfo.backLeftCount
                else -> 0 // Default to 0 if direction not recognized
            }
        }
        return when (maxDirection) {
            "Front" -> "You are positioned too far in front of the goal. This can leave the goal area behind you vulnerable. Try moving back to cover more area and be ready for shots from a distance. Your anticipation will improve, making it harder for attackers to catch you off guard."
            "Back" -> "You are positioned too close to the goal line. Being too far back reduces your reaction time and leaves more of the goal exposed. Consider moving forward to engage attackers and narrow their shooting angles. This proactive approach will give you more control over the game."
            "Left" -> "You are positioned too far to the left. This unbalanced positioning can make it challenging to cover the right side of the goal effectively. Work on balancing your positioning to cover both sides evenly. By doing so, you'll create a solid foundation for a well-rounded defense."
            "Right" -> "You are positioned too far to the right. This unbalanced positioning can make it challenging to cover the left side of the goal effectively. Work on balancing your positioning to cover both sides evenly. Achieving symmetry in your stance will enhance your overall goalkeeping capabilities."
            "FrontRight" -> "You are positioned too far forward and to the right. This unbalanced positioning can make it challenging to cover the left side of the goal effectively. Work on balancing your positioning to cover both sides evenly. By finding the right balance, you'll become a formidable presence in the goal area."
            "FrontLeft" -> "You are positioned too far forward and to the left. This unbalanced positioning can compromise your ability to cover the right side of the goal effectively. Work on balancing your positioning to cover both sides evenly. Your adaptability will increase, making it harder for opponents to exploit your position."
            "BackRight" -> "You are positioned too far back and to the right. This unbalanced positioning can make it challenging to cover the left side of the goal effectively. Work on balancing your positioning to cover both sides evenly. A symmetrical stance will enhance your agility and responsiveness."
            else -> "You are positioned too far back and to the left. This unbalanced positioning can make it challenging to cover the right side of the goal effectively. Work on balancing your positioning to cover both sides evenly. Striving for equilibrium will improve your ability to defend against shots from all directions."
        }
    }
}