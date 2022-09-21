package com.shexa.baseproject.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.shexa.baseproject.R
import com.shexa.baseproject.entities.WriteHistoryModel

class RvWriteDataHistoryAdapter(val context: Context, private val lstWriteData:List<WriteHistoryModel>) :
    RecyclerView.Adapter<RvWriteDataHistoryAdapter.RvWriteDataHistoryViewHolder>()
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvWriteDataHistoryViewHolder
    {
        val view = LayoutInflater.from(context).inflate(R.layout.item_write_data_history,parent,false)
        return RvWriteDataHistoryViewHolder(context,view)
    }

    override fun onBindViewHolder(holder: RvWriteDataHistoryViewHolder, position: Int)
    {
        val model = lstWriteData[position]
        holder.tvData?.text = "${model.recordId} \n ${model.recordContent} \n ${model.recordType}"
    }

    override fun getItemCount(): Int
    {
        return lstWriteData.size
    }

    class RvWriteDataHistoryViewHolder(val context: Context, private val itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        var tvData : AppCompatTextView?=null
        init {
            tvData = itemView.findViewById(R.id.tv_write_data)
        }
    }

}