import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';

type SendResult = {
  success: boolean;
  messageId: string;
};

type WearEngineType = {
  hasAvailableDevices(): void;
  requestPermission(): void;
  checkPermission(): void;
  getDevices(): void;
  getConnectedDevice(): void;
  setAndPingConnectedDevice(peerPkgName: String, peerFingerPrint: String): void;
  sendMessage(messageStr: String): SendResult;
  sendFile(filePath: String): SendResult;
  cancelFile(filePath: String): SendResult;
  addEventListener(
    event: string,
    listener: (event: any) => any
  ): EmitterSubscription;
  removeEventListener(listener: EmitterSubscription): void;
  removeAllListeners(event: string): void;
};

const { WearEngine } = NativeModules;

const emitter = new NativeEventEmitter(WearEngine);

WearEngine.addEventListener = function (
  event: string,
  listener: (event: any) => any
): EmitterSubscription {
  return emitter.addListener(event, listener);
};

WearEngine.removeEventListener = function (listener: EmitterSubscription) {
  return emitter.removeSubscription(listener);
};

WearEngine.removeAllListeners = function (event: string) {
  return emitter.removeAllListeners(event);
};

export default WearEngine as WearEngineType;
