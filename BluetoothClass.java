package com.vritti.inventory.physicalInventory.bean;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.vritti.inventory.physicalInventory.activity.Devicelist_LablelPrint;
import com.vritti.sales.CounterBilling.DeviceListActivity;
import com.zj.btsdk.BluetoothService;

public class BluetoothClass extends Application {
	static Activity act = null;
	static Context context = null;
	static String bluetootMacAddress = null;
	static boolean printerConnected = false, btOnOffConnect = false, printerConnecting = false;
	private static final int REQUEST_ENABLE_BT = 4;
	static BluetoothService mService = null;
	static BluetoothDevice con_dev = null;
	private static SharedPreferences pref = null;
	private static final int REQUEST_CONNECT_DEVICE = 6;
	public static boolean isPrinterConnected(Context con, Activity act1){
		context = con;
		act = act1;
		if(printerConnected)
			return true;
		else
			return false;
	}

	public static boolean isPrinterConnected(){
		if(printerConnected)
			return true;
		else
			return false;
	}

	public static BluetoothService getServiceInstance(){
		return mService;
	}

	public static void connectPrinter(Context con, Activity act1) {
		Log.v("HyperMan","connect printer");
		context = con;
		act = act1;IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		act.registerReceiver(mReceiver, filter);
		if(printerConnecting){
			Toast.makeText(context, "Trying to connect printer!!", Toast.LENGTH_LONG).show();
		}
		else{
			mService = new BluetoothService(context, mHandler);

			if( mService.isAvailable() == false ){
				Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_LONG).show();
			}
			else if( mService.isBTopen() == true){

				if(bluetootMacAddress==null)
					pairPrinter(con, act1);
				else{
					if(!btOnOffConnect){
						btOnOffConnect = true;
						//IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
						context.registerReceiver(mReceiver, filter);
					}
					con_dev = mService.getDevByMac(bluetootMacAddress);
					mService.connect(con_dev);
				}
			}
			else if( mService.isBTopen() == false)
			{
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				act.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	public static void connectPrinterAuto(Context con, Activity act1) {
		Log.v("HyperMan","connect printer");
		context = con;
		act = act1;
		IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		act.registerReceiver(mReceiver, filter);
		if(printerConnecting){
			Toast.makeText(context, "Trying to connect printer!!", Toast.LENGTH_LONG).show();
		}
		else{
			mService = new BluetoothService(context, mHandler);

			if( mService.isAvailable() == false ){
				Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_LONG).show();
			}
			else if( mService.isBTopen() == true){
				if(bluetootMacAddress==null)
					pairPrinter(con, act1);
				else{
					if(!btOnOffConnect){
						btOnOffConnect = true;
						//IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
						context.registerReceiver(mReceiver, filter);
					}
					con_dev = mService.getDevByMac(bluetootMacAddress);
					mService.connect(con_dev);
				}
			}
			else if( mService.isBTopen() == false)
			{
				Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				act.startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	public static void pairPrinter(Context con, Activity act1){
		context = con;
		act = act1;
		if(!btOnOffConnect){
			btOnOffConnect = true;
			IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
			context.registerReceiver(mReceiver, filter);
		}
		//Intent serverIntent = new Intent(context, DeviceListActivity.class);
		Intent serverIntent = new Intent(context, Devicelist_LablelPrint.class);
		//Intent serverIntent = new Intent(context, DeviceListActivityNew.class);
		act.startActivityForResult(serverIntent,REQUEST_CONNECT_DEVICE);
	}

	public static void pairedPrinterAddress(Context con, Activity act1, String mac_address){
		context = con;
		act = act1;

		bluetootMacAddress = mac_address;
		con_dev = mService.getDevByMac(mac_address);
		mService.connect(con_dev);
	}

	public static void disconnect(){
		if(mService!=null&&mService.isAvailable())
			mService.stop();
		mService=null;
		bluetootMacAddress = null;
		con_dev = null;
		btOnOffConnect = false;
		printerConnected = false;
		context.unregisterReceiver(mReceiver);
	}

	private final static Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case BluetoothService.MESSAGE_STATE_CHANGE:
					switch (msg.arg1) {
						case BluetoothService.STATE_CONNECTED:   //������
							printerConnected = true;
							Toast.makeText(context, "Connect successful",
									Toast.LENGTH_SHORT).show();
							printerConnecting = false;
							break;
						case BluetoothService.STATE_CONNECTING:  //��������
							printerConnected = false;
							printerConnecting = true;
							break;
						case BluetoothService.STATE_LISTEN:
						case BluetoothService.STATE_NONE:
							printerConnected = false;
							printerConnecting = false;
							break;
					}
					break;
				case BluetoothService.MESSAGE_CONNECTION_LOST:    //�����ѶϿ�����
					Toast.makeText(context, "Printer Connection lost",
							Toast.LENGTH_SHORT).show();
					printerConnected = false;
					disconnect();
					break;
				case BluetoothService.MESSAGE_UNABLE_CONNECT:     //�޷������豸
					printerConnected = false;
					Toast.makeText(context, "Unable to connect device",
							Toast.LENGTH_SHORT).show();
					disconnect();
					break;
			}
		}
	};
	//this is another extra receiver to check if BT is turned off suddenly to avoid force close dialog
	private final static BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
						BluetoothAdapter.ERROR);
				switch (state) {
					case BluetoothAdapter.STATE_OFF:
						disconnect();
						break;
					case BluetoothAdapter.STATE_TURNING_OFF:
						disconnect();
						break;
					case BluetoothAdapter.STATE_ON:
						//setButtonText("Bluetooth on");
						break;
					case BluetoothAdapter.STATE_TURNING_ON:
						//setButtonText("Turning Bluetooth on...");
						break;
				}
			}
			else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)){
				disconnect();
			}
			else if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)){
				disconnect();
			}
		}
	};

	protected void onDestroy() {
		if (mService != null)
			mService.stop();
		mService = null;
		unregisterReceiver(mReceiver);
	}
}
