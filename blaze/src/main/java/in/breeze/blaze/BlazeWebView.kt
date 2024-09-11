package `in`.breeze.blaze

import android.annotation.SuppressLint
import android.app.Activity
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

  init {
    this.webView.settings.javaScriptEnabled = true
    this.webView.webViewClient = WebViewClient()
    this.webView.addJavascriptInterface(this, "Native")
    this.webView.loadUrl(getBaseUrl(initiatePayload))
    this.sendEvent("initiate", this.initiatePayload)
  }


  fun process(payload: JSONObject) {
    this.sendEvent("process", payload)
    this.webView.layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    val rootView = this.context.window.decorView.findViewById<ViewGroup>(android.R.id.content)
    rootView?.addView(webView)
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
    val eventMessage = eventJson.getString("eventName")
    val eventData = eventJson.optString("eventData")
    val eventDataJson = safeParseJson(eventData)
    if (eventMessage == "appReady") {
      isWebViewReady = true
      drainEventQueue()
    }
    if (eventMessage == "callbackEvent") {
      this.callbackFn.invoke(eventDataJson)
    }

    if (eventMessage == "consumeBackPress") {
      consumingBackPress = true
    }

    if (eventMessage == "releaseBackPress") {
      consumingBackPress = false
    }
    
  }


  private fun sendEvent(event: String, payload: JSONObject) {
    Log.d("BlazeWebView", "Sending event: $event")
    Log.d("BlazeWebView", "Payload: $payload")
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

}