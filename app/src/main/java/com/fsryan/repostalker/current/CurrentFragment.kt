package com.fsryan.repostalker.current

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.fsryan.repostalker.App
import com.fsryan.repostalker.R
import com.fsryan.repostalker.ViewUtil
import com.fsryan.repostalker.current.event.CurrentViewEvent
import com.fsryan.repostalker.rx.AlarmingDisposableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_current_member.*
import javax.inject.Inject

class CurrentFragment : Fragment() {

    @Inject
    internal lateinit var presenter: Current.Presenter

    private val compositeDisposable = CompositeDisposable()

    companion object {
        fun create(): CurrentFragment = CurrentFragment()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        App.componentsOf(context!!).currentFragementComponent().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_current_member, container, false)
        return view
    }

    override fun onStart() {
        super.onStart()
        compositeDisposable.add(presenter.eventObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: AlarmingDisposableObserver<CurrentViewEvent>() {
                override fun onNext(event: CurrentViewEvent) {
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

    private fun render(event: CurrentViewEvent) {
        if (event.showUserDetails()) {
            ViewUtil.setVisibility(View.VISIBLE, memberLoginText, memberLocationText, memberEmailText, memberAvatarImage)
            memberLoginText.text = event.userDetails?.userName
            memberLocationText.text = event.userDetails?.location
            memberEmailText.text = event.userDetails?.email
            memberAvatarImage.setImageBitmap(ViewUtil.bytesToBitmap(event.userDetails?.avatarBytes))
        }

        if (event.hideUserDetails()) {
            ViewUtil.setVisibility(View.GONE, memberLoginText, memberLocationText, memberEmailText, memberAvatarImage)
        }

        if (event.showEmpty) {
            currentMemberDataEmptyText.visibility = View.VISIBLE
        }

        if (event.hideEmpty()) {
            currentMemberDataEmptyText.visibility = View.GONE
        }

        if (event.showLoading()) {
            currentMemberProgressSpinner.visibility = View.VISIBLE
        }

        if (event.hideLoading()) {
            currentMemberProgressSpinner.visibility = View.GONE
        }
    }
}