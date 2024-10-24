package `in`.breeze.blaze

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import org.json.JSONObject

internal class BlazeWebView @SuppressLint(
  "SetJavaScriptEnabled",
  "JavascriptInterface"
) constructor(
  private val context: Activity,
  private val initiatePayload: JSONObject,
  private val callbackFn: CallbackFn
) {

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
    this.webView.destroy()
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
      context.runOnUiThread {
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
    this.context.runOnUiThread {
      this.webView.layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      val rootView = this.context.window.decorView.findViewById<ViewGroup>(android.R.id.content)
      rootView?.addView(webView)
    }
  }

  private fun hideView() {
    this.context.runOnUiThread {
      this.webView.layoutParams = FrameLayout.LayoutParams(0, 0)
      val rootView = this.context.window.decorView.findViewById<ViewGroup>(android.R.id.content)
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
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e("BlazeSDK: openApp: ", e.message.toString())
    }
  }

}