package `in`.breeze.blazeapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.breeze.blaze.Blaze
import `in`.breeze.blazeapp.ui.theme.BlazeAppTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID.randomUUID


class MainActivity : ComponentActivity() {

  private lateinit var blaze: Blaze

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WebView.setWebContentsDebuggingEnabled(true)

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (blaze.handleBackPress()) {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
        }
      }
    })


    val activityContext = this
    setContent {

      val callbackEventsStr = remember { mutableStateOf("Event \n") }

      val otpSessionToken = remember {
        mutableStateOf("")
      }

      BlazeAppTheme {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {


          Row {
            InitiateView {
              blaze = Blaze()

              val initiatePayload = createInitiatePayload()
              val initSDKPayload = createSDKPayload(initiatePayload)

              blaze.initiate(activityContext, initSDKPayload) { callbackEvent ->
                run {
                  callbackEventsStr.value =
                    callbackEventsStr.value + callbackEvent.toString(2) + "\n ------- \n"

                  Log.d("Blaze:", callbackEvent.toString())

                  val callbackEventAction =
                    callbackEvent.optJSONObject("payload")?.optString("action")

                  Toast.makeText(activityContext, callbackEvent.toString(2), Toast.LENGTH_SHORT)
                    .show()

                  if (callbackEventAction == "sendOTP") {
                    otpSessionToken.value =
                      callbackEvent.optJSONObject("payload")?.optString("otpSessionToken") ?: ""
                  }

                }
              }
            }

            Spacer(modifier = Modifier.width(16.dp))

            TerminateView {
              blaze.terminate()
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          AppLayout({ payload ->
            blaze.process(payload)
          }, otpSessionToken, callbackEventsStr)

        }
      }
    }
  }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppLayout(
  processor: (payload: JSONObject) -> Unit,
  otpSessionToken: MutableState<String>,
  eventString: MutableState<String>
) {
  var selectedTabIndex by remember { mutableStateOf(0) }
  val tabs = listOf("Checkout", "Login", "Update Customer", "Events")

  Scaffold(topBar = {
    TabRow(selectedTabIndex = selectedTabIndex, indicator = { tabPositions ->
      Box(
        Modifier
          .tabIndicatorOffset(tabPositions[selectedTabIndex])
          .height(4.dp)
      )
    }) {
      tabs.forEachIndexed { index, title ->
        Tab(selected = selectedTabIndex == index,
          onClick = { selectedTabIndex = index },
          text = { Text(title) })
      }
    }
  }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding)) {
      when (selectedTabIndex) {
        0 -> StartCheckoutView(processor)
        1 -> LoginProcessParams(processor, otpSessionToken)
        2 -> UpdateCustomerView(processor)
        3 -> Logs(eventString)
      }
    }

  }
}


fun createSDKPayload(payload: JSONObject): JSONObject {
  val sdkPayload = JSONObject()
  sdkPayload.put("requestId", randomUUID().toString())
  sdkPayload.put("service", "in.breeze.onecco")
  sdkPayload.put("payload", payload)
  return sdkPayload
}

fun createCustomerUpdatePayload(
  customerName: String,
  email: String,
  fullAddress: String,
  name: String,
  phoneNumber: String,
  postalCode: String,
  state: String,
  city: String,
  country: String,
  countryPhoneCode: String
): JSONObject {
  val payload = JSONObject()
  payload.put("action", "updateCustomer")
  payload.put("name", customerName)
  payload.put("emailAddress", email)

  val address = JSONObject()
  address.put("fullAddress", fullAddress)
  address.put("name", name)
  address.put("phoneNumber", phoneNumber)
  address.put("postalCode", postalCode)
  address.put("state", state)
  address.put("city", city)
  address.put("country", country)
  address.put("countryPhoneCode", countryPhoneCode)


  if (fullAddress.isNotEmpty() && name.isNotEmpty()) {
    val addresses = JSONArray()
    addresses.put(address)
    payload.put("addresses", addresses)
  }

  return payload
}

