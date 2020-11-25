import { NativeModules } from 'react-native';

type WearEngineType = {
  multiply(a: number, b: number): Promise<number>;
};

const { WearEngine } = NativeModules;

export default WearEngine as WearEngineType;
