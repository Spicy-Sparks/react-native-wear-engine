import { NativeEventEmitter, NativeModules } from 'react-native';

type WearEngineType = {
  hasAvailableDevices(): void;
  requestPermission(): void;
  checkPermission(): void;
  getDevices(): void;
  getConnectedDevice(): void;
  setAndPingConnectedDevice(peerPkgName: String, peerFingerPrint: String): void;
  sendMessage(messageStr: String): void;
  sendFile(filePath: String): void;
  cancelFile(filePath: String): void;
  addEventListener(event : String, listener : any) : void;
};

const { WearEngine } = NativeModules;

const emitter = new NativeEventEmitter(WearEngine);

WearEngine.addEventListener = function(event : string, listener : any) {
    return emitter.addListener(event, listener);
}

export default WearEngine as WearEngineType;