fun createInitiatePayload(): JSONObject {
  val initiatePayload = JSONObject()
  initiatePayload.put("merchantId", "d2cstorebeta")
  initiatePayload.put("environment", "beta")
  initiatePayload.put("shopUrl", "https://d2cstore-custom.vercel.app")
  return initiatePayload
}

fun createStartCheckoutPayload(): JSONObject {
  val cartString = """
    {
      "token": "Z2NwLWFzaWEtc291dGhlYXN0MTowMUo5Qjc2SDJOSlZGTVE2RU1QQTE3SzFCRQ?key=87d1860b490836e51d550b978d263f3b",
      "note": "",
      "attributes": {      },
      "original_total_price": 200,
      "total_price": 200,
      "total_discount": 0,
      "total_weight": 750,
      "item_count": 1,
      "items": [
        {
          "id": 49031395803451,
          "properties": {
    
          },
          "quantity": 1,
          "variant_id": 49031395803451,
          "key": "49031395803451:7e551f8ea968a2e49b09c57379e7561b",
          "title": "Sony WH-1000XM5 Dummy (Ankit)",
          "price": 200,
          "original_price": 200,
          "presentment_price": 2,
          "discounted_price": 200,
          "line_price": 200,
          "original_line_price": 200,
          "total_discount": 0,
          "discounts": [],
          "sku": null,
          "grams": 750,
          "vendor": "Sony",
          "taxable": false,
          "product_id": 9614223049019,
          "product_has_only_default_variant": true,
          "gift_card": false,
          "final_price": 200,
          "final_line_price": 200,
          "url": "/products/sony-wh-1000xm5-dummy-ankit?variant=49031395803451",
          "featured_image": {
            "aspect_ratio": 1,
            "alt": "Sony WH-1000XM5 Dummy (Ankit)",
            "height": 679,
            "url": "https://cdn.shopify.com/s/files/1/0684/1624/1979/files/51KGPDttQhL._SX679.jpg?v=1690640424",
            "width": 679
          },
          "image": "https://cdn.shopify.com/s/files/1/0684/1624/1979/files/51KGPDttQhL._SX679.jpg?v=1690640424",
          "handle": "sony-wh-1000xm5-dummy-ankit",
          "requires_shipping": true,
          "product_type": "disableTrackOrder",
          "product_title": "Sony WH-1000XM5 Dummy (Ankit)",
          "product_description": "\n\nIndustry Leading noise cancellation-two processors control 8 microphones for unprecedented noise cancellation. With Auto NC Optimizer, noise cancelling is automatically optimized based on your wearing conditions and environment.\nIndustry-leading call quality with our Precise Voice Pickup Technology uses four beamforming microphones and an AI-based noise reduction algorithm\nMagnificent Sound, engineered to perfection with the new Integrated Processor V1\nCrystal clear hands-free calling with 4 beamforming microphones, precise voice pickup, and advanced audio signal processing.\nUp to 40-hour battery life for continuous music playtime (With Noise Cancellation ON, get up to 30 Hours of playtime, and With Noise Cancellation off get up to 40 Hours.) All-day power and quick charging (3 min charge for 3 hours of playback).\nUltra-comfortable, lightweight design with soft fit leather\nMultipoint connection allows you to quickly switch between devices\n\n\n\n\n\nCarry your headphones effortlessly in the redesigned case.\nIntuitive touch control settings to pause play skip tracks, control volume, activate your voice assistant, and answer phone calls.\nFor everyday convenience, just Speak-to-Chat and Quick Attention mode stop your music and let in ambient sound\n\n\n",
          "variant_title": null,
          "variant_options": [
            "Default Title"
          ],
          "options_with_values": [
            {
              "name": "Title",
              "value": "Default Title"
            }
          ],
          "line_level_discount_allocations": [],
          "line_level_total_discount": 0,
          "has_components": false
        }
      ],
      "requires_shipping": true,
      "currency": "INR",
      "items_subtotal_price": 200,
      "cart_level_discount_applications": []
    }
    """.trimIndent()

  val processPayload =
    JSONObject().putOpt("cart", JSONObject(cartString)).put("action", "startCheckout")
  return processPayload
}

