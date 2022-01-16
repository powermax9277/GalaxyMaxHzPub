
package com.tribalfs.gmh.resochanger
/*
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tribalfs.gmh.R.id
import com.tribalfs.gmh.R.layout
import com.tribalfs.gmh.databinding.ActivityResoBinding
import com.tribalfs.gmh.helpers.UtilsRefreshRateSt
import com.tribalfs.gmh.profiles.ResolutionDetails



class ResolutionActivity : AppCompatActivity(), MyRecyclerViewAdapter.ItemClickListener  {

    private lateinit var mBinding: ActivityResoBinding
    private val mUtilsRefreshRate by lazy{ UtilsRefreshRateSt.instance(applicationContext)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, layout.activity_reso)

        val data = mUtilsRefreshRate.getResolutionsForKey(null)?.get(0)?.map{
            it.value
        }

        val recyclerView = mBinding.rvResos
        val numberOfColumns = 2
        recyclerView.layoutManager = GridLayoutManager(this, numberOfColumns)
        val adapter = MyRecyclerViewAdapter(this, data!!)
        adapter.setClickListener(this)
        recyclerView.adapter = adapter
    }

    override fun onItemClick(view: View?, position: Int) {
        //Log.i("TAG", "You clicked number " + adapter.getItem(position) + ", which is at cell position " + position);
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val qsTile: ComponentName? = intent?.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        Log.d("TESTTEST", "qsTile: ${qsTile?.className}")
    }

}

private class MyRecyclerViewAdapter internal constructor(context: Context?, data: List<ResolutionDetails>) :
    RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {
    private val mData: List<ResolutionDetails> = data
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    // data is passed into the constructor

    // inflates the cell layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = mInflater.inflate(layout.reso_cell, parent, false)
        return ViewHolder(view)
    }

    // binds the data to the TextView in each cell
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.myTextView.text = "${mData[position].resName}[${mData[position].resStrLxw}]"
    }

    // total number of cells
    override fun getItemCount(): Int {
        return mData.size
    }

    // stores and recycles views as they are scrolled off screen
    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var myTextView: TextView = itemView.findViewById(id.info_text)

        override fun onClick(view: View?) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): String {
        return mData[id].resStrLxw
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }


}*/
