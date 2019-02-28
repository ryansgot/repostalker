package com.fsryan.repostalker.main

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import com.fsryan.repostalker.App
import com.fsryan.repostalker.rx.AlarmingDisposableObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import android.view.MenuItem
import com.fsryan.repostalker.R
import kotlinx.android.synthetic.main.activity_main.*
import android.view.Menu
import android.widget.Toast
import com.fsryan.repostalker.current.CurrentFragment
import com.fsryan.repostalker.followerlist.FollowerListFragment
import com.fsryan.repostalker.main.event.MainViewEvent
import kotlinx.android.synthetic.main.settings_dialog.view.*

class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var presenter: Main.Presenter

    private val compositeDisposable = CompositeDisposable()

    private var settingsDialog: AlertDialog? = null

    companion object {
        private const val EXTRA_PREVENT_LOADING_FRAGMENTS = "prevent_loading_fragments"

        fun intent(context: Context, preventLoadingFragments: Boolean = false) = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_PREVENT_LOADING_FRAGMENTS, preventLoadingFragments)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setTitle(R.string.app_name)

        App.componentsOf(this).mainActivityComponent().inject(this)
    }

    override fun onStart() {
        super.onStart()
        compositeDisposable.add(presenter.eventObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: AlarmingDisposableObserver<MainViewEvent>() {
                override fun onNext(event: MainViewEvent) {
                    render(event)
                }
            }))
        presenter.onReady()

        if (intent.getBooleanExtra(EXTRA_PREVENT_LOADING_FRAGMENTS, false)) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.currentMemberFragmentContainer, CurrentFragment.create())
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.memberListFragmentContainer, FollowerListFragment.create())
            .commit()
    }

    override fun onStop() {
        presenter.onUnready()
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.menu_settings -> {
                presenter.userRequestedSettings()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        presenter.userRequestedBackNav()
    }

    private fun render(event: MainViewEvent) {
        if (event.navBack) {
            super.onBackPressed()
        }
        
        if (event.hideSettings) {
            settingsDialog?.dismiss()
            settingsDialog = null
        }

        if (event.showSettings) {
            showSettings(event.cacheInvalidationInterval)
        }

        if (event.showErrorMessage()) {
            Toast.makeText(this, event.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSettings(currentInvalidationInterval: Long) {
        if (settingsDialog != null) {
            settingsDialog?.dismiss()
        }

        val dialogView = layoutInflater.inflate(R.layout.settings_dialog, null, false)
        val inputText = dialogView.settings_dialog_invalidation_interval_entry_text
        inputText.setText(currentInvalidationInterval.toString())

        settingsDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_dialog_title)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(R.string.cancel) { _, _ -> presenter.userCanceledSettings() }
            .setPositiveButton(R.string.save) { _, _ -> presenter.userSavedSettings(inputText.text.toString()) }
            .create()
        settingsDialog?.show()
    }
}
