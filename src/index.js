import { NativeEventEmitter, NativeModules, } from 'react-native';
const { WearEngine } = NativeModules;
const emitter = new NativeEventEmitter(WearEngine);
WearEngine.addEventListener = function (event, listener) {
    return emitter.addListener(event, listener);
};
WearEngine.removeEventListener = function (listener) {
    return listener.remove && listener.remove()
};
WearEngine.removeAllListeners = function (event) {
    return emitter.removeAllListeners(event);
};
export default WearEngine;
