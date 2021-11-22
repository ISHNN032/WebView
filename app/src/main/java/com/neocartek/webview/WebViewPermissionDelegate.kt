package com.neocartek.webview

import android.Manifest
import android.app.Activity
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat.requestPermissions
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import android.content.DialogInterface

import android.widget.LinearLayout

import android.R
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Spinner

import androidx.core.content.ContextCompat
import java.util.*
import android.widget.ScrollView
import android.view.ViewGroup

import android.widget.TextView

import android.widget.ArrayAdapter
import android.content.res.TypedArray

class WebViewPermissionDelegate(private var mActivity: Activity) : GeckoSession.PermissionDelegate {
    private var mCallback: GeckoSession.PermissionDelegate.Callback? = null

    fun onRequestPermissionsResult(
        permissions: Array<String?>?,
        grantResults: IntArray
    ) {
        if (mCallback == null) {
            return
        }
        val cb: PermissionDelegate.Callback = mCallback!!
        mCallback = null
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                // At least one permission was not granted.
                cb.reject()
                return
            }
        }
        cb.grant()
    }


    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>?,
        callback: GeckoSession.PermissionDelegate.Callback
    ) {
        mCallback = callback;
        requestPermissions(
            mActivity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
            PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: GeckoSession.PermissionDelegate.ContentPermission
    ): GeckoResult<Int>? {
        return super.onContentPermissionRequest(session, perm)
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback
    ) {
        // Reject permission if Android permission has been previously denied.
        // Reject permission if Android permission has been previously denied.
        if ((audio != null
                    && ContextCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED)
            || (video != null
                    && ContextCompat.checkSelfPermission(
                mActivity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            callback.reject()
            return
        }

        val host: String? = Uri.parse(uri).authority
        val title: String = when {
            audio == null -> {
                String.format("비디오 권한 요청 %s", host)
            }
            video == null -> {
                String.format("오디오 권한 요청 %s", host)
            }
            else -> {
                String.format("미디어 권한 요청 %s", host)
            }
        }

        // Get the media device name from the `MediaDevice`

        // Get the media device name from the `MediaDevice`
        val videoNames: Array<String?>? = normalizeMediaName(video)
        val audioNames: Array<String?>? = normalizeMediaName(audio)

        val builder: AlertDialog.Builder = AlertDialog.Builder(mActivity)

        // Create drop down boxes to allow users to select which device to grant permission to

        // Create drop down boxes to allow users to select which device to grant permission to
        val container: LinearLayout = addStandardLayout(builder, title, null)!!
        val videoSpinner: Spinner?
        if (video != null) {
            videoSpinner = addMediaSpinner(
                builder.context,
                container,
                video,
                videoNames
            ) // create spinner and add to alert UI
        } else {
            videoSpinner = null
        }

        val audioSpinner: Spinner?
        if (audio != null) {
            audioSpinner = addMediaSpinner(
                builder.context,
                container,
                audio,
                audioNames
            ) // create spinner and add to alert UI
        } else {
            audioSpinner = null
        }

        builder.setNegativeButton(R.string.cancel, null)
            .setPositiveButton(
                R.string.ok,
                DialogInterface.OnClickListener { _, _ -> // gather selected media devices and grant access
                    val videoS =
                        if (videoSpinner != null) videoSpinner.selectedItem as PermissionDelegate.MediaSource else null
                    val audioS =
                        if (audioSpinner != null) audioSpinner.selectedItem as PermissionDelegate.MediaSource else null
                    callback.grant(videoS, audioS)
                })

        val dialog: AlertDialog = builder.create()
        dialog.setOnDismissListener(DialogInterface.OnDismissListener { callback.reject() })
        dialog.show()
    }

    private fun normalizeMediaName(sources: Array<PermissionDelegate.MediaSource>?): Array<String?>? {
        if (sources == null) {
            return null
        }
        val res = arrayOfNulls<String>(sources.size)
        for (i in sources.indices) {
            val mediaSource = sources[i].source
            val name = sources[i].name
            if (PermissionDelegate.MediaSource.SOURCE_CAMERA == mediaSource) {
                if (name!!.lowercase(Locale.ENGLISH).contains("front")) {
                    res[i] = "front cam"
                } else {
                    res[i] = "back cam"
                }
            } else if (!name!!.isEmpty()) {
                res[i] = name
            } else if (PermissionDelegate.MediaSource.SOURCE_MICROPHONE == mediaSource) {
                res[i] = "microphone"
            } else {
                res[i] = "other"
            }
        }
        return res
    }

    private fun addStandardLayout(
        builder: AlertDialog.Builder, title: String, msg: String?
    ): LinearLayout? {
        val scrollView = ScrollView(builder.context)
        val container = LinearLayout(builder.context)
        val horizontalPadding: Int = getViewPadding(builder)
        val verticalPadding = if (msg == null || msg.isEmpty()) horizontalPadding else 0
        container.orientation = LinearLayout.VERTICAL
        container.setPadding( /* left */
            horizontalPadding,  /* top */verticalPadding,  /* right */
            horizontalPadding,  /* bottom */verticalPadding
        )
        scrollView.addView(container)
        builder.setTitle(title).setMessage(msg).setView(scrollView)
        return container
    }

    private fun addMediaSpinner(
        context: Context,
        container: ViewGroup,
        sources: Array<PermissionDelegate.MediaSource>,
        sourceNames: Array<String?>?
    ): Spinner? {
        val adapter: ArrayAdapter<PermissionDelegate.MediaSource?> = object :
            ArrayAdapter<PermissionDelegate.MediaSource?>(context, R.layout.simple_spinner_item) {
            private fun convertView(position: Int, view: View?): View? {
                if (view != null) {
                    val item = getItem(position)
                    (view as TextView).text = sourceNames?.get(position) ?: item!!.name
                }
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return convertView(position, super.getView(position, convertView, parent))!!
            }

            override fun getDropDownView(position: Int, view: View?, parent: ViewGroup?): View? {
                return convertView(position, super.getDropDownView(position, view, parent!!))
            }
        }
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        adapter.addAll(*sources)
        val spinner = Spinner(context)
        spinner.adapter = adapter
        spinner.setSelection(0)
        container.addView(spinner)
        return spinner
    }

    private fun getViewPadding(builder: AlertDialog.Builder): Int {
        val attr = builder
            .context
            .obtainStyledAttributes(intArrayOf(R.attr.listPreferredItemPaddingLeft))
        val padding = attr.getDimensionPixelSize(0, 1)
        attr.recycle()
        return padding
    }
}