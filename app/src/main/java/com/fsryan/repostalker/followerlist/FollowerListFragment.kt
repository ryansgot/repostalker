package com.fsryan.repostalker.followerlist

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.fsryan.repostalker.App
import com.fsryan.repostalker.R
import com.fsryan.repostalker.ViewUtil
import com.fsryan.repostalker.followerlist.event.FollowerDetails
import com.fsryan.repostalker.followerlist.event.FollowerListViewEvent
import com.fsryan.repostalker.rx.AlarmingDisposableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_follower_list.*
import kotlinx.android.synthetic.main.fragment_follower_list.view.*
import kotlinx.android.synthetic.main.item_follower.view.*
import javax.inject.Inject

class FollowerListFragment : Fragment() {

    @Inject
    lateinit var presenter: FollowerList.Presenter

    private val recyclerAdapter = FollowersRecyclerAdapter()

    private val compositeDisposable = CompositeDisposable()

    companion object {
        fun create(): FollowerListFragment = FollowerListFragment()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        App.componentsOf(context!!).followerListFragmentComponent().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_follower_list, container, false)
        val layoutManager = LinearLayoutManager(view.context)
        view.followerRecyclerView.layoutManager = layoutManager
        view.followerRecyclerView.adapter = recyclerAdapter

        view.followerFilterButton.setOnClickListener {
            presenter.userRequestedFollowerListFilter(filterTextEntry.text.toString())
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        compositeDisposable.add(presenter.eventObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: AlarmingDisposableObserver<FollowerListViewEvent>() {
                override fun onNext(event: FollowerListViewEvent) {
                    render(event)
                }
            }))
        presenter.onReady()
    }

    override fun onStop() {
        presenter.onUnready()
        compositeDisposable.clear()
        super.onStop()
    }

    private fun render(event: FollowerListViewEvent) {
        if (event.disableFiltering) {
            filterTextEntry.isEnabled = false
            followerFilterButton.isEnabled = false
        }
        if (event.enableFiltering) {
            filterTextEntry.isEnabled = true
            followerFilterButton.isEnabled = true
        }
        if (event.clearFilterText) {
            filterTextEntry.setText("")
        }

        if (event.showList) {
            followerRecyclerView.visibility = View.VISIBLE
        }
        if (event.hideList()) {
            followerRecyclerView.visibility = View.GONE
        }
        if (event.clearList) {
            recyclerAdapter.clear()
        }
        if (event.hasNewFollowerDetails()) {
            recyclerAdapter.add(event.followerDetails!!)
        }

        if (event.showEmpty) {
            emptyFollowersText.visibility = View.VISIBLE
        }
        if (event.hideEmpty()) {
            emptyFollowersText.visibility = View.GONE
        }

        if (event.showLoading) {
            followerListProgressSpinner.visibility = View.VISIBLE
        }
        if (event.hideLoading()) {
            followerListProgressSpinner.visibility = View.GONE
        }

        if (event.showErrorMessage()) {
            Toast.makeText(activity, event.errorMessage!!, Toast.LENGTH_LONG).show()
        }
    }

    private inner class FollowersRecyclerAdapter: RecyclerView.Adapter<FollowerViewHolder>() {
        val data = mutableListOf<FollowerDetails>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_follower, parent, false)
            return FollowerViewHolder(v)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(viewHolder: FollowerViewHolder, position: Int) {
            viewHolder.attachData(data[position])
        }

        fun clear() {
            data.clear()
            notifyDataSetChanged()
        }

        fun add(followerDetails: FollowerDetails) {
            data.add(followerDetails)
            notifyItemInserted(data.size - 1)
        }
    }

    private inner class FollowerViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        fun attachData(followerDetails: FollowerDetails) {
            if (followerDetails.hasAvatar()) {
                itemView.followerItemAvatarImage.setImageBitmap(ViewUtil.bytesToBitmap(followerDetails.avatarBytes))
            }
            if (followerDetails.hasEmail()) {
                itemView.followerItemEmail.text = followerDetails.email
            }
            if (followerDetails.hasLocation()) {
                itemView.followerItemLocation.text = followerDetails.location
            }

            itemView.followerItemUserName.text = followerDetails.userName
            itemView.setOnClickListener {
                presenter.userRequestedFollower(followerDetails.userName)
            }
        }
    }
}