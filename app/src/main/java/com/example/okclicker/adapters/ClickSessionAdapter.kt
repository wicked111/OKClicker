package com.example.okclicker.adapters

import android.provider.Telephony.Mms.Part
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.okclicker.R
import com.example.okclicker.clickItem
import com.google.gson.Gson

class ClickSessionAdapter(
    private val sessions: MutableList<clickItem>,
    private val onDeleteClicked: (clickItem) -> Unit,
    private val onRunClicked: (clickItem) -> Unit
) : RecyclerView.Adapter<ClickSessionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventName: TextView = view.findViewById(R.id.item_event_name)
        val eventDate: TextView = view.findViewById(R.id.item_event_date)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
        val runButton:ImageButton = view.findViewById(R.id.run_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.clickitem_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        holder.eventName.text = session.eventName
        holder.eventDate.text = session.eventDate

        // Handle item click
        holder.itemView.setOnClickListener {
            val fullData = Gson().toJson(session)
            Toast.makeText(holder.itemView.context, fullData, Toast.LENGTH_SHORT).show()
        }

        holder.deleteButton.setOnClickListener{
            onDeleteClicked(session)

        }

        holder.runButton.setOnClickListener{
            onRunClicked(session)
        }
    }

    override fun getItemCount(): Int = sessions.size

    fun removeItem(ParticularItem: clickItem) {
        val position = sessions.indexOf(ParticularItem)
        if (position != -1) {
            sessions.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
