package thesis.sec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class Utils {

	public static char[] getUniqueIdentifier(Context context) {
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
		BluetoothAdapter m_BluetoothAdapter = null; // Local Bluetooth adapter
		m_BluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		String bluetoothMAC = m_BluetoothAdapter.getAddress();
		
		//6 SUM THE IDs
    	String longID = imei + deviceIDshort + androidID+ wlanMAC + bluetoothMAC;
    	MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5"); //the quickest among the rest of hash methods.Also it has the quickest implementation in Java6
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 
		m.update(longID.getBytes(),0,longID.length());
		byte p_md5Data[] = m.digest();
		
		for (int i=0;i<p_md5Data.length;i++) {
			int b =  (0xFF & p_md5Data[i]);
			// if it is a single digit, make sure it have 0 in front (proper padding)
			if (b <= 0xF) uniqueID+="0";
			// add number to string
			uniqueID+=Integer.toHexString(b); 
		}
		uniqueID = uniqueID.toUpperCase();

		return uniqueID.toCharArray();
	}

}
