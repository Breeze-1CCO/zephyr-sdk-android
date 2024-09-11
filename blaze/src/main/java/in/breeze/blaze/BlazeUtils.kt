package `in`.breeze.blaze

import org.json.JSONObject

fun safeParseJson(jsonString: String): JSONObject {
  return try {
    JSONObject(jsonString)
  } catch (e: Exception) {
    JSONObject()
  }
}

fun getBaseUrl(payload: JSONObject): String {
  val environment = payload.optJSONObject("payload")?.optString("environment") ?: "release"
  return if (environment == "beta") {
    "https://app.beta.breeze.in"
  } else {
    "https://app.breeze.in"
  }
}