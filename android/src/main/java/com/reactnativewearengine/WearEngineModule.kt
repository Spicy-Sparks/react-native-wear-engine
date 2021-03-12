package com.reactnativewearengine

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.device.DeviceClient
import com.huawei.wearengine.p2p.*
import java.io.File
import java.util.*


class WearEngineModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    // Store the paired device list
    private var deviceList: MutableList<Device> = ArrayList<Device>()
    private var p2pClient : P2pClient = P2pClient.getInstance()
    private var connectedDevice: Device? = null

    override fun getName(): String {
        return "WearEngine"
    }

    private val receiver = Receiver { message ->
      if (message != null) {
        val payload = Arguments.createMap()
        // Put data to map
        payload.putString("data", String(message.data))
        sendEvent("onMessageReceived", payload)
        //printOperationResult("ReceiveMessage is:$data")
      }
    }

    @ReactMethod
    fun unregisterReceiver(){
      HiWear.getP2pClient(currentActivity).unregisterReceiver(receiver)
        .addOnSuccessListener {
          // Succeed in cancelling the registration of a listener for the callback method of messages sent by the phone
          sendEvent("onUnregisterReceiver", true)
        }
        .addOnFailureListener {
          // Fail to cancel the registration of a listener for the callback function of messages sent by the phone
          sendEvent("onUnregisterReceiver", false)
        }
    }

    @ReactMethod
    fun hasAvailableDevices() {

      // Step 1: Obtain the DeviceClient object.
      //This indicates the Context object of the app.
      val deviceClient : DeviceClient = HiWear.getDeviceClient(currentActivity)

      // Step 2: Call the hasAvailableDevices method to query whether users have wearable devices.
      deviceClient.hasAvailableDevices().addOnSuccessListener(object : OnSuccessListener<Boolean?> {
          override fun onSuccess(result: Boolean?) {
              // Query whether there is return code indicating that the available device is founded. The return value represents whether there is available devices.
              sendEvent("onHasAvailableDevices", result)
          }
      }).addOnFailureListener(object : OnFailureListener {
          override fun onFailure(e: Exception?) {
              // Query whether there are available devices fails to be executed.
              sendEvent("onHasAvailableDevices", false)
          }
      })
    }

    @ReactMethod
    fun requestPermission() {
      // Step 1: Obtain the AuthClient object.
      //This indicates the Context object of the app.
      val authClient = HiWear.getAuthClient(currentActivity)

      // Step 2: Define the callback object for user authorization.
      val authCallback: AuthCallback = object : AuthCallback {
        override fun onOk(permissions: Array<Permission?>?) {
        // Return the list of permissions granted by the user
          val payload = Arguments.createMap()
          payload.putBoolean("status", true)
          payload.putBoolean("granted", true)
          sendEvent("onRequestPermission", payload)
        }

        override fun onCancel() {
        // Users cancel the authorization
          val payload = Arguments.createMap()
          payload.putBoolean("status", true)
          payload.putBoolean("granted", false)
          sendEvent("onRequestPermission", payload)
        }
      }

      // Step 3: Apply for the required permission (for example, DEVICE_MANAGER, device management permission).
      authClient.requestPermission(authCallback, Permission.DEVICE_MANAGER)
        .addOnSuccessListener {
              // The request authorization task is successfully executed
          //promise.resolve(true)
        }
        .addOnFailureListener {
        //The request authorization task is successfully executed
          //promise.reject(it)
          val payload = Arguments.createMap()
          payload.putBoolean("status", false)
          payload.putBoolean("granted", false)
          sendEvent("onRequestPermission", payload)
        }
      // Note: After this API is called, the user will be prompted with the authorization screen and asked to grant the corresponding permissions.
    }

    @ReactMethod
    fun checkPermission() {
      // Step 1: Obtain the AuthClient object.
      val authClient = HiWear.getAuthClient(currentActivity)

      // Step 2: Call the checkPermission method to check whether the permission is granted.
      authClient.checkPermission(Permission.DEVICE_MANAGER).addOnSuccessListener {
        //Return whether the permission is granted. The value true indicates that the permission is granted, and the value false indicates that the permission is not granted.
        val payload = Arguments.createMap()
        payload.putBoolean("status", true)
        payload.putBoolean("granted", it)
        sendEvent("onCheckPermission", it)
      }.addOnFailureListener {
        // Failed to call the API.
        val payload = Arguments.createMap()
        payload.putBoolean("status", false)
        payload.putBoolean("granted", false)
        sendEvent("onCheckPermission", payload)
      }

      // Call the checkPermissions method to check whether a group of permissions are granted.
      /*val permissions = arrayOf(Permission.DEVICE_MANAGER)
      authClient.checkPermissions(permissions).addOnSuccessListener {
        //Return whether the group of permissions are granted. The value true indicates that the permission is granted, and the value false indicates that the permission is not granted.
        //promise.resolve(it)
        val payload = Arguments.createMap()
        payload.putBoolean("status", true)
        payload.putBoolean("granted", it[0])
      }.addOnFailureListener {
        // Failed to call the API.
        val payload = Arguments.createMap()
        payload.putBoolean("status", false)
        payload.putBoolean("granted", false)
      }*/
    }

  @ReactMethod
  fun getDevices() {
    // Step 1: Obtain the DeviceClient object.
    val deviceClient = HiWear.getDeviceClient(currentActivity)

    // Step 2: Obtain the list of paired devices
    deviceClient.bondedDevices
      .addOnSuccessListener { devices -> // Obtained device list
        deviceList = devices;

        val payload = Arguments.createArray()

        for (device in deviceList)
        {
          val deviceMap = Arguments.createMap()
          deviceMap.putString("name", device.name)
          deviceMap.putString("model", device.model)
          deviceMap.putString("uuid", device.uuid)
          deviceMap.putBoolean("isConnected", device.isConnected)
          deviceMap.putInt("productType", device.productType)

          payload.pushMap(deviceMap)
        }

        sendEvent("onDevicesResult", payload)
      }
      .addOnFailureListener {
        // Process logic when the device list fails to be obtained
        sendEvent("onDevicesResult", false)
      }
  }

    @ReactMethod
    fun setAndPingConnectedDevice(peerPkgName: String, peerFingerPrint: String) {
      // Step 1: Obtain the DeviceClient object.
      val deviceClient = HiWear.getDeviceClient(currentActivity)

      // Step 2: Obtain the list of paired devices
      deviceClient.bondedDevices
        .addOnSuccessListener { devices -> // Obtained device list
          deviceList = devices;

          if (deviceList != null && deviceList.isNotEmpty()) {
            for (device in deviceList) {
              if (device.isConnected) {
                connectedDevice = device
              }
            }
          }

          // Step 2: Obtain the point-to-point communication object
          p2pClient = HiWear.getP2pClient(currentActivity)

          // Step 3: Set the package name of the app on the wearable device that needs communications
          p2pClient.setPeerPkgName(peerPkgName)
          p2pClient.setPeerFingerPrint(peerFingerPrint)


          // Step 4: Check that the third-party apps on the wearable device are running.
          if (connectedDevice != null) {
            p2pClient.ping(connectedDevice) {
                      // Result of the ping communication with the device.
                  val payload = Arguments.createMap()
                  payload.putBoolean("status", true);
                  payload.putInt("code", it);
                  sendEvent("onSetAndPingConnectedDevice", payload)

                  // Register listener
                  p2pClient.registerReceiver(connectedDevice, receiver)
                    .addOnFailureListener {
                      //Log.d("WearEngine", it.toString());
                      //promise.reject(it)
                      // Succeed in registering a listener for the callback method of messages sent by the phone
                    }
                    .addOnSuccessListener {
                      //Log.d("WearEngine", it.toString());
                      // Succeed in registering a listener for the callback method of messages sent by the phone
                    }
            }.addOnSuccessListener {
                        // Related processing logic for third-party apps after the ping command runs
                      }.addOnFailureListener {
                        // Related processing logic for third-party apps after the ping command fails to run
                      }
                    }

        }
        .addOnFailureListener {
          // Process logic when the device list fails to be obtained
          val payload = Arguments.createMap()
          payload.putBoolean("status", false);
          sendEvent("onSetAndPingConnectedDevice", payload)
        }
    }

  @ReactMethod
  fun sendMessage(messageStr: String) {

    if(p2pClient == null)
      sendEvent("onSendResult", false)

    // Step 5: Send short messages from third-party apps on the phone to those on the wearable device
    // Build a Message object
    val builder: Message.Builder = Message.Builder()
    builder.setPayload(messageStr.toByteArray())
    val sendMessage: Message = builder.build()

    val sendCallback: SendCallback = object : SendCallback {
      override fun onSendResult(resultCode: Int) {
        val payload = Arguments.createMap()
        payload.putBoolean("status", true);
        payload.putInt("code", resultCode);
        sendEvent("onSendResult", payload)
      }
        override fun onSendProgress(progress: Long) {
            sendEvent("onSendProgress", progress)
        }
    }
    if (connectedDevice != null && connectedDevice!!.isConnected && sendMessage != null && sendCallback != null) {
      p2pClient.send(connectedDevice, sendMessage, sendCallback)
        .addOnSuccessListener(OnSuccessListener<Void?> {
            //Related processing logic for third-party apps after the send command runs
        })
        .addOnFailureListener(OnFailureListener {
            //Related processing logic for third-party apps after the send command fails to run
            val payload = Arguments.createMap()
            payload.putBoolean("status", false);
            sendEvent("onSendResult", payload)
        })
    }
  }

    @ReactMethod
    fun sendFile(filePath: String) {

        if(p2pClient == null)
            sendEvent("onSendResult", false)

        // Step 5: Send short messages from third-party apps on the phone to those on the wearable device
        // Build a Message object
        val sendFile = File(filePath)
        val builder = Message.Builder()
        builder.setPayload(sendFile)
        val fileMessage = builder.build()

        val sendCallback: SendCallback = object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                val payload = Arguments.createMap()
                payload.putBoolean("status", true);
                payload.putInt("code", resultCode);
                sendEvent("onSendResult", payload)
            }
            override fun onSendProgress(progress: Long) {
                sendEvent("onSendProgress", progress)
            }
        }
        if (connectedDevice != null && connectedDevice!!.isConnected && fileMessage != null && sendCallback != null) {
            p2pClient.send(connectedDevice, fileMessage, sendCallback)
                    .addOnSuccessListener(OnSuccessListener<Void?> {
                        //Related processing logic for third-party apps after the send command runs
                    })
                    .addOnFailureListener(OnFailureListener {
                        //Related processing logic for third-party apps after the send command fails to run
                        val payload = Arguments.createMap()
                        payload.putBoolean("status", false);
                        sendEvent("onSendResult", payload)
                    })
        }
    }

    @ReactMethod
    fun cancelFile(filePath: String) {

        if(p2pClient == null)
            sendEvent("onSendResult", false)

        // Step 5: Send short messages from third-party apps on the phone to those on the wearable device
        // Build a Message object
        val cancelFile = File(filePath)
        val builder: FileIdentification.Builder = FileIdentification.Builder()
        builder.setFile(cancelFile)
        val fileIdentification: FileIdentification = builder.build()

        val cancelFileTransferCallBack = CancelFileTransferCallBack { errCode -> /*
           * The common result codes are described as follows:
           * errCode:207: File canceled.
           * errCode:1990020004: Failed to cancel the file sending task.
           * errCode:5
           * (1) When the API for canceling file sending is called, the input parameter is empty.
           * (2) The file to be canceled does not exist, the file is not being sent, or the package name of the peer end is incorrect.
           */
            val payload = Arguments.createMap()
            payload.putBoolean("status", false);
            payload.putInt("code", errCode);
            sendEvent("onSendResult", payload)
        }

        // Cancel to send files to the device.
        if (connectedDevice != null && connectedDevice!!.isConnected && fileIdentification != null && cancelFileTransferCallBack != null) {
            p2pClient.cancelFileTransfer(connectedDevice, fileIdentification, cancelFileTransferCallBack)
                    .addOnSuccessListener {
                        // Related processing logic for your app after file sending task is canceled.
                        val payload = Arguments.createMap()
                        payload.putBoolean("status", true);
                        sendEvent("onSendResult", payload)
                    }
                    .addOnFailureListener { e ->
                        val payload = Arguments.createMap()
                        payload.putBoolean("status", false);
                        sendEvent("onSendResult", e.message)
                        // Related processing logic for your app after file sending task is unable to be canceled.
                    }
        }
    }

  fun sendEvent(eventName: String, payload: Any?){
    reactApplicationContext
      .getJSModule(RCTDeviceEventEmitter::class.java)
      .emit(eventName, payload)
  }
}
