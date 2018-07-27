package com.crumbdonut;
 
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.InvalidAlgorithmParameterException;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.lang.IllegalArgumentException;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Convert;

import org.json.JSONObject;
import org.json.JSONException;

public class Web3jModule extends ReactContextBaseJavaModule {
  private static final String LOG_TAG = "ReactNativeWeb3j";

  private Context context;
  private Web3j web3;

  public Web3jModule(ReactApplicationContext reactContext) {
    super(reactContext);
    context = reactContext;
  }
 
  @Override
  public String getName() {
    return "Web3jModule";
  }

  @ReactMethod
  public void init(String ethClientUrl, Callback callback) {
    if (ethClientUrl.isEmpty()) {
      web3 = Web3jFactory.build(new HttpService());
      getClientVersion(callback);
    } else if (URLUtil.isValidUrl(ethClientUrl)) {
      web3 = Web3jFactory.build(new HttpService(ethClientUrl));
      getClientVersion(callback);
    } else {
      callback.invoke("Invalid client URL");
    }
  }

  @ReactMethod
  public void getClientVersion(Callback callback) {
    try {
      Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
      String clientVersion = web3ClientVersion.getWeb3ClientVersion();
      callback.invoke(null, clientVersion);
    }
    catch (InterruptedException | ExecutionException e) {
      Log.d(LOG_TAG, e.toString());
      callback.invoke(e.getMessage());
    }
  }

  @ReactMethod
  public void createWallet(String password, Callback callback) {
    String walletFilename;
    File dataDir = context.getDataDir();

    try {
      walletFilename = WalletUtils.generateLightNewWalletFile(password, dataDir);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CipherException | IOException e) {
      Log.d(LOG_TAG, e.toString());
      callback.invoke(e.getMessage());
      return;
    }
    
    String filePath = dataDir.getAbsolutePath() + "/" + walletFilename;
    File walletFile = new File(filePath);
    try {
      WritableNativeMap walletMap = createWalletMapFromFile(walletFile);
      callback.invoke(null, walletMap);
    } catch (JSONException e) {
      String errorMessage = "Error parsing file " + walletFile.getAbsolutePath() + ": " + e.getMessage();
      Log.e(LOG_TAG, errorMessage);
      callback.invoke(errorMessage);
    }
  }

  @ReactMethod
  public void listWallets(Callback callback) {
    File[] files = context.getDataDir().listFiles();

    WritableNativeArray wallets = new WritableNativeArray();
    for (File file : files) {
      if (file.getName().endsWith(".json")) {
        try {
          WritableNativeMap walletMap = createWalletMapFromFile(file);
          wallets.pushMap(walletMap);
        } catch (JSONException e) {
          Log.e(LOG_TAG, "Error parsing file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
      }
    }
    callback.invoke(null, wallets);
  }

  @ReactMethod
  public void getBalance(String address, String unit, Callback callback) {
    WritableNativeMap balance = new WritableNativeMap();
    try {
      EthGetBalance ethGetBalance = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).sendAsync().get();
      BigInteger wei = ethGetBalance.getBalance();
      BigDecimal amount = Convert.fromWei(wei.toString(), Convert.Unit.fromString(unit));
      balance.putString("amount", amount.toString());
      balance.putString("unit", unit);
      callback.invoke(null, balance);
    } catch (InterruptedException | ExecutionException e) {
      Log.e(LOG_TAG, e.getMessage());
      callback.invoke(e.getMessage());
    } catch (IllegalArgumentException e) {
      String message = "Invalid conversion unit. Please use one of the following: " + getUnitNames();
      Log.e(LOG_TAG, message);
      callback.invoke(message);
    }
  }

  private static String getUnitNames() {
    ArrayList arr = new ArrayList<Convert.Unit>(Arrays.asList(Convert.Unit.values()));
    return TextUtils.join(", ", arr);
  }

  private WritableNativeMap createWalletMapFromFile(File walletFile) throws JSONException {
    WritableNativeMap walletMap = new WritableNativeMap();
    JSONObject walletJson = walletAsJson(walletFile);
    String address = walletJson.getString("address");
    walletMap.putString("shortAddress", "0x" + address.substring(0, 5) + "..." + address.substring(address.length() - 5, address.length()));
    walletMap.putString("address", "0x" + address);
    walletMap.putString("filePath", walletFile.getAbsolutePath());

    return walletMap;
  }

  private JSONObject walletAsJson(File walletFile) throws JSONException {
    return new JSONObject(readJsonFile(walletFile));
  }
  
  private String readJsonFile(File file) {
    String json = null;
    try {
      FileInputStream stream = new FileInputStream(file);
      int size = stream.available();
      byte[] buffer = new byte[size];
      stream.read(buffer);
      stream.close();
      json = new String(buffer, "UTF-8");
    } catch (IOException e) {
      Log.e(LOG_TAG, e.getMessage());
    }

    return json;
  }
}