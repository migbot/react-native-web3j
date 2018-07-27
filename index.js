import { NativeModules } from 'react-native';

const Web3jModule = NativeModules.Web3jModule;

function handleResponse(error, result, resolve, reject) {
  if (error) {
    reject(error);
  } else {
    resolve(result);
  }
}

class Web3j {
  constructor() {
    // init listeners + subscriptions
  }

  initClient(url='') {
    return new Promise((resolve, reject) => {
      Web3jModule.init(url, (error, result) =>
        handleResponse(error, result, resolve, reject));
    });
  }

  listWallets() {
    return new Promise((resolve, reject) => {
      Web3jModule.listWallets((error, result) =>
        handleResponse(error, result, resolve, reject));
    });
  }

  createWallet(password) {
    return new Promise((resolve, reject) => {
      Web3jModule.createWallet(password, (error, result) =>
        handleResponse(error, result, resolve, reject));
    });
  }

  getBalance(address, unit='wei') {
    return new Promise((resolve, reject) => {
      Web3jModule.getBalance(address, unit, (error, result) =>
        handleResponse(error, result, resolve, reject));
    });
  }
}

export default new Web3j();