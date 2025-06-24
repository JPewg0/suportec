package com.example.sup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

class ClienteTicketAdapter(
    private val context: Context,
    private val dataSource: List<SupportItem>
) : BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView: View
        val holder: ViewHolder

        if (convertView == null) {
            rowView = inflater.inflate(R.layout.list_item_cliente_ticket, parent, false)
            holder = ViewHolder()
            holder.motiveTextView = rowView.findViewById(R.id.textViewTicketMotive)
            holder.statusTextView = rowView.findViewById(R.id.textViewTicketStatus)
            rowView.tag = holder
        } else {
            rowView = convertView
            holder = rowView.tag as ViewHolder
        }

        val item = getItem(position) as SupportItem

        holder.motiveTextView.text = item.motive
        holder.statusTextView.text = "Status: ${item.status}"

        // Change color based on status
        val statusColor = when (item.status) {
            "Fechado" -> R.color.light_grey
            else -> R.color.white
        }
        holder.statusTextView.setTextColor(ContextCompat.getColor(context, statusColor))

        return rowView
    }

    private class ViewHolder {
        lateinit var motiveTextView: TextView
        lateinit var statusTextView: TextView
    }
}
