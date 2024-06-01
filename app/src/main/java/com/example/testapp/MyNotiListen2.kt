package com.example.testapp

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import com.faendir.rhino_android.RhinoAndroidHelper
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import org.mozilla.javascript.Context
import com.google.android.gms.wearable.Wearable

class MyNotiListen2 : NotificationListenerService(), DataClient.OnDataChangedListener {

    companion object {
        var execContext: android.content.Context? = null
    }
    override fun onCreate() {
        super.onCreate()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this)
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        println("listenerConnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        println("MyNotiListen2 시작 ")
        //logActiveNotifications("답장답장답장")
        println("MyNotiListen2 끝 ")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        println("데이터 변경")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val buttonText = dataMapItem.dataMap.getString("button_text")
                logActiveNotifications(buttonText.toString())
            }
        }
    }

    public fun logActiveNotifications(myreply: String) {
        val activeNotifications = getActiveNotifications()
        for (notification in activeNotifications) {
            if (notification.packageName == "jp.naver.line.android") {
                val wExt = Notification.WearableExtender(notification?.notification)
                val action = wExt.actions.firstOrNull(){act ->
                    act.remoteInputs != null && act.remoteInputs.isNotEmpty() &&
                            (act.title.toString().contains("reply", true) || act.title.toString()
                                .contains("답장", true))
                }
                println("wEXT, action : " + wExt + " / " + action)
                if (action != null){
                    execContext = applicationContext
                    callResponder(action, myreply)
                }
                Log.d(
                    "ActiveNotification", "Package: ${notification.packageName}, " +
                            "Title: ${notification.notification.extras.getString("android.title")}, " +
                            "Text: ${notification.notification.extras.getString("android.text")}"
                )
            }
        }
    }

    fun callResponder(session: Notification.Action?, myreply: String){
        val parseContext = RhinoAndroidHelper.prepareContext()
        val replier = MyNotiListen2.SessionCacheReplier(session)
        parseContext.optimizationLevel = -1
        replier.reply(myreply)
        Context.exit()
    }

    class SessionCacheReplier (private val session : Notification.Action?){
        fun reply(value: String){
            if (session == null){ return }

            val sendIntent = Intent()
            val msg = Bundle()

            session.remoteInputs?.forEach { inputable -> msg.putCharSequence(inputable.resultKey, value)}

            RemoteInput.addResultsToIntent(session.remoteInputs, sendIntent, msg)

            try {
                session.actionIntent.send(MyNotiListen2.execContext, 0, sendIntent)
            }catch (e:PendingIntent.CanceledException){
                // 예외 처리
            }
        }
    }
}

