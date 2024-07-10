# Zephyr SDK Android

The Zephyr SDK is an easy-to-use toolkit that allows you to effortlessly integrate and utilize [Breeze 1 Click Checkout](https://breeze.in) & its services into your android app.

## Android SDK Integration

Follow the below steps to integrate Zephyr SDK into your android application:

### Step 1: Obtaining the Zephyr SDK

1.1. Include the repository for SDK Resolution to your project's `settings.gradle.kts` or `settings.gradle` file.

```kotlin
dependencyResolutionManagement {
 repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
 repositories {
   google()
   mavenCentral()
   // jitpack repository for Zephyr SDK resolution
   maven {
    url = uri("https://jitpack.io")
  }
 }
}
```

1.2. Add Breeze SDK as an dependency to your app modules's build.gradle file

```kotlin
dependencies {
 // ... rest of your app dependencies
 // Add Zephyr SDK as a dependency
 implementation 'com.github.juspay:zephyr-sdk-android:0.0.1-alpha
}
```

### Step 2: Initialize the SDK

**2.1. Create an instance of the `Zephyr` class in your application class.**

```kotlin
val zephyr = Zephyr()
```

**2.2. Initiate the Zephyr instance. Preferably in the `onCreate` method of your application class.**

In order to call initiate you need to perform the following steps:

#### 2.2.1: Construct the Initiate Payload

Create a Json with correct parameters to initiate the SDK. This is the data that will be used to initialize the SDK.

```kotlin
// Create a JSONObject for the Initiate data
val initiatePayload = JSONObject()
initiatePayload.put("merchantId", "<MERCHANT_ID>")
initiatePayload.put("environment", "<ENVIRONMENT>")
initiatePayload.put("shopUrl", "<SHOP_URL>")


// Place Initiate Payload into SDK Payload
val initSDKPayload = JSONObject()
initSDKPayload.put("requestId", "<UNIQUE_RANDOM_ID>")
initSDKPayload.put("service", "in.breeze.onecco")
initSDKPayload.put("payload", initiatePayload)

```

Note: Obtain values for `merchantId`, `environment` and `shopUrl` from the Breeze team.

Refer to schemas for understanding what keys mean.

#### 2.2.2: Construct the Callback Method

During the user journey the SDK will call the callback method with the result of the SDK operation.
You need to implement this method in order to handle the result of the SDK operation.

```kotlin

fun zephyrCallbackHandler (event: JSONObject) {
  val eventName = event.getString("eventName")
  val eventData = event.getJSONObject("eventData")

  when(eventName) {
    // Handle various events according your desired logic
  }
}

```

***2.2.3: Call the initiate method on Zephyr Instance **

Finally, call the initiate method on the Zephyr instance with the payload and the callback method.
The first parameter is the context of the application.

```kotlin
zephyr.initiate(this, initSDKPayload, ::zephyrCallbackHandler)
```

***Combined Example:***

```kotlin
class MyApplication : Application() {

    // Create Zephyr Instance
    val zephyr = Zephyr()

    override fun onCreate() {
      super.onCreate()

      // 2.2 Initiate

      // 2.2.1 Create a JSONObject for the Initiate data
      val initiatePayload = JSONObject()
      initiatePayload.put("merchantId", "<MERCHANT_ID>")
      initiatePayload.put("environment", "<ENVIRONMENT>")
      initiatePayload.put("shopUrl", "<SHOP_URL>")

      // Place Initiate Payload into SDK Payload
      val initSDKPayload = JSONObject()
      initSDKPayload.put("requestId", "<UNIQUE_RANDOM_ID>")
      initSDKPayload.put("service", "in.breeze.onecco")
      initSDKPayload.put("payload", initiatePayload)

      // 2.2.3 Initiate Zephyr SDK
      zephyr.initiate(this, initSDKPayload, ::zephyrCallbackHandler)
    }
}

// 2.2.2: Creating a Callback handler
fun zephyrCallbackHandler (event: JSONObject) {
  val eventName = event.getString("eventName")
  val eventData = event.getJSONObject("eventData")

  when(eventName) {
    // Handle various events according your desired logic
  }
}

```

### Step 3: Start processing your requests

Once the SDK is initiated, you can start processing your requests using the initialized instance of the SDK.
The SDK will call the callback method with the result of the SDK operation.

#### 3.1: Construct the Process Payload

Create a Json payload with the required parameters to process the request.
The process payload differs based on the request.
Refer to schemas sections to understand what kind of data is required for different requests

```kotlin

// 3.1 Create SDK Process Payload
// Create a JSONObject for the Process data
val processPayload = JSONObject()
processPayload.put("action", "<ACTION>")
// and more parameters required as per the action

// Place Process Payload into SDK Payload
val processSDKPayload = JSONObject()
processSDKPayload.put("requestId", "<UNIQUE_RANDOM_ID>")
processSDKPayload.put("service", "in.breeze.onecco")
processSDKPayload.put("payload", processPayload)

```

#### 3.2: Call the process method on Zephyr Instance

Call the process method on the Zephyr instance with the process payload to start the user journey or a headless flow.

```kotlin
zephyr.process(processSDKPayload)
```

### Step 4: Handling Back Press

For making the hardware back button work as expected, you need to call the `handleBackPress` method on the Zephyr instance.
The method should be called in the `onBackPressed` method of the activity.
This method returns a boolean value which indicates if you need to handle back press or not.

```kotlin
override fun onBackPressed() {
  if (!zephyr.handleBackPress()) {
    super.onBackPressed()
  }
}
```

### Optional Steps

This section contains optional steps required to be implemented only if you are overriding the life cycle events of your activity.

***onActivityResult***

In order to handle results of App Switches in cases like UPI Intent transactions, you need to call super.onActivityResult() if available, if not you can make same function call on Zephyr Instance.

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  // If super.onActivityResult is available use following:
  super.onActivityResult(requestCode, resultCode, data)

  // In case super.onActivityResult is NOT available please use following:
  zephyr.onActivityResult(requestCode, resultCode, data)
}
```

***onRequestPermissionsResult***

In order to handle results of Permission Request results for cases like OTP Reading, you need to call super.onRequestPermissionsResult() in your onRequestPermissionsResult lifeCycle hook, if available, if not you can make same function call on Zephyr Instance.

```kotlin
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
  //  If super.onRequestPermissionsResult is available use following:
  super.onRequestPermissionsResult(requestCode, permissions, grantResults)

  // In case super.onActivityResult is NOT available please use following:
  zephyr.onRequestPermissionsResult(requestCode, permissions, grantResults)
}
```
