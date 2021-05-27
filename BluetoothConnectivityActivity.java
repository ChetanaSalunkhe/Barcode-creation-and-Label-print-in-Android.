package com.vritti.inventory.physicalInventory.activity;

import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.helper.printer.BtService;
import com.qs.helper.printer.Device;
import com.qs.helper.printer.PrintService;
import com.qs.helper.printer.PrinterClass;
import com.vritti.ekatm.R;
import com.zj.btsdk.BluetoothService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothConnectivityActivity extends ListActivity {

    private TextView textPrint_text,imagePrint_text,barPrint_text,eqPrint_text,setting_text,codeChage_text;
    private ImageView textPrint_image,imagePrint_image,barPrint_image,eqPrint_image,setting_image,codeChage_image;
    LinearLayout layprint;

    public static PrinterClass pl = null;
    Handler mhandler = null;
    Handler handler = null;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private Thread tv_update;
    // TextView textView_state;
    BluetoothService mService = null;
    BluetoothDevice con_dev = null;
    private boolean deviceConnected = false;
    public static final int REQUEST_ENABLE_BT = 4;
    public static final int REQUEST_CONNECT_DEVICE = 6;

    protected static final String TAG = "BluetoothConnectivityActivity";
    public static boolean checkState = true;
    TextView textView_state;

    String radStatus = "";
    private SharedPreferences sharedPrefs;
    String labelSize = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connectivity);

        //init();
        setListAdapter(new SimpleAdapter(this, getData("simple-list-item-2"),
                android.R.layout.simple_list_item_2, new String[] { "title",
                "description" }, new int[] { android.R.id.text1,
                android.R.id.text2 }));

        mhandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        Log.e("Test", "readBuf:" + readBuf[0]);
                        if (readBuf[0] == 0x13) {
                            PrintService.isFUll = true;
                            ShowMsg(getResources().getString(R.string.str_printer_state)+":"+getResources().getString(R.string.str_printer_bufferfull));
                        } else if (readBuf[0] == 0x11) {
                            PrintService.isFUll = false;
                            ShowMsg(getResources().getString(R.string.str_printer_state)+":"+getResources().getString(R.string.str_printer_buffernull));
                        } else if (readBuf[0] == 0x08) {
                            ShowMsg(getResources().getString(R.string.str_printer_state)+":"+getResources().getString(R.string.str_printer_nopaper));
                        } else if (readBuf[0] == 0x01) {
                            //ShowMsg(getResources().getString(R.string.str_printer_state)+":"+getResources().getString(R.string.str_printer_printing));
                        }  else if (readBuf[0] == 0x04) {
                            ShowMsg(getResources().getString(R.string.str_printer_state)+":"+getResources().getString(R.string.str_printer_hightemperature));
                        } else if (readBuf[0] == 0x02) {
                            ShowMsg(getResources().getString(R.string.str_printer_state)+":"+getResources().getString(R.string.str_printer_lowpower));
                        }else {
                            String readMessage = new String(readBuf, 0, msg.arg1);
                            Log.e("", "readMessage"+readMessage);
                            //if (readMessage.contains("800"))// 80mm paper
                            if (readMessage.contains("500"))// 50mm paper
                            {
                                PrintService.imageWidth = 72;
                                Toast.makeText(BluetoothConnectivityActivity.this, "80mm", Toast.LENGTH_SHORT).show();
                                Log.e("", "imageWidth:"+"80mm");
                            } else if (readMessage.contains("580"))// 58mm paper
                            {
                                PrintService.imageWidth = 48;
                                Toast.makeText(BluetoothConnectivityActivity.this, "58mm", Toast.LENGTH_SHORT).show();
                                Log.e("", "imageWidth:"+"58mm");
                            }
                        }
                        break;
                    case MESSAGE_STATE_CHANGE:// 蓝牙连接状
                        switch (msg.arg1) {
                            case PrinterClass.STATE_CONNECTED:// 已经连接
                                break;
                            case PrinterClass.STATE_CONNECTING:// 正在连接
                                Toast.makeText(getApplicationContext(),
                                        "STATE_CONNECTING", Toast.LENGTH_SHORT).show();
                                break;
                            case PrinterClass.STATE_LISTEN:
                            case PrinterClass.STATE_NONE:
                                break;
                            case PrinterClass.SUCCESS_CONNECT:
                                pl.write(new byte[] { 0x1b, 0x2b });// 检测打印机型号
                                Toast.makeText(getApplicationContext(), "SUCCESS_CONNECT", Toast.LENGTH_SHORT).show();
                                break;
                            case PrinterClass.FAILED_CONNECT:
                                Toast.makeText(getApplicationContext(), "FAILED_CONNECT", Toast.LENGTH_SHORT).show();
                                break;
                            case PrinterClass.LOSE_CONNECT:
                                Toast.makeText(getApplicationContext(), "LOSE_CONNECT", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case MESSAGE_WRITE:

                        break;
                }
                super.handleMessage(msg);
            }
        };

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        break;
                    case 1:// 閹殿偅寮跨�灞剧�?
                        Device d = (Device) msg.obj;
                        if (d != null) {
                            if (Devicelist_NewPrinterSetting.deviceList == null) {
                                Devicelist_NewPrinterSetting.deviceList = new ArrayList<Device>();
                            }
                            if (!checkData(Devicelist_NewPrinterSetting.deviceList, d)) {
                                Devicelist_NewPrinterSetting.deviceList.add(d);
                            }
                        }
                        break;
                    case 2:// 閸嬫粍顒涢幍顐ｅ�?
                        break;
                }
            }
        };

        tv_update = new Thread() {
            public void run() {
                while (true) {
                    if (checkState) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // TODO Auto-generated method stub
                        if (BluetoothConnectivityActivity.pl != null) {
                            if (BluetoothConnectivityActivity.pl.getState() == PrinterClass.STATE_CONNECTED) {
                                BluetoothConnectivityActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                     //   Toast.makeText(BluetoothConnectivityActivity.this,"Connected",Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else if (BluetoothConnectivityActivity.pl.getState() == PrinterClass.STATE_CONNECTING) {
                                BluetoothConnectivityActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                     //   Toast.makeText(BluetoothConnectivityActivity.this,"Connecting",Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else if (BluetoothConnectivityActivity.pl.getState() == PrinterClass.LOSE_CONNECT
                                    || BluetoothConnectivityActivity.pl.getState() == PrinterClass.FAILED_CONNECT) {
                                checkState = false;
                                BluetoothConnectivityActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                      //  Toast.makeText(BluetoothConnectivityActivity.this,"disconnected",Toast.LENGTH_SHORT).show();
                                    }
                                });
                                Intent intent = new Intent();
                                intent.setClass(BluetoothConnectivityActivity.this,
                                        Devicelist_NewPrinterSetting.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(intent);
                            } else {
                                BluetoothConnectivityActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                     //   Toast.makeText(BluetoothConnectivityActivity.this,"disconnected",Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                }
            }
        };
        tv_update.start();

        BluetoothConnectivityActivity.pl = new BtService(this, mhandler, handler);
        Intent intent = new Intent();
        intent.putExtra("position", 0);
        intent.setClass(BluetoothConnectivityActivity.this, CounterAuditorSelectActvity.class);
        startActivity(intent);
        PrintService.imageWidth = 72;
    }

    private boolean checkData(List<Device> list, Device d) {
        for (Device device : list) {
            if (device.deviceAddress.equals(d.deviceAddress)) {
                return true;
            }
        }
        return false;
    }

    protected void onListItemClick(ListView listView, View v, int position, long id) {
        BluetoothConnectivityActivity.checkState = true;
        Intent intent = new Intent();
        intent.putExtra("position", position);
        intent.setClass(BluetoothConnectivityActivity.this, CounterAuditorSelectActvity.class);

        switch (position) {
            case 0:
                startActivity(intent);
                break;
            case 1:
                startActivity(intent);
                break;
            case 2:

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        /*if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            BluetoothClass.pairPrinter(getApplicationContext(), BluetoothConnectivityActivity.this);

        }else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            String address = data.getExtras().getString(Devicelist_LablelPrint.EXTRA_DEVICE_ADDRESS).split("\n")[1];
            BluetoothClass.pairedPrinterAddress(getApplicationContext(), this,address);
        }*/

        switch (resultCode)
        {
            case 0:
                if(BluetoothClass_QSPrinter.pl.getState() != PrinterClass.STATE_CONNECTED)
                {
                    BluetoothConnectivityActivity.this.finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        checkState = true;
        super.onRestart();
    }

    private void ShowMsg(String msg){
        Toast.makeText(getApplicationContext(), msg,
                Toast.LENGTH_SHORT).show();
    }

    private List<Map<String, String>> getData(String title) {
        List<Map<String, String>> listData = new ArrayList<Map<String, String>>();

        Map<String, String> map = new HashMap<String, String>();
        map.put("title", getResources().getString(R.string.mode_bt));
        map.put("description", "");
        listData.add(map);

		/*map = new HashMap<String, String>();
		map.put("title", getResources().getString(R.string.mode_wifi));
		map.put("description", "");
		listData.add(map);

		map = new HashMap<String, String>();
		map.put("title", getResources().getString(R.string.mode_usb));
		map.put("description", "");
		listData.add(map);*/

        return listData;
    }

}