fun createCustomStartCheckoutPayload(): JSONObject {
  val processPayload = JSONObject()
  processPayload.put("action", "startCheckout")
  processPayload.put("cart", "{\"id\":\"7feaa429-308d-49b7-8461-46bfa4b37ff7\",\"items\":[{\"id\":\"3aecf419-6823-4581-a093-0c90c5b5e1fc\",\"title\":\"Apple iPhone 7 Plus\",\"variantTitle\":\"256 GB / GOLD\",\"image\":\"https://dfelk5npz6ka0.cloudfront.net/products/40767940985052.jpg\",\"quantity\":1,\"initialPrice\":100,\"finalPrice\":100,\"discount\":0}],\"initialPrice\":100,\"totalPrice\":100,\"totalDiscount\":0,\"itemCount\":1,\"currency\":\"INR\"}")
  processPayload.put("signature", "T4BRKwHKWfkWRLgF5ecss2+2tyID4zKVUqkfEgdFS73lyAokBq92VJRz4g+xzCcUE84ZNl7oQ9t26i8zGQLgh/B/6vNliM9u7VMX0soYDC9pEWd4TsWeetlYMzl/UIzitYan5q9aQ2UfS7HENHQGvGfOjsa75gP3SVwpufK8Sb1VFRFsnJKsXzgWq+y9iWLieJe596poEzUP2Wkt17mSGwH9rWukTy5S1ddZoZXUxDQiMtfXFj9NOnPDUA8psmERifivaD8IvhMEYwlEk+u6MrxF7IhasBs8dYGEZW+YDeaRz6mzK37sarYWgCUJgEYTJ97rmcRedgvnj+28YES/pw==")
  processPayload.put("keyId", "90701")
  return processPayload
}

fun createCustomStartCheckoutPayloadWithHiddenFields(): JSONObject {
  val processPayload = JSONObject()
  processPayload.put("action", "startCheckout")
  processPayload.put("cart", "{\"id\":\"7feaa429-308d-49b7-8461-46bfa4b37ff7\",\"items\":[{\"id\":\"3aecf419-6823-4581-a093-0c90c5b5e1fc\",\"title\":\"Apple iPhone 7 Plus\",\"variantTitle\":\"256 GB / GOLD\",\"image\":\"https://dfelk5npz6ka0.cloudfront.net/products/40767940985052.jpg\",\"quantity\":1,\"initialPrice\":100,\"finalPrice\":100,\"discount\":0}],\"initialPrice\":100,\"totalPrice\":100,\"totalDiscount\":0,\"itemCount\":1,\"currency\":\"INR\"}")
  processPayload.put("signature", "T4BRKwHKWfkWRLgF5ecss2+2tyID4zKVUqkfEgdFS73lyAokBq92VJRz4g+xzCcUE84ZNl7oQ9t26i8zGQLgh/B/6vNliM9u7VMX0soYDC9pEWd4TsWeetlYMzl/UIzitYan5q9aQ2UfS7HENHQGvGfOjsa75gP3SVwpufK8Sb1VFRFsnJKsXzgWq+y9iWLieJe596poEzUP2Wkt17mSGwH9rWukTy5S1ddZoZXUxDQiMtfXFj9NOnPDUA8psmERifivaD8IvhMEYwlEk+u6MrxF7IhasBs8dYGEZW+YDeaRz6mzK37sarYWgCUJgEYTJ97rmcRedgvnj+28YES/pw==")
  processPayload.put("keyId", "90701")
 processPayload.put("hideUserProfile", true)
 processPayload.put("hideOffersSection", true)
 processPayload.put("hideAddress", true)
  return processPayload
}

fun createSendOtpPayload(phoneNumber: String): JSONObject {
  val payload = JSONObject()
  payload.put("action", "sendOTP")
  payload.put("phoneNumber", phoneNumber)
  payload.put("countryCode", "+91")
  return payload;
}

fun createVerifyOtpPayload(otp: String, otpSessionToken: String): JSONObject {
  val payload = JSONObject()
  payload.put("action", "verifyOTP")
  payload.put("otp", otp)
  payload.put("otpSessionToken", otpSessionToken)
  return payload
}

