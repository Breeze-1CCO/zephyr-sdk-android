package `in`.breeze.blaze

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.annotation.Keep
import org.json.JSONObject


typealias CallbackFn = (event: JSONObject) -> Unit;

class Blaze {

  private lateinit var webView: BlazeWebView;
  private lateinit var context: Activity;
  private var callbackFn: CallbackFn? = null;
  private var isInitialized: Boolean = false;

  fun initiate(context: Activity, initiatePayload: JSONObject, callbackFn: CallbackFn) {
      this.context = context;
      this.callbackFn = callbackFn;
      this.webView = BlazeWebView(this.context, initiatePayload, callbackFn)
      this.isInitialized = true;
  }

  fun process(payload: JSONObject) {
    this.webView.process(payload)
  }

  fun handleBackPress(): Boolean {
    return this.webView.handleBackPress()
  }

  fun terminate() {
    this.webView.terminate();
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

}