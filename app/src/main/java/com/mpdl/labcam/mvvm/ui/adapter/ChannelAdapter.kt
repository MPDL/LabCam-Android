package com.mpdl.labcam.mvvm.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.mpdl.labcam.R
import com.mpdl.labcam.mvvm.repository.bean.ChannelBean

class ChannelAdapter(val context:Context, var data:List<ChannelBean>): BaseAdapter() {


    override fun getView(position: Int, p1: View?, viewGroup: ViewGroup?): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_channel,viewGroup!!,false)
        view?.let {
            val tvName = it.findViewById<TextView>(R.id.tv_name)
            val tvUrl = it.findViewById<TextView>(R.id.tv_url)
            val vLine = it.findViewById<View>(R.id.v_line)

            tvName.text = data[position].name
            tvUrl.text = data[position].url

            if (position == count-1){
                vLine.visibility = View.INVISIBLE
            }else{
                vLine.visibility = View.VISIBLE
            }
        }
        return view
    }

    override fun getItem(position: Int): Any = data.get(position)

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = data.size
}