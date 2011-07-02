	package thesis.sec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipException;

import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.service.DecryptService;
import nl.siegmann.epublib.util.IOUtil;
import nl.siegmann.epublib.util.ResourceUtil;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Decrypter implements DecryptService {

	private static final String TAG = "Decrypter";
	private boolean isEncrypted = true;
	private String epubContainerPath;
	private String packagePath;
	private Context context;
	
	static{
		System.loadLibrary("loader");
		Log.d(TAG, "library loaded");
	}

	private native void dec(byte[] data);
	
	public Decrypter(Context context){
		this.context = context;
	}

	public Decrypter(String epubContainerPath, Object obj) {
		this.epubContainerPath = epubContainerPath;
		this.context = (Context)obj;

		PackageManager pm = context.getPackageManager();
		String dataPath = null;
		try {
			dataPath = pm.getApplicationInfo(context.getPackageName(), 0).dataDir;
		} catch (NameNotFoundException e) {
			Log.e(TAG, e.getMessage());
		}

		this.packagePath = dataPath;
		copyLib();
	}


	private void copyLib() {

		File destFile = new File(packagePath + "/libdec.so");
		if(destFile.exists())
			destFile.delete();
		
		try {
			Resource decResource = ResourceUtil.getResourceFromEpub(
					epubContainerPath, "libdec.so");
			if (decResource != null) {

				InputStream in = decResource.getInputStream();
				OutputStream out = new FileOutputStream(destFile);
				IOUtil.copy(in, out);
			} else {
				isEncrypted = false;
			}
		} catch (ZipException e) {
			isEncrypted = false;
		} catch (IOException e) {
			isEncrypted = false;
		}
	}

	@Override
	public byte[] decrypt(byte[] data) throws InvalidKeyException {
		if (isEncrypted) {
			dec(data);
		}
		return data;
	}

	@Override
	public byte[] decrypt(InputStream stream) throws InvalidKeyException {
		byte[] data = null;
		try {
			data = IOUtil.toByteArray(stream);
			if (isEncrypted) {
				dec(data);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		return data;
	}

	@Override
	public byte[] decrypt(Reader reader) throws InvalidKeyException {
		byte[] data = null;
		try {
			data = IOUtil.toByteArray(reader, "UTF-8"); // TODO: make encoding
														// dynamic
			if (isEncrypted) {
				dec(data);
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}

		return data;
	}

	@Override
	public Resource decrypt(Resource resource) throws InvalidKeyException {
		byte[] data = resource.getData();
		if (isEncrypted) {
			dec(data);
			resource.setData(data);
		}
		return resource;
	}

	public String getUniqueIdentifier() {
		String uniqueID = new String();

		// 1 IMEI - some devices( ex. tablets) doesn't have it
		TelephonyManager TelephonyMgr = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = TelephonyMgr.getDeviceId(); // Requires READ_PHONE_STATE

		// 2 compute DEVICE ID
		String deviceIDshort = "35"
				+ // we make this look like a valid IMEI
				Build.BOARD.length() % 10 + Build.BRAND.length() % 10
				+ Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10
				+ Build.DISPLAY.length() % 10 + Build.HOST.length() % 10
				+ Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10
				+ Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10
				+ Build.TAGS.length() % 10 + Build.TYPE.length() % 10
				+ Build.USER.length() % 10; // 13 digits

		// 3 android ID - unreliable
		String androidID = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);

		// 4 wifi manager, read MAC address (it can be spoofed)- requires
		// android.permission.ACCESS_WIFI_STATE or comes as null
		WifiManager wm = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		String wlanMAC = wm.getConnectionInfo().getMacAddress();

		// 5 Bluetooth MAC address android.permission.BLUETOOTH required
		// Bluetooth must be ON..Unreliable for our case
		// BluetoothAdapter m_BluetoothAdapter = null; // Local Bluetooth
		// adapter
		// m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// String bluetoothMAC = null;
		// if (m_BluetoothAdapter != null)
		// bluetoothMAC = m_BluetoothAdapter.getAddress();

		// 6 SUM THE IDs
		String longID = imei + deviceIDshort + androidID + wlanMAC;// +
																	// bluetoothMAC;
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5"); // the quickest among the rest
													// of hash methods.Also it
													// has the quickest
													// implementation in Java6
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		m.update(longID.getBytes(), 0, longID.length());
		byte p_md5Data[] = m.digest();

		for (int i = 0; i < p_md5Data.length; i++) {
			int b = (0xFF & p_md5Data[i]);
			// if it is a single digit, make sure it have 0 in front (proper
			// padding)
			if (b <= 0xF)
				uniqueID += "0";
			// add number to string
			uniqueID += Integer.toHexString(b);
		}
		uniqueID = uniqueID.toUpperCase();

		return uniqueID;
	}
	
	public void throwException() throws InvalidKeyException{
		throw new InvalidKeyException("wrong device");
	}
	
	public void finalize() {
        System.out.println("A garbage collected");
    }

}
