package `in`.breeze.blazeapp

import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.breeze.blaze.Blaze
import `in`.breeze.blazeapp.ui.theme.BlazeAppTheme
import org.json.JSONObject
import java.util.UUID.randomUUID

class MainActivity : ComponentActivity() {

  private lateinit var blaze: Blaze

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WebView.setWebContentsDebuggingEnabled(true)

    val activityContext = this
    setContent {
      BlazeAppTheme {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          InitiateView {
            Toast.makeText(activityContext, "Initiate Triggered", Toast.LENGTH_SHORT).show()
            blaze = Blaze()

            val initiatePayload = createInitiatePayload()
            val initSDKPayload = createSDKPayload(initiatePayload)

            blaze.initiate(activityContext, initSDKPayload) { callbackEvent ->
              run {
                println("callbackEvent: " + callbackEvent.toString(2))
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          ProcessView {
            Toast.makeText(activityContext, "Process Triggered", Toast.LENGTH_SHORT).show()
            blaze.process(JSONObject())
          }
        }
      }
    }
  }
}

fun createInitiatePayload(): JSONObject {
  val initiatePayload = JSONObject()
  initiatePayload.put("merchantId", "d2cstorebeta")
  initiatePayload.put("environment", "sandbox")
  initiatePayload.put("shopUrl", "https://d2c-store-beta.myshopify.com")
  return initiatePayload
}

fun createSDKPayload(payload: JSONObject): JSONObject {
  val sdkPayload = JSONObject()
  sdkPayload.put("requestId", randomUUID().toString())
  sdkPayload.put("service", "in.breeze.onecco")
  sdkPayload.put("payload", payload)
  return sdkPayload
}

@Composable
fun InitiateView(onClick: () -> Unit) {
  Row {
    Button(onClick = onClick) {
      Text(text = "Initiate")
    }
  }
}

@Composable
fun ProcessView(onClick: () -> Unit) {
  Row {
    Button(onClick = onClick) {
      Text(text = "Process")
    }
  }
}

