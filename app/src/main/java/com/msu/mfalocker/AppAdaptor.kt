package com.msu.mfalocker

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.msu.mfalocker.databinding.AppLayoutBinding

class AppAdaptor(private var activity: Activity, private var appList: ArrayList<MainActivity.App>, private var type: String, private var lockedAppsList: ArrayList<String>):
    RecyclerView.Adapter<AppAdaptor.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppAdaptor.ViewHolder {
        val id = AppLayoutBinding.inflate(LayoutInflater.from(activity), parent, false)
        return ViewHolder(id)
    }

    override fun onBindViewHolder(holder: AppAdaptor.ViewHolder, position: Int) { holder.bind(position) }
    override fun getItemCount(): Int { return appList.size }
    override fun getItemId(position: Int): Long { return position.toLong() }
    override fun getItemViewType(position: Int): Int { return position }

    inner class ViewHolder(private var id: AppLayoutBinding): RecyclerView.ViewHolder(id.root) {
        fun bind(position: Int) {
            val app = appList[position]

            id.icon.setImageDrawable(app.icon)
            id.name.text = app.appName
            id.packageName.text = app.packageName

            if (type == "filter") {
                id.switchLock.visibility = View.GONE
                id.intro.visibility = View.VISIBLE
                id.intro.text = app.status

                if (position != 0) {
                    val previousApp = appList[position - 1]
                    if (previousApp.status == app.status) id.intro.visibility = View.GONE
                }
            }
            else if (type == "none") {
                id.intro.visibility = View.GONE
                id.switchLock.visibility = View.GONE
            }
            else {
                id.intro.visibility = View.GONE
                id.switchLock.visibility = View.VISIBLE

                if (app.status == "Locked") locked()
                else unlocked()

                id.switchLock.setOnClickListener {
                    if (app.status == "Locked") {
                        app.status = "Unlocked"
                        unlocked()
                        lockedAppsList.remove(app.packageName)
                    }
                    else {
                        app.status = "Locked"
                        locked()
                        lockedAppsList.add(app.packageName)
                    }
                }
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        private fun unlocked() {
            id.imageView2.setColorFilter(activity.getColor(R.color.grey))
            id.imageView2.setImageDrawable(activity.getDrawable(R.drawable.unlock))
            id.view.setBackgroundResource(R.drawable.outline)
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        private fun locked() {
            id.imageView2.setColorFilter(activity.getColor(android.R.color.holo_purple))
            id.imageView2.setImageDrawable(activity.getDrawable(R.drawable.lock))
            id.view.setBackgroundColor(activity.getColor(R.color.grey_light))
        }
    }
}