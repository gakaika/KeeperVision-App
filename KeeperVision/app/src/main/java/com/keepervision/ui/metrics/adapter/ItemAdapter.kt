package com.keepervision.ui.metrics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.keepervision.R
import com.keepervision.ui.metrics.model.SessionInfo

class ItemAdapter(private val context: Context,  private val dataset: List<SessionInfo>)
    : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    // The click listener
    var onCardClick: ((SessionInfo) -> Unit)? = null

    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.card_view)
        val sessionTimeView: TextView = view.findViewById(R.id.session_time)
        val sessionDirectivesView : TextView = view.findViewById(R.id.session_directives)
        val sessionSessionRating: TextView = view.findViewById(R.id.session_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.session_list_item, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        val totalDirectives = item.backRightCount + item.backCount + item.backLeftCount + item.leftCount + item.frontLeftCount + item.frontCount + item.frontRightCount + item.rightCount;
        holder.sessionTimeView.text = item.dateTimestamp
        holder.sessionDirectivesView.text = "Directives Issued: " + totalDirectives;
        holder.sessionSessionRating.text = "Rating: " + calc_rating(totalDirectives)
        holder.cardView.setOnClickListener {
            onCardClick?.invoke(dataset[position])
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

    override fun getItemCount(): Int {
        return dataset.size
    }
}