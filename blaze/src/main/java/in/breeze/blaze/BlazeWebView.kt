package `in`.breeze.blaze

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.Collections

internal class BlazeWebView @SuppressLint(
  "SetJavaScriptEnabled",
  "JavascriptInterface"
) constructor(
  context: Activity,
  private val initiatePayload: JSONObject,
  private val callbackFn: CallbackFn
) {

  private val contextRef: WeakReference<Activity> = WeakReference(context)
  private val webView: WebView = WebView(context)
  private var isWebViewReady: Boolean = false
  private var consumingBackPress: Boolean = false
  private var eventQueue: HashMap<String, JSONObject> = hashMapOf()
  private val sharedPreferences: SharedPreferences

  init {
    this.webView.settings.javaScriptEnabled = true
    this.webView.webViewClient = WebViewClient()
    this.webView.addJavascriptInterface(this, "Native")
    this.webView.loadUrl(getBaseUrl(initiatePayload))
    this.sendEvent("initiate", this.initiatePayload)
    this.sharedPreferences = context.getSharedPreferences("BlazeSharedPref", Context.MODE_PRIVATE)
  }


  fun process(payload: JSONObject) {
    this.sendEvent("process", payload)
  }

  fun handleBackPress(): Boolean {
    if (consumingBackPress) {
      this.sendEvent("backPress", JSONObject())
      return false
    }
    return true
  }

  fun terminate() {
    this.sendEvent("terminate", JSONObject())
    hideView()
    contextRef.get()?.runOnUiThread {
      webView.stopLoading()
      webView.removeJavascriptInterface("Native")
      this.webView.destroy()
    }
    eventQueue.clear()
  }


  @JavascriptInterface
  fun onEvent(event: String) {
    val eventJson = safeParseJson(event)
    val eventName = eventJson.getString("eventName")
    val eventData = eventJson.optString("eventData")
    val eventDataJson = safeParseJson(eventData)

    if (eventName == "appReady") {
      isWebViewReady = true
      drainEventQueue()
    }
    if (eventName == "callbackEvent") {
      this.callbackFn.invoke(eventDataJson)
    }

    if (eventName == "consumeBackPress") {
      consumingBackPress = true
    }

    if (eventName == "releaseBackPress") {
      consumingBackPress = false
    }

    if (eventName == "renderView") {
      renderView()
    }

    if (eventName == "hideView") {
      hideView()
    }

  }


  private fun sendEvent(event: String, payload: JSONObject) {
    val eventMessage =
      JSONObject()
        .put("eventName", event)
        .put("eventData", payload)
        .put("source", "blaze")
    if (isWebViewReady) {
      contextRef.get()?.runOnUiThread {
        webView.evaluateJavascript("javascript:onSDKEvent(JSON.stringify($eventMessage))") {}
      }
    } else {
      eventQueue[event] = payload
    }
  }

  private fun drainEventQueue() {
    val pendingEvents = eventQueue.toList()
    eventQueue.clear()
    pendingEvents.forEach { (event, payload) ->
      sendEvent(event, payload)
    }
  }

  private fun renderView() {
    contextRef.get()?.runOnUiThread {
      this.webView.layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      val rootView = contextRef.get()?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
      rootView?.addView(webView)
    }
  }

  private fun hideView() {
    contextRef.get()?.runOnUiThread {
      this.webView.layoutParams = FrameLayout.LayoutParams(0, 0)
      val rootView = contextRef.get()?.window?.decorView?.findViewById<ViewGroup>(android.R.id.content)
      rootView?.removeView(webView)
    }
  }

  @JavascriptInterface
  fun saveToStorage(key: String, value: String): Boolean {
    try {
      val editor = sharedPreferences.edit()
      editor.putString(key, value)
      editor.apply()
      return true
    } catch (e: Exception) {
      Log.e("BlazeSDK: Failure: ", e.message.toString())
      return false
    }
  }

  @JavascriptInterface
  fun getFromStorage(key: String): String? {
    try {
      return sharedPreferences.getString(key, null)
    } catch (e: Exception) {
      Log.e("BlazeSDK: Failure: ", e.message.toString())
      return null
    }
  }

  @JavascriptInterface
  fun openApp(
    intentUri: String
  ) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intentUri))
      contextRef.get()?.startActivity(intent)
    } catch (e: Exception) {
      Log.e("BlazeSDK: openApp: ", e.message.toString())
    }
  }

  @JavascriptInterface
  fun findApps(payload: String): String {
    val pm = contextRef.get()?.packageManager
    val apps = JSONArray()

    if (pm !== null ) {
      val upiApps = Intent()
      upiApps.setData(Uri.parse(payload))
      val launchables: List<ResolveInfo> = pm.queryIntentActivities(upiApps, 0)
      Collections.sort(launchables, ResolveInfo.DisplayNameComparator(pm))
      for (resolveInfo in launchables) {
        val jsonObject = JSONObject()
        try {
          val appInfo = pm.getApplicationInfo(resolveInfo.activityInfo.packageName, 0)
          jsonObject.put("packageName", appInfo.packageName)
          jsonObject.put("appName", pm.getApplicationLabel(appInfo))
          apps.put(jsonObject)
        } catch (e: Exception) {
          Log.e("BlazeSDK: openApp: ", e.message.toString())
        }
      }
    }
    return apps.toString()
  }

}