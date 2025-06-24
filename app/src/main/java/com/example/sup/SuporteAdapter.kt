package com.example.sup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView

class SuporteAdapter(
    private val context: Context,
    private val dataSource: List<SupportItem>,
    private val onCloseTicketClicked: (String) -> Unit
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
            rowView = inflater.inflate(R.layout.list_item_suporte, parent, false)
            holder = ViewHolder()
            holder.titleTextView = rowView.findViewById(R.id.textViewItemTitle)
            holder.statusTextView = rowView.findViewById(R.id.textViewItemStatus)
            holder.closeButton = rowView.findViewById(R.id.buttonCloseTicket)
            rowView.tag = holder
        } else {
            rowView = convertView
            holder = rowView.tag as ViewHolder
        }

        val item = getItem(position) as SupportItem

        holder.titleTextView?.text = item.motive
        holder.statusTextView?.text = "Status: ${item.status}"

        holder.closeButton?.setOnClickListener {
            onCloseTicketClicked(item.id)
        }

        if (item.status == "Fechado") {
            holder.statusTextView?.setTextColor(context.resources.getColor(R.color.light_grey))
            holder.closeButton?.visibility = View.GONE
        } else {
            holder.statusTextView?.setTextColor(context.resources.getColor(R.color.white))
            holder.closeButton?.visibility = View.VISIBLE
        }

        return rowView
    }

    private class ViewHolder {
        var titleTextView: TextView? = null
        var statusTextView: TextView? = null
        var closeButton: Button? = null
    }
}