fun createLogoutPayload(): JSONObject {
  val payload = JSONObject()
  payload.put("action", "logout")
  return payload
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

@Composable
fun TerminateView(onClick: () -> Unit) {
  Row {
    Button(onClick = onClick) {
      Text(text = "Terminate")
    }
  }
}


@Composable
fun StartCheckoutView(processor: (payload: JSONObject) -> Unit) {
  ProcessView {
    processor(createSDKPayload(createStartCheckoutPayload()))
  }
  ProcessView {
    processor(createSDKPayload(createCustomStartCheckoutPayload()))
  }
  ProcessView {
    processor(createSDKPayload(createCustomStartCheckoutPayloadWithHiddenFields()))
  }
}

@Composable
fun LoginProcessParams(
  processor: (payload: JSONObject) -> Unit, otpSessionToken: MutableState<String>
) {
  var phoneNumber by remember {
    mutableStateOf("")
  }

  var otp by remember {
    mutableStateOf("")
  }

  Column(
    modifier = Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    OutlinedTextField(value = phoneNumber,
      onValueChange = { phoneNumber = it },
      label = { Text("Phone Number") })
    OutlinedTextField(value = otp, onValueChange = { otp = it }, label = { Text("OTP") })
    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Button(onClick = {
        processor(createSDKPayload(createSendOtpPayload(phoneNumber)))
      }, enabled = phoneNumber.isNotEmpty()) {
        Text(text = "SendOTP")
      }
      Button(onClick = {
        processor(createSDKPayload(createVerifyOtpPayload(otp, otpSessionToken.value)))
      }, enabled = otp.isNotEmpty()) {
        Text(text = "VerifyOTP")
      }

      Button(onClick = {
        processor(createSDKPayload(createLogoutPayload()))
      }) {
        Text(text = "Logout")
      }
    }
  }
}


@Composable
fun UpdateCustomerView(processor: (payload: JSONObject) -> Unit) {
  var customerName by remember {
    mutableStateOf("Test Name")
  }

  var email by remember {
    mutableStateOf("test@test.com")
  }


  var name by remember { mutableStateOf("Test") }
  var phoneNumber by remember { mutableStateOf("9876543210") }
  var postalCode by remember { mutableStateOf("560030") }
  var state by remember { mutableStateOf("Karnataka") }
  var city by remember { mutableStateOf("Bengaluru") }
  var fullAddress by remember { mutableStateOf("Test FirstLine") }
  var country by remember { mutableStateOf("India") }
  var countryPhoneCode by remember { mutableStateOf("+91") }



  Column(
    modifier = Modifier
      .padding(vertical = 16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {

    Text("Customer Profile")
    OutlinedTextField(value = customerName,
      onValueChange = { customerName = it },
      label = { Text("Name") })
    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })

    Spacer(modifier = Modifier.height(16.dp))
    Text("Customer Address (optional)")

    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
    OutlinedTextField(value = phoneNumber,
      onValueChange = { phoneNumber = it },
      label = { Text("Phone Number") })
    OutlinedTextField(value = postalCode,
      onValueChange = { postalCode = it },
      label = { Text("Postal Code") })
    OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") })
    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") })
    OutlinedTextField(value = fullAddress,
      onValueChange = { fullAddress = it },
      label = { Text("Full Address") })
    OutlinedTextField(value = country,
      onValueChange = { country = it },
      label = { Text("Country") })
    OutlinedTextField(value = countryPhoneCode,
      onValueChange = { countryPhoneCode = it },
      label = { Text("Country Phone Code") })

    Row(
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Button(onClick = {
        processor(
          createSDKPayload(
            createCustomerUpdatePayload(
              customerName,
              email,
              fullAddress,
              name,
              phoneNumber,
              postalCode,
              state,
              city,
              country,
              countryPhoneCode
            )
          )
        )

      }, enabled = customerName.isNotEmpty() && email.isNotEmpty()) {
        Text(text = "UpdateCustomer")
      }
    }
  }
}

@Composable
fun Logs(eventString: MutableState<String>) {

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = eventString.value, modifier = Modifier.padding(16.dp)
    )
  }
}
