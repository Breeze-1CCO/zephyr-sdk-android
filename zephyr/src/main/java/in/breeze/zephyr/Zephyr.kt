package `in`.breeze.zephyr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.Keep
import org.json.JSONException
import org.json.JSONObject


typealias CallbackFn = (event: JSONObject) -> Unit;

class Zephyr {

  private lateinit var initiatePayload: JSONObject;
  private lateinit var webView: WebView;
  private lateinit var context: Activity;
  private var callbackFn: CallbackFn? = null;
  private var isInitialized: Boolean = false;


  @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
  fun initiate(context: Activity, initiatePayload: JSONObject, callbackFn: CallbackFn) {
    try {
      this.context = context;
      this.initiatePayload = initiatePayload;
      this.callbackFn = callbackFn;
      this.webView = WebView(context);
      this.webView.settings.javaScriptEnabled = true;
      this.webView.addJavascriptInterface(this, "Zephyr");
      this.webView.loadUrl(this.getAppUrl());
      this.isInitialized = true;
    } catch (e: JSONException) {
      callbackFn(
        this.createCallbackEvent(
          "initiateResult",
          JSONObject()
            .put("error", e.message)
            .put("message", "Bad Initiate Payload")
        )
      )
    }
  }

  fun process() {
    this.webView.layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    val rootView = this.context.window.decorView.findViewById<ViewGroup>(android.R.id.content)
    rootView?.addView(webView)
  }

  fun handleBackPress(): Boolean {
    if (this.webView.canGoBack()) {
      this.webView.goBack();
      return false;
    }
    return true;
  }

  fun terminate() {
    this.webView.destroy();
  }

  @Keep
  fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
  }

  @Keep
  fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
  }


  // Utils
  // Todo: Move to a better place.
  private fun getAppUrl(): String {
    val urlString = if (this.getEnvironment() == "sandbox") {
      "https://app.beta.breeze.in"
    } else {
      "https://app.breeze.in"
    }
    val merchantId = this.getInitiatePayload().get("merchantId")
    val shopUrl = this.getInitiatePayload().get("shopUrl")
    return "$urlString?merchantId=$merchantId&shopUrl=$shopUrl"
  }

  private fun getEnvironment(): String {
    return this.getInitiatePayload().getString("environment")
  }

  private fun getInitiatePayload(): JSONObject {
    return this.initiatePayload.getJSONObject("payload")
  }

  private fun createCallbackEvent(eventName: String, eventData: JSONObject): JSONObject {
    val event = JSONObject()
    event.put("eventName", eventName)
    event.put("eventData", eventData)
    return event
  }

}