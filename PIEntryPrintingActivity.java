package com.vritti.inventory.physicalInventory.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.beeprt.print.PrintPP_CPCL;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Writer;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.vritti.databaselib.data.DatabaseHandlers;
import com.vritti.databaselib.other.Utility;
import com.vritti.databaselib.other.WebUrlClass;
import com.vritti.ekatm.R;
import com.vritti.ekatm.services.SendOfflineData;
import com.vritti.inventory.bean.PrintDataClass_QSPrinter;
import com.vritti.inventory.physicalInventory.Interface.PrintCompleteService;
import com.vritti.inventory.physicalInventory.adapter.BatchListAdapter;
import com.vritti.inventory.physicalInventory.adapter.LocationListAdapter;
import com.vritti.inventory.physicalInventory.adapter.PrintServicesAdapter;
import com.vritti.inventory.physicalInventory.bean.BatchList;
import com.vritti.inventory.physicalInventory.bean.BeanTag;
import com.vritti.inventory.physicalInventory.bean.BeanTagList;
import com.vritti.inventory.physicalInventory.bean.BluetoothClass;
import com.vritti.inventory.physicalInventory.bean.Constants_wifi;
import com.vritti.inventory.physicalInventory.bean.LocationList;
import com.vritti.inventory.physicalInventory.bean.Util_Wifi_print;
import com.vritti.inventory.physicalInventory.bean.Utils_print;
import com.vritti.inventory.physicalInventory.bean.WifiScanner;
import com.vritti.sales.CounterBilling.DeviceListActivity;
import com.vritti.sales.beans.BillNoClass;
import com.vritti.sessionlib.CallbackInterface;
import com.vritti.sessionlib.StartSession;
import com.vritti.vwb.CommonClass.AppCommon;
import com.vritti.vwb.classes.CommonFunction;
import com.zj.btsdk.BluetoothService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import static java.lang.String.valueOf;

public class PIEntryPrintingActivity extends AppCompatActivity implements PrintCompleteService {
    ImageView img_barcode;
    Button btnprint, btncancel,btn_prev_tag;
    LinearLayout addressLayout, locationarea, countedbyayout;
    Spinner spinner_pinumber;   //spinner_location;
    ProgressDialog progressDialog_1;
    TextView txt_release, txt_runtag, txtbatchno, txtwarehouse, txtuom;
    EditText edt_qty, edt_weight, edt_countedby, edt_area, edt_verifyby, edt_tag_desc;
    TextView edt_itemcode, edt_description, edt_location;

    private static final int REQUEST_WRITE_PERMISSION = 786;

    SharedPreferences.Editor editor;
    String PlantMasterId ="", LoginId="", Password="", CompanyURL="", EnvMasterId="",
            UserMasterId="",UserName = "", MobileNo = "",Indentamount,ItemPlantId="";
    DatabaseHandlers db;
    CommonFunction cf;
    Utility ut;
    SQLiteDatabase sql;
    ArrayList<String> ItemCodelist;
    ArrayList<String> ItemDesclist;

    BluetoothService mService = null;
    BluetoothDevice con_dev = null;
    private boolean deviceConnected = false;
    public static final int REQUEST_ENABLE_BT = 4;
    public static final int REQUEST_CONNECT_DEVICE = 6;
    public static final int REQ_PARTCODE = 7;
    public static final int REQ_PARTNAME = 8;
    public static final int REQ_LOCATION = 9;

    public static String today, todaysDate;
    public static String date = null;
    public static String time = null;
    String DATE = "", TIME = "";
    DatePickerDialog datePickerDialog;
    static int year, month, day;
    BillNoClass billNoClass;
    double CONV_factor = 1;
    String UOMVAL = "";
    String billnumber = "0000";

    /********************************************************************************************************/
    private WifiConfiguration mPrinterConfiguration, mOldWifiConfiguration;
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResults = new ArrayList<ScanResult>();
    private WifiScanner mWifiScanner;

    private PrintManager mPrintManager;
    private List<PrintJob> mPrintJobs;
    private PrintJob mCurrentPrintJob;

    private File pdfFile;
    private String externalStorageDirectory;

    private Handler mPrintStartHandler = new Handler();
    private Handler mPrintCompleteHandler = new Handler();
    private String connectionInfo;

    private boolean isMobileDataConnection = false;
    OutputStream outputStream ;

    ArrayList<BatchList>batchListArrayList;
    ArrayList<LocationList>locationListArrayList;
    BatchListAdapter batchListAdapter;
    LocationListAdapter locationListAdapter;

    String BatchHdID="",LocationID="",LocationCode="";
    public String FinalObj;
    private SharedPreferences sharedPrefs;
    String labelSize = "",selPrinterName="";
    Gson gson;
    private String json;
    Type type;
    int  TagCurrentNo,TagEndNo,TagStartNo;
    String batchHDR = "", batchCODE = "";
    int billNO = 0;
    String Status="",BatchPrint="",uuidInString="";
    private UUID uuid;
    String selectedBatchFlag = "";
    int selectedBatchPosition;
    boolean selectedtchRelease = false;
    BeanTagList beanTagObj;
    BeanTag glBeanObject;
    String keyForBatch = "", WareHouseMasterId = "", warehousecode = "";
    String duplicateCountedBy = "";

    /*for previous tag edit*/
    String PIHdrId = "", PIDtlId = "", AuditedItemPlantId = "", AuditedItemCode = "", AuditedItemDesc = "", AuditedLocationCode = "",
            AuditedLocationMasterId = "", AuditedWeight = "", AuditedActualQty = "", AuditedVerifyBy = "", TAGNO = "",
            AuditedWareHouseMasterId = "", AuditedWarehouseCode = "";

    String OLDItemPlantId = "", OLDItemCode = "", OLDItemDesc = "", OLDLocationCode = "",OLDLocationMasterId = "",
            OLDWeight = "", OLDActualQty = "", OLDVerifyBy = "", OLDWareHouseMasterId = "", OLDWarehouseCode = "";

    PrintLabel printLabel;
    PrintPP_CPCL printPP_cpcl;
    private PrintLabel pl;
    private int interval;
    View viewforprint;
    private boolean isSending = false;
    private boolean isConnected = false;
    private ProgressDialog dialog;
    private BluetoothAdapter mBluetoothAdapter;
    PrintDataClass_QSPrinter dataClass_qsPrinter;

    @SuppressLint("WifiManagerLeak")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_lay);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy =
                    new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        init();

        /*LTK printer*/
        /*mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //If the Bluetooth adapter is not supported,programmer is over
        if (mBluetoothAdapter == null) {
            Toast.makeText(PIEntryPrintingActivity.this,
                    "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }*/

        /*Intent serverIntent = new Intent(PIEntryPrintingActivity.this, Devicelist_LablelPrint.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);*/
      //  printPP_cpcl = new PrintPP_CPCL();

        dataClass_qsPrinter = new PrintDataClass_QSPrinter();

        getPrevCountedByName();

        setListeners();
    }

    @SuppressLint("WifiManagerLeak")
    private void init() {

        img_barcode=findViewById(R.id.img_barcode);
        edt_itemcode=findViewById(R.id.edt_itemcode);
        edt_description=findViewById(R.id.edt_description);
        edt_location = findViewById(R.id.edt_location);
        edt_qty=findViewById(R.id.edt_qty);
        edt_weight = findViewById(R.id.txt_weight);
       // spinner_location = findViewById(R.id.spinner_location);
        edt_countedby = findViewById(R.id.edt_countedby);
        edt_verifyby = findViewById(R.id.edt_verifyby);
        edt_tag_desc = findViewById(R.id.edt_tag_desc);
        edt_area = findViewById(R.id.edt_area);
        /*spinner_pinumber=findViewById(R.id.spinner_pinumber);
        spinner_pinumber.setEnabled(true);*/
        txt_release=findViewById(R.id.txt_release);
        progressDialog_1 = new ProgressDialog(PIEntryPrintingActivity.this);
        btnprint = findViewById(R.id.btnprint);
        btncancel = findViewById(R.id.btncancel);
        addressLayout = findViewById(R.id.addressLayout);
        locationarea = findViewById(R.id.locationarea);
        countedbyayout = findViewById(R.id.countedbyayout);
        txt_runtag = findViewById(R.id.txt_runtag);
        txtbatchno = findViewById(R.id.txtbatchno);
        txtwarehouse = findViewById(R.id.txtwarehouse);
        txtuom = findViewById(R.id.txtuom);
        btn_prev_tag = findViewById(R.id.btn_prev_tag);

        ut = new Utility();
        cf = new CommonFunction(PIEntryPrintingActivity.this);
        String settingKey = ut.getSharedPreference_SettingKey(PIEntryPrintingActivity.this);
        String dabasename = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_DATABASE_NAME_KEY, settingKey);
        db = new DatabaseHandlers(PIEntryPrintingActivity.this, dabasename);
        CompanyURL = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_COMPANY_URL_KEY, settingKey);
        EnvMasterId = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_EnvMasterID_KEY, settingKey);
        PlantMasterId = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_PlantID_KEY, settingKey);
        LoginId = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_LOGIN_KEY, settingKey);
        Password =ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_PSW_KEY, settingKey);
        UserMasterId = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_USERMASTERID_KEY, settingKey);
        UserName = ut.getValue(PIEntryPrintingActivity.this, WebUrlClass.GET_USERNAME_KEY, settingKey);
        sql = db.getWritableDatabase();

        mService = new BluetoothService(PIEntryPrintingActivity.this, mHandler);
        if (mService.isAvailable() == false) {
            Toast.makeText(PIEntryPrintingActivity.this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        /*********************************** Wifi **********************************************/
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mWifiScanner = new WifiScanner();
        /***************************************************************************************/

        Intent intent=getIntent();
        Status=intent.getStringExtra("status");
        BatchPrint=intent.getStringExtra("bathchprint");

        edt_verifyby.setText(UserName);

      /*  Toast.makeText(this,"Selected batch is "+batchCODE+" mode - "+Status+", printmode - "+BatchPrint,
                Toast.LENGTH_SHORT).show();*/

        ItemCodelist = new ArrayList<String>();
        ItemDesclist = new ArrayList<String>();
        billNoClass = new BillNoClass();
        beanTagObj = new BeanTagList();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
        batchCODE = sharedPrefs.getString("selectedBatchCode", "");
        BatchHdID = sharedPrefs.getString("selectedBatchHDRID", "");
        WareHouseMasterId = sharedPrefs.getString("WareHouseMasterId", "");
        warehousecode = sharedPrefs.getString("warehousecode", "");
        TagStartNo = sharedPrefs.getInt("startNo",0);
        TagEndNo = sharedPrefs.getInt("endNo",0);
        //countedPI = sharedPrefs.getString("countedPI", "");
        //uploadPendingPI = sharedPrefs.getString("uploadPendingPI", "");

        txtbatchno.setText(batchCODE);
        txtwarehouse.setText(warehousecode);

        if (AppCommon.getInstance(this).getBillNo_print().equals("")) {

        } else {
            billNoClass = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BillNoClass.class);
            beanTagObj = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BeanTagList.class);
            if(beanTagObj!= null ){
                if(beanTagObj.getBeanTagArrayList() != null){
                    for(BeanTag beanTag : beanTagObj.getBeanTagArrayList() ){
                        if(batchCODE.equalsIgnoreCase(beanTag.getBatchNo())){
                            glBeanObject = beanTag;
                        //    Toast.makeText(this,"Selected tag is "+glBeanObject.getTagNo(), Toast.LENGTH_SHORT).show();

                           String  newTag = "0000" + String.valueOf(glBeanObject.getTagNo()+1);
                            //StringUtils.leftPad("129018", 10, "0");   //padding
                            if (newTag.length() == 5) {
                                newTag = newTag.substring(newTag.length() - 5, 5);
                            } else if (newTag.length() == 6) {
                                newTag = newTag.substring(newTag.length() - 5, 6);
                            } else if (newTag.length() == 7) {
                                newTag = newTag.substring(newTag.length() - 5, 7);
                            } else if (newTag.length() == 8) {
                                newTag = newTag.substring(newTag.length() - 5, 8);
                            }else if (newTag.length() == 9) {
                                newTag = newTag.substring(newTag.length() - 5, 9);
                            }

                            txt_runtag.setText(newTag);
                        }
                    }
                }
            }
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
        labelSize = sharedPrefs.getString("labelSize", "3x2mm");
        selPrinterName = sharedPrefs.getString("PrinterName", "");


        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
        gson = new Gson();
        json = sharedPrefs.getString("batch", "");
        type = new TypeToken<List<BatchList>>() {}.getType();
        batchListArrayList = gson.fromJson(json, type);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
        gson = new Gson();
        json = sharedPrefs.getString("location", "");
        type = new TypeToken<List<LocationList>>() {}.getType();
        locationListArrayList = gson.fromJson(json, type);

    }

    private void setListeners() {
        txt_release.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callReleaseAPI();
            }
        });

        btncancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        img_barcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(PIEntryPrintingActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setOrientationLocked(true);
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();

            }
        });

        edt_itemcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PIEntryPrintingActivity.this, PartCodeActivity.class);
                startActivityForResult(intent,REQ_PARTCODE);
            }
        });

        edt_description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PIEntryPrintingActivity.this, PartNameActivity.class);
                startActivityForResult(intent,REQ_PARTNAME);
            }
        });

        edt_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PIEntryPrintingActivity.this, LocationPIActivity.class);
                startActivityForResult(intent,REQ_LOCATION);
            }
        });

        btn_prev_tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get previous tag details edit it,send to server, save to local and and take printout
                final String prevTag = String.valueOf(glBeanObject.getTagNo());
                Intent intent = new Intent(PIEntryPrintingActivity.this, EditDeleteScreenActivity.class);
                intent.putExtra("PrevTAG",prevTag);
                intent.putExtra("CallFrom","PIENTRY");
                intent.putExtra("status",Status);
                intent.putExtra("bathchprint",BatchPrint);
                startActivity(intent);

            }
        });


        edt_weight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String edt = s.toString();

                if( edt.equals("")){

                }else {

                    if(CONV_factor == 1){
                        edt_qty.setText(String.valueOf(CONV_factor));
                    }else if(CONV_factor > 1) {
                        double wt = 0, qty = 0;
                        if(edt_weight.getText().toString().trim() == ""){
                            wt = 0;
                        }else {
                            wt = Double.parseDouble(edt_weight.getText().toString().trim());
                        }
                        qty = wt * CONV_factor;
                        edt_qty.setText(String.valueOf(qty));
                    }
                }
            }
        });

        btnprint.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                //take print out
                if(validate()){

                    //thermalprinetr_teklogic
                 /*   if(BluetoothClass.isPrinterConnected(getApplicationContext(), PIEntryPrintingActivity.this)) {
                        mService = BluetoothClass.getServiceInstance();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            //add data in local table
                            if(isDuplicate()){
                                showalert();
                            }else {
                                viewforprint = v;
                                sendDataToServer();
                            }
                            //printReceipt();        //old
                        }
                }else {
                    BluetoothClass.connectPrinter(getApplicationContext(), PIEntryPrintingActivity.this);
                }*/

                   //label printer_LTK
                  /* if(!isConnected){
                     //  Intent serverIntent = new Intent(PIEntryPrintingActivity.this, Devicelist_LablelPrint.class);
                     //  startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                   }else {
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                           //add data in local table
                           if(isDuplicate()){
                               showalert();
                           }else {
                               viewforprint = v;
                               sendDataToServer();
                           }
                       }
                   }*/

                  //labelprinter_QSPrinter
                    btnprint.setEnabled(false);
                    btnprint.setText("Sending...");
                    btnprint.setAlpha((float) 0.3);

                    if(isDuplicate()){
                        showalert();
                    }else {
                        viewforprint = v;
                        sendDataToServer();
                    }

                }else {

                }
            }
        });
    }

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.refresh){
            //call batchlist API
          //  callBatchListAPI();

            //call locationlist API
            //callLocationListAPI();
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
        return super.onCreateOptionsMenu(menu);
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
        } else {

        }
    }

    @Override
    public void onMessage(int status) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mPrintJobs = mPrintManager.getPrintJobs();
        }

        mPrintCompleteHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mPrintCompleteHandler.postDelayed(this, 2000);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_COMPLETED) {

                        for (int i = 0; i < mPrintJobs.size(); i++) {
                            if (mPrintJobs.get(i).getId() == mCurrentPrintJob.getId()) {
                                mPrintJobs.remove(i);
                            }
                        }

                       // switchConnection();

                        mPrintCompleteHandler.removeCallbacksAndMessages(null);
                    } else if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_FAILED) {
                        Toast.makeText(PIEntryPrintingActivity.this, "Print Failed!", Toast.LENGTH_LONG).show();
                        mPrintCompleteHandler.removeCallbacksAndMessages(null);
                    } else if (mCurrentPrintJob.getInfo().getState() == PrintJobInfo.STATE_CANCELED) {
                        Toast.makeText(PIEntryPrintingActivity.this, "Print Cancelled!", Toast.LENGTH_LONG).show();
                        mPrintCompleteHandler.removeCallbacksAndMessages(null);
                    }
                }

            }
        }, 2000);
    }

    private boolean isnet() {
        // TODO Auto-generated method stub
        Context context = this.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void displayProduct() {

        ItemCodelist.clear();
        ItemDesclist.clear();

        String query = "SELECT distinct ItemCode,ItemMasterId,ItemDesc" +
                " FROM " + db.TABLE_GetItemList;
        Cursor cur = sql.rawQuery(query, null);
        //   lstReferenceType.add("Select");
        if (cur.getCount() > 0) {

            cur.moveToFirst();
            do {
                ItemCodelist.add(cur.getString(cur.getColumnIndex("ItemCode")));
                ItemDesclist.add(cur.getString(cur.getColumnIndex("ItemDesc")));

            } while (cur.moveToNext());

        }
        //Collections.sort(Productionitems,String.CASE_INSENSITIVE_ORDER);
        ArrayAdapter<String> customDept = new ArrayAdapter<String>(PIEntryPrintingActivity.this,
                R.layout.crm_custom_spinner_txt,ItemCodelist);
       // edt_itemcode.setAdapter(customDept);//SF0006_ADATSOFT

        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<String>(PIEntryPrintingActivity.this,
                R.layout.crm_custom_spinner_txt,ItemDesclist);
       // edt_description.setAdapter(stringArrayAdapter);//SF0006

        //Collections.sort(Productionitems, String.CASE_INSENSITIVE_ORDER);

        // Collections.sort(Productionitems, String.CASE_INSENSITIVE_ORDER);
    }

    private void GetItemCode(String itemcode) {
        //String query = "SELECT * FROM " + db.TABLE_GetItemList + " WHERE  ItemCode like '%" + itemcode + "%'";
        String query = "SELECT * FROM " + db.TABLE_GetItemList + " WHERE  ItemCode='"+itemcode+"'";
        Cursor cur = sql.rawQuery(query, null);
        if (cur.getCount() > 0) {
            cur.moveToFirst();
            do {
                edt_weight.setText("");
                edt_qty.setText("");

                String itemcode1=cur.getString(cur.getColumnIndex("ItemDesc"));

                edt_description.setText(itemcode1);
                try{
                    String cnv = cur.getString(cur.getColumnIndex("ConvFactor"));
                    if(cnv.equalsIgnoreCase("null")){
                        CONV_factor = 0;
                    }else {
                        CONV_factor = Double.parseDouble(cnv);
                    }

                    if(CONV_factor == 1){
                        edt_qty.setEnabled(true);
                        edt_qty.setClickable(true);
                        addressLayout.setVisibility(View.GONE);
                        /*addressLayout.setAlpha((float) 0.3);
                        edt_weight.setEnabled(false);
                        edt_weight.setFocusable(false);
                        edt_weight.setClickable(false);*/
                    }else if(CONV_factor > 1) {
                        edt_qty.setEnabled(false);
                        addressLayout.setVisibility(View.VISIBLE);
                       /* addressLayout.setAlpha(1);
                        edt_weight.setEnabled(true);
                        edt_weight.setFocusable(true);
                        edt_weight.setClickable(true);*/
                    }else {
                        edt_qty.setEnabled(true);
                        edt_qty.setClickable(true);
                        addressLayout.setVisibility(View.GONE);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            } while (cur.moveToNext());
        }
    }

    private void GetItemDesc(String itemdesc) {
        //String query = "SELECT * FROM "  + db.TABLE_GetItemList + " WHERE  ItemDesc like '%" + itemdesc + "%'";
        String query = "SELECT * FROM "  + db.TABLE_GetItemList + " WHERE  ItemDesc='"+itemdesc+"'";
        Cursor cur = sql.rawQuery(query, null);
        if (cur.getCount() > 0) {
            cur.moveToFirst();
            do {
                edt_weight.setText("");
                edt_qty.setText("");

                String itemdesc1=cur.getString(cur.getColumnIndex("ItemCode"));
                edt_itemcode.setText(itemdesc1);
                try{
                    CONV_factor = Double.parseDouble(cur.getString(cur.getColumnIndex("ConvFactor")));
                    if(CONV_factor == 1){
                        edt_qty.setEnabled(true);
                        edt_qty.setClickable(true);
                        addressLayout.setVisibility(View.GONE);
                        /*addressLayout.setAlpha((float) 0.3);
                        edt_weight.setEnabled(false);
                        edt_weight.setFocusable(false);
                        edt_weight.setClickable(false);*/
                    }else if(CONV_factor > 1) {
                        edt_qty.setEnabled(false);
                        addressLayout.setVisibility(View.VISIBLE);
                       /* addressLayout.setAlpha(1);
                        edt_weight.setEnabled(true);
                        edt_weight.setFocusable(true);
                        edt_weight.setClickable(true);*/
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }

            } while (cur.moveToNext());


        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg1) {
            switch (msg1.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg1.arg1) {
                        case BluetoothService.STATE_CONNECTED: // ÒÑÁ¬½Ó
                            Toast.makeText(PIEntryPrintingActivity.this, "Connect successful",
                                    Toast.LENGTH_SHORT).show();
                            deviceConnected = true;
                            break;
                        case BluetoothService.STATE_CONNECTING: // ÕýÔÚÁ¬½Ó
                            Log.d("À¶ÑÀµ÷ÊÔ", "ÕýÔÚÁ¬½Ó.....");
                            break;
                        case BluetoothService.STATE_LISTEN: // ¼àÌýÁ¬½ÓµÄµ½À´
                        case BluetoothService.STATE_NONE:
                            Log.d("À¶ÑÀµ÷ÊÔ", "µÈ´ýÁ¬½Ó.....");
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_CONNECTION_LOST: // À¶ÑÀÒÑ¶Ï¿ªÁ¬½Ó
                    Toast.makeText(PIEntryPrintingActivity.this, "Device connection was lost",
                            Toast.LENGTH_SHORT).show();
                    deviceConnected = false;
                    break;
                case BluetoothService.MESSAGE_UNABLE_CONNECT: // ÎÞ·¨Á¬½ÓÉè±¸
                    Toast.makeText(PIEntryPrintingActivity.this, "Unable to connect device",
                            Toast.LENGTH_SHORT).show();
                    deviceConnected = false;
                    break;
            }
        }
    };

    private String callCheckLenght(String value) {
        if (value.length() <= 14) {
            int diff = 14 - value.length();
            if (diff > 0) {
                for (int i = 0; i < diff; i++) {
                    value = " " + value;
                }
            }
        }
        return value;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void getPrintData(int bill_no){
        getDataFromLocal();

        //  for(int b =0; b<5; b++){
        final byte[] ALIGN_LEFT = {0x1B, 0x61, 0};
        final byte[] ALIGN_CENTER = {0x1B, 0x61, 1};
        final byte[] ALIGN_RIGHT = {0x1B, 0x61, 2};
        final byte[] SMALLFONT = {0x1b, 0x21, 0x01}; //small font
        final byte[] DEFAULT = {0x1b, 0x21, 0x00};
        final byte[] NORMAL = new byte[]{0x1B, 0x21, 0x00};  // 0- normal size text

        byte[] format = {27, 33, 0};
        byte[] arrayOfByte1 = {27, 33, 0};
        // Small
        format[2] = ((byte) (0x1 | arrayOfByte1[2]));

        String msg = null, company = "";
        String itemcode = "", itemdesc = "", dateTime = "", username = "", countedby = "", area = "", verifyby = "";
        double weight = 0, qty = 0;

        final Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);

        String yr = String.valueOf(year);

        if(yr.equals("2020")){
            try{
                Date today1 = new Date();

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(today1);

                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);

                Date lastDayOfMonth = calendar.getTime();
                DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                String a = sdf.format(lastDayOfMonth);

                date = a;
                DATE = date;

            }catch (Exception e) {
                e.printStackTrace();

                date = day + "-" + String.format("%02d", (month + 1)) + "-" + year;
                DATE = date;
            }
        }else {
            date = day + "-" + String.format("%02d", (month + 1)) + "-" + year;
            DATE = date;
        }

        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);

        time = updateTime(hour, minute);
        TIME = time;

        itemcode = edt_itemcode.getText().toString().trim();
        itemdesc = edt_description.getText().toString().trim();
        countedby = edt_countedby.getText().toString().trim();
        verifyby = edt_verifyby.getText().toString().trim();
        // area = edt_area.getText().toString().trim();
        area = warehousecode;

        if (CONV_factor == 1 || CONV_factor == 1.0 || CONV_factor == 1.00 || CONV_factor == 1.000 || CONV_factor == 1.0000) {
            weight = 0;
        } else if (CONV_factor > 1 || CONV_factor > 1.0 || CONV_factor > 1.00 || CONV_factor > 1.000 || CONV_factor > 1.0000) {
            weight = Double.parseDouble(edt_weight.getText().toString().trim());
        } else {
            weight = 0;
        }

        int quantity = 0;
        if (CONV_factor == 1 || CONV_factor == 1.0 || CONV_factor == 1.00 || CONV_factor == 1.000 || CONV_factor == 1.0000) {
            qty = Double.parseDouble(edt_qty.getText().toString().trim());

        } else if (CONV_factor > 1 || CONV_factor > 1.0 || CONV_factor > 1.00 || CONV_factor > 1.000 || CONV_factor > 1.0000) {
            qty = Double.parseDouble(String.valueOf(weight * CONV_factor));
        } else {
            qty = Double.parseDouble(edt_qty.getText().toString().trim());
        }

        //Print text on label
        try {
            String productId = billnumber;

            Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            Writer codeWriter;
            codeWriter = new Code128Writer();
            BitMatrix byteMatrix = codeWriter.encode(productId, BarcodeFormat.CODE_128, 300, 50, hintMap);
            int width1 = byteMatrix.getWidth();
            int height1 = byteMatrix.getHeight();
            Log.e("x ", String.valueOf(width1));
            Log.e("y ", String.valueOf(height1));
            Bitmap bitmap = Bitmap.createBitmap(width1, height1, Bitmap.Config.ARGB_8888);
            for (int i = 0; i < width1; i++) {
                for (int j = 0; j < height1; j++) {
                    bitmap.setPixel(i, j, byteMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                }
            }
            //print barcode
            /*mService.write(ALIGN_RIGHT);
            byte[] command = Utils_print.decodeBitmap(bitmap);
            mService.write(command);*/

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        msg = "" + itemcode + "            " + billnumber + "\n";

        if (itemdesc.length() > 40) {
            itemdesc = itemdesc.substring(0, 40);
        } else if (itemdesc.length() <= 40) {
            int diff = 40 - itemdesc.length();
            for (int i = 0; i < diff; i++) {
                itemdesc += " ";
            }
        }

            msg += "" + itemdesc + "\n";
            msg += "Area/Loc   : " + area + "/" + LocationCode + "\n";

        if (CONV_factor == 1 || CONV_factor == 1.0 || CONV_factor == 1.00 || CONV_factor == 1.000 || CONV_factor == 1.0000) {
            //do not show weight
            msg += "Qty        : " + String.format("%.2f",qty)+" "+UOMVAL+ "\n";
            //msg += "Qty        : " + String.valueOf(qty)+" "+UOMVAL+ "\n";
        } else if (CONV_factor > 1 || CONV_factor > 1.0 || CONV_factor > 1.00 || CONV_factor > 1.000 || CONV_factor > 1.0000) {
            msg += "Weight/Qty : " + String.format("%.2f", weight)+" kg" + "/" + String.format("%.2f",qty) +" "+UOMVAL+ "\n";
            //msg += "Weight/Qty : " + String.valueOf(weight)+" kg" + "/" + String.valueOf(qty) +" "+UOMVAL+ "\n";
        } else {
            msg += "Qty        : " + String.format("%.2f",qty)+" "+UOMVAL+ "\n";
           // msg += "Qty        : " + String.valueOf(qty)+" "+UOMVAL+ "\n";
        }

            //msg += "Date Time  : " + date + " " + time + "\n";
            msg += ""+date + " " + time + " "+edt_countedby.getText().toString().trim()+ "\n";
           // msg += "Counted By : " + edt_countedby.getText().toString().trim() + "\n";
            msg += "Verified By: " + UserName + "\n";

        if(!edt_tag_desc.getText().toString().trim().equalsIgnoreCase("") ||
                !edt_tag_desc.getText().toString().trim().equalsIgnoreCase(null) ){
            msg += "Tag Desc   :" + edt_tag_desc.getText().toString().trim() + "\n";
        }else {

        }

       /* if (msg.length() > 0) {
            mService.write(ALIGN_LEFT);
            mService.write(SMALLFONT);
            mService.sendMessage(msg + "\n", "GBK");
        }*/

     //   dataClass_qsPrinter.printLabel_single(msg,billnumber);    //data to print
        //billnumber = "Tag : "+billnumber;
        String areloc = "Area/Loc: "+area+"/"+LocationCode;

        String cntBy =  "Counted By: "+countedby;
        String vrfyBy = "Verify By: "+UserName;
        String tagdesc = edt_tag_desc.getText().toString().trim();

        try{
            if(labelSize.equalsIgnoreCase("2x2mm")){
                dataClass_qsPrinter.printLabel_single(msg, billnumber,itemcode);    //data to print
            }else if(labelSize.equalsIgnoreCase("3x2mm")){

                if(selPrinterName.equalsIgnoreCase("")){
                    dataClass_qsPrinter.printLabel_customsize(billnumber, itemcode, itemdesc.trim(),tagdesc, areloc,
                            String.format("%.2f", weight), String.format("%.2f", qty)+" "+UOMVAL, date+" "+ time, cntBy, vrfyBy);
                }else if(selPrinterName.contains("Qsprinter")){
                    dataClass_qsPrinter.printLabel_customsize(billnumber, itemcode, itemdesc.trim(),tagdesc, areloc,
                            String.format("%.2f", weight), String.format("%.2f", qty)+" "+UOMVAL, date+" "+ time, cntBy, vrfyBy);    //data to print

                }else {
                    dataClass_qsPrinter.printLabel_customsize_HMPrinter(billnumber, itemcode, itemdesc.trim(),tagdesc, areloc,
                            String.format("%.2f", weight), String.format("%.2f", qty)+" "+UOMVAL, date+" "+ time, cntBy, vrfyBy);
                }

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }


        try {
            billNoClass.setBill_no(billNO);

            if(beanTagObj!= null ){
                if(beanTagObj.getBeanTagArrayList() != null){
                    for(int i = 0 ; i<beanTagObj.getBeanTagArrayList().size() ;i++ ){
                        if(batchCODE.equalsIgnoreCase(beanTagObj.getBeanTagArrayList().get(i).getBatchNo())){
                            beanTagObj.getBeanTagArrayList().get(i).setTagNo(billNO);
                        }
                    }
                }
            }

            AppCommon.getInstance(this).setBillNo_print( new Gson().toJson(beanTagObj));

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (AppCommon.getInstance(this).getBillNo_print().equals("")) {

        } else {
            billNoClass = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BillNoClass.class);
            beanTagObj = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BeanTagList.class);
            if(beanTagObj!= null ){
                if(beanTagObj.getBeanTagArrayList() != null){
                    for(BeanTag beanTag : beanTagObj.getBeanTagArrayList() ){
                        if(batchCODE.equalsIgnoreCase(beanTag.getBatchNo())){
                            glBeanObject = beanTag;
                         //   Toast.makeText(this,"Selected tag is "+glBeanObject.getTagNo(), Toast.LENGTH_SHORT).show();

                            String  newTag = "0000" + String.valueOf(glBeanObject.getTagNo()+1);
                            //StringUtils.leftPad("129018", 10, "0");   //padding
                            if (newTag.length() == 5) {
                                newTag = newTag.substring(newTag.length() - 5, 5);
                            } else if (newTag.length() == 6) {
                                newTag = newTag.substring(newTag.length() - 5, 6);
                            } else if (newTag.length() == 7) {
                                newTag = newTag.substring(newTag.length() - 5, 7);
                            } else if (newTag.length() == 8) {
                                newTag = newTag.substring(newTag.length() - 5, 8);
                            }else if (newTag.length() == 9) {
                                newTag = newTag.substring(newTag.length() - 5, 9);
                            }
                            txt_runtag.setText(newTag);
                        }
                    }
                }
            }
        }
        addressLayout.setVisibility(View.VISIBLE);
        edt_weight.setText("");
        edt_weight.setEnabled(true);
        edt_weight.setClickable(true);
        edt_weight.setFocusable(true);
        edt_qty.setText("");
        edt_itemcode.setText("");
        edt_description.setText("");
        edt_area.setText("");
        edt_location.setText("");

        btnprint.setEnabled(true);
        btnprint.setText("PRINT");
        btnprint.setAlpha(1);

      /*  String remark = "Record save successfully-"+billnumber;
        String url = CompanyURL + WebUrlClass.api_PostPIdetail;

        String op = "true";
        CreateOfflineAssignActivity(url, FinalObj, WebUrlClass.POSTFLAG, remark, op);*/
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void printReceipt(int bill_no){
        getDataFromLocal();

            //  for(int b =0; b<5; b++){
            final byte[] ALIGN_LEFT = {0x1B, 0x61, 0};
            final byte[] ALIGN_CENTER = {0x1B, 0x61, 1};
            final byte[] ALIGN_RIGHT = {0x1B, 0x61, 2};
            final byte[] SMALLFONT = {0x1b, 0x21, 0x01}; //small font
            final byte[] DEFAULT = {0x1b, 0x21, 0x00};
            final byte[] NORMAL = new byte[]{0x1B, 0x21, 0x00};  // 0- normal size text

            byte[] format = {27, 33, 0};
            byte[] arrayOfByte1 = {27, 33, 0};
            // Small
            format[2] = ((byte) (0x1 | arrayOfByte1[2]));

            String msg = null, company = "";
            String itemcode = "", itemdesc = "", dateTime = "", username = "", countedby = "", area = "", verifyby = "";
            double weight = 0, qty = 0;

            final Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);

        String yr = String.valueOf(year);

        if(yr.equals("2020")){
            try{
                Date today1 = new Date();

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(today1);

                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DATE, -1);

                Date lastDayOfMonth = calendar.getTime();
                DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                String a = sdf.format(lastDayOfMonth);

                date = a;
                DATE = date;

            }catch (Exception e) {
                e.printStackTrace();

                date = day + "-" + String.format("%02d", (month + 1)) + "-" + year;
                DATE = date;
            }
        }else {
            date = day + "-" + String.format("%02d", (month + 1)) + "-" + year;
            DATE = date;
        }

            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);

            time = updateTime(hour, minute);
            TIME = time;

            itemcode = edt_itemcode.getText().toString().trim();
            itemdesc = edt_description.getText().toString().trim();
            countedby = edt_countedby.getText().toString().trim();
            verifyby = edt_verifyby.getText().toString().trim();
           // area = edt_area.getText().toString().trim();
            area = warehousecode;

            if (CONV_factor == 1) {
                weight = 0;
            } else if (CONV_factor > 1) {
                weight = Double.parseDouble(edt_weight.getText().toString().trim());
            } else {
                weight = 0;
            }

            int quantity = 0;
            if (CONV_factor == 1) {
                qty = Double.parseDouble(edt_qty.getText().toString().trim());

            } else if (CONV_factor > 1) {
                qty = Double.parseDouble(String.valueOf(weight * CONV_factor));
            } else {
                qty = Double.parseDouble(edt_qty.getText().toString().trim());
            }

            //Print text on label
            try {
                String productId = billnumber;

                Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
                hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
                Writer codeWriter;
                codeWriter = new Code128Writer();
                BitMatrix byteMatrix = codeWriter.encode(productId, BarcodeFormat.CODE_128, 300, 50, hintMap);
                int width1 = byteMatrix.getWidth();
                int height1 = byteMatrix.getHeight();
                Log.e("x ", String.valueOf(width1));
                Log.e("y ", String.valueOf(height1));
                Bitmap bitmap = Bitmap.createBitmap(width1, height1, Bitmap.Config.ARGB_8888);
                for (int i = 0; i < width1; i++) {
                    for (int j = 0; j < height1; j++) {
                        bitmap.setPixel(i, j, byteMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
                    }
                }
                mService.write(ALIGN_RIGHT);
                byte[] command = Utils_print.decodeBitmap(bitmap);
                mService.write(command);

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }

           // msg = " No.: " + billnumber + "         " + itemcode + "\n";
             msg = " " + itemcode + "               " + billnumber + "\n";
            //msg += " "+itemcode+"\n";

            if (itemdesc.length() > 40) {
                itemdesc = itemdesc.substring(0, 40);
            } else if (itemdesc.length() <= 40) {
                int diff = 40 - itemdesc.length();
                for (int i = 0; i < diff; i++) {
                    itemdesc += " ";
                }
            }

            msg += " " + itemdesc + "\n";
            //msg += " Loc/Area : " + LocationCode + "/" + area + "\n";
            msg += " Area/Loc : " + area + "/" + LocationCode + "\n";

            if (CONV_factor == 1) {
                //do not show weight
                msg += " Qty  : " + String.format("%.2f",qty) + "\n";
            } else if (CONV_factor > 1) {
                msg += " Weight/Qty : " + String.format("%.2f", weight) + " kg" + "/" + String.format("%.2f",qty) + "\n";
                // msg += "Qty       : "+qty+"\n";
            } else {
                msg += " Qty       : " + String.format("%.2f",qty) + "\n";
            }

            // msg += "Qty       : "+qty+"\n";
            msg += " Date Time : " + date + " " + time + "\n";
            msg += " Counted By : " + edt_countedby.getText().toString().trim() + "\n";
            msg += " Verified By: " + UserName + "\n";

            if (msg.length() > 0) {
                mService.write(ALIGN_LEFT);
                mService.write(SMALLFONT);
                mService.sendMessage(msg + "\n", "GBK");
            }

            try {
                 billNoClass.setBill_no(billNO);

                if(beanTagObj!= null ){
                    if(beanTagObj.getBeanTagArrayList() != null){
                        for(int i = 0 ; i<beanTagObj.getBeanTagArrayList().size() ;i++ ){
                            if(batchCODE.equalsIgnoreCase(beanTagObj.getBeanTagArrayList().get(i).getBatchNo())){
                                beanTagObj.getBeanTagArrayList().get(i).setTagNo(billNO);
                            }
                        }
                    }
                }

                AppCommon.getInstance(this).setBillNo_print( new Gson().toJson(beanTagObj));

            } catch (Exception e) {
                e.printStackTrace();
            }

        if (AppCommon.getInstance(this).getBillNo_print().equals("")) {

        } else {
            billNoClass = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BillNoClass.class);
            beanTagObj = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BeanTagList.class);
            if(beanTagObj!= null ){
                if(beanTagObj.getBeanTagArrayList() != null){
                    for(BeanTag beanTag : beanTagObj.getBeanTagArrayList() ){
                        if(batchCODE.equalsIgnoreCase(beanTag.getBatchNo())){
                            glBeanObject = beanTag;
                         //   Toast.makeText(this,"Selected tag is "+glBeanObject.getTagNo(), Toast.LENGTH_SHORT).show();

                            String  newTag = "0000" + String.valueOf(glBeanObject.getTagNo()+1);
                            //StringUtils.leftPad("129018", 10, "0");   //padding
                            if (newTag.length() == 5) {
                                newTag = newTag.substring(newTag.length() - 5, 5);
                            } else if (newTag.length() == 6) {
                                newTag = newTag.substring(newTag.length() - 5, 6);
                            } else if (newTag.length() == 7) {
                                newTag = newTag.substring(newTag.length() - 5, 7);
                            } else if (newTag.length() == 8) {
                                newTag = newTag.substring(newTag.length() - 5, 8);
                            }else if (newTag.length() == 9) {
                                newTag = newTag.substring(newTag.length() - 5, 9);
                            }
                            txt_runtag.setText(newTag);
                        }
                    }
                }
            }
        }
            addressLayout.setVisibility(View.VISIBLE);
            edt_weight.setText("");
            edt_weight.setEnabled(true);
            edt_weight.setClickable(true);
            edt_weight.setFocusable(true);
            edt_qty.setText("");
            edt_itemcode.setText("");
            edt_description.setText("");
            edt_area.setText("");
            txtuom.setText("");

      /*  String remark = "Record save successfully-"+billnumber;
        String url = CompanyURL + WebUrlClass.api_PostPIdetail;

        String op = "true";
        CreateOfflineAssignActivity(url, FinalObj, WebUrlClass.POSTFLAG, remark, op);*/
    }

    private void getDataFromLocal() {
       // batchCODE =  batchListArrayList.get(spinner_pinumber.getSelectedItemPosition()).getCode();
        beanTagObj = new Gson().fromJson(AppCommon.getInstance(this).getBillNo_print(), BeanTagList.class);
        if(beanTagObj!= null ){
            if(beanTagObj.getBeanTagArrayList() != null){
                for(BeanTag beanTag : beanTagObj.getBeanTagArrayList() ){
                    if(batchCODE.equalsIgnoreCase(beanTag.getBatchNo())){
                        glBeanObject = beanTag;
                    }
                }
            }
        }
    }

    private void sendDataToServer() {
        getDataFromLocal();

        if (Status.equalsIgnoreCase("Offline")) {
            setoffline();
        }


        else {

        int BILL_NO = 0;
        try{
            BILL_NO =  glBeanObject.getTagNo();

          }catch (Exception e){
            e.printStackTrace();
            BILL_NO = 0;
        }

        if(BILL_NO == TagEndNo){
            // stop do not procees
            Toast.makeText(this,"Your batch limit exceeded",Toast.LENGTH_SHORT).show();
        }else {
                //billNO = TagCurrentNo + 1;
            billNO = glBeanObject.getTagNo() + 1;

                billnumber = "0000" + String.valueOf(billNO);
                //StringUtils.leftPad("129018", 10, "0");   //padding
                if (billnumber.length() == 5) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 5);
                } else if (billnumber.length() == 6) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 6);
                } else if (billnumber.length() == 7) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 7);
                } else if (billnumber.length() == 8) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 8);
                }else if (billnumber.length() == 9) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 9);
                }

                String flag = "";
                // Online Code

                UUID uuid = UUID.randomUUID();
                String uuidInString = uuid.toString();

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("PIDtlId", uuidInString);
                    jsonObject.put("PIHdrId", BatchHdID);
                    jsonObject.put("ItemPlantId", ItemPlantId);
                    jsonObject.put("Location", LocationID);
                    String weight=edt_weight.getText().toString();
                    if (weight.equalsIgnoreCase("")){
                        jsonObject.put("Weight", "0.0");
                    }else {
                        jsonObject.put("Weight", edt_weight.getText().toString());
                    }
                    jsonObject.put("ActualQty", edt_qty.getText().toString());
                    jsonObject.put("AddedBy", UserName);
                    jsonObject.put("Printed", "Y");
                    jsonObject.put("TagNo", billnumber);
                    jsonObject.put("Mode", "A");
                    //jsonObject.put("Verifier",edt_verifyby.getText().toString().trim());
                    jsonObject.put("CountedBy",edt_countedby.getText().toString().trim());
                    jsonObject.put("TAGDescription",edt_tag_desc.getText().toString().trim());

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                FinalObj = jsonObject.toString();

                //add data to PI table
                cf.insertPIData(jsonObject, UserName, edt_itemcode.getText().toString().trim(),
                        edt_description.getText().toString().trim(),edt_location.getText().toString().trim());

                callPOSTPIAPI();
            }
        }
    }

    private void callLocationListAPI() {
        if (isnet()) {

            new StartSession(PIEntryPrintingActivity.this, new CallbackInterface() {
                @Override
                public void callMethod() {

                    new DownloadGetLocationData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                @Override
                public void callfailMethod(String msg) {
                    ut.displayToast(getApplicationContext(), msg);
                    progressDialog_1.dismiss();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }

    private void callBatchListAPI() {
        if (isnet()) {
            new StartSession(PIEntryPrintingActivity.this, new CallbackInterface() {
                @Override
                public void callMethod() {

                    new DownloadBatchlistData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                @Override
                public void callfailMethod(String msg) {
                    ut.displayToast(getApplicationContext(), msg);
                    progressDialog_1.dismiss();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }

    private void callPOSTPIAPI() {
        if (isnet()) {
            try {
                if (progressDialog_1 == null) {
                   // progressDialog_1.setMessage("Sending data please wait...");
                    progressDialog_1.setTitle("Sending data please wait...");
                    progressDialog_1.setIndeterminate(false);
                    progressDialog_1.setCancelable(false);

                }
                progressDialog_1.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

            new StartSession(PIEntryPrintingActivity.this, new CallbackInterface() {
                @Override
                public void callMethod() {

                    new PostPIData().execute();
                }

                @Override
                public void callfailMethod(String msg) {
                    ut.displayToast(getApplicationContext(), msg);
                    progressDialog_1.dismiss();
                }
            });

        } else {
            btnprint.setEnabled(true);
            btnprint.setText("PRINT");
            btnprint.setAlpha(1);
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }

    private void callReleaseAPI() {

        if (isnet()) {
            new StartSession(PIEntryPrintingActivity.this, new CallbackInterface() {
                @Override
                public void callMethod() {

                    new DownloadReleaseData().execute();
                }

                @Override
                public void callfailMethod(String msg) {
                    ut.displayToast(getApplicationContext(), msg);
                    progressDialog_1.dismiss();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }

    private void setoffline() {

        Toast.makeText(PIEntryPrintingActivity.this, "You are in offline mode now", Toast.LENGTH_LONG).show();

        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (progressDialog_1 == null) {
                        // progressDialog_1.setMessage("Sending data please wait...");
                        progressDialog_1.setTitle("Saving data please wait...");
                        progressDialog_1.setIndeterminate(false);
                        progressDialog_1.setCancelable(false);

                    }
                    progressDialog_1.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });*/

        if (BatchPrint.equalsIgnoreCase("Immediate")||BatchPrint.equalsIgnoreCase("Batch")) {

            int BILL_NO = 0;
            try {
               // BILL_NO = billNoClass.getBill_no();
                BILL_NO = glBeanObject.getTagNo();
                Log.e("BILL_NO - ", valueOf(BILL_NO));
            } catch (Exception e) {
                e.printStackTrace();
                BILL_NO = 0;
            }

            if (BILL_NO == TagEndNo) {
                // stop do not procees
                Toast.makeText(this, "Your batch limit exceeded", Toast.LENGTH_SHORT).show();
            } else {

                billNO = glBeanObject.getTagNo() + 1;


                billnumber = "0000" + valueOf(billNO);
                //StringUtils.leftPad("129018", 10, "0");   //padding
                if (billnumber.length() == 5) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 5);
                } else if (billnumber.length() == 6) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 6);
                } else if (billnumber.length() == 7) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 7);
                } else if (billnumber.length() == 8) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 8);
                }else if (billnumber.length() == 9) {
                    billnumber = billnumber.substring(billnumber.length() - 5, 9);
                }
            }

            uuid = UUID.randomUUID();
            uuidInString = uuid.toString();
            JSONObject jsonObject = new JSONObject();
            try {

                jsonObject.put("PIDtlId", uuidInString);
                jsonObject.put("PIHdrId", BatchHdID);
                jsonObject.put("ItemPlantId", ItemPlantId);
                jsonObject.put("Location", LocationID);
                String weight=edt_weight.getText().toString();
                if (weight.equalsIgnoreCase("")){
                    jsonObject.put("Weight", "0.0");
                }else {
                    jsonObject.put("Weight", edt_weight.getText().toString());
                }
                jsonObject.put("ActualQty", edt_qty.getText().toString());
                jsonObject.put("AddedBy", UserName);
                jsonObject.put("Printed", "N");
                jsonObject.put("TagNo", billnumber);
                jsonObject.put("Mode", "A");
                jsonObject.put("CountedBy",edt_countedby.getText().toString().trim());
                jsonObject.put("TAGDescription",edt_tag_desc.getText().toString().trim());
                //jsonObject.put("Verifier",edt_verifyby.getText().toString());
                //TAGDescription

            } catch (JSONException e) {
                e.printStackTrace();
            }

            FinalObj = jsonObject.toString();

            //add data to PI table
             cf.insertPIData(jsonObject, UserName,edt_itemcode.getText().toString().trim(),
                     edt_description.getText().toString().trim(),edt_location.getText().toString().trim());

            String remark = "Record save successfully-" + billnumber;
            String url = CompanyURL + WebUrlClass.api_PostPIdetail;

            String op = "true";

            CreateOfflinePrintOfflineActivity(url, FinalObj, WebUrlClass.POSTFLAG, remark, op);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                 printReceipt(billNO);       //teklogic printer

                //LTKPrint(billNO, viewforprint);       //LTKPrinter

                getPrintData(billNO);           //QSPrinter
            }


        }else {
        }

    }

    public boolean chkDuplicate(){
        boolean val = false;
        if(edt_itemcode.getText().toString().equalsIgnoreCase(LocationCode) /*||
            edt_itemcode.getText().toString().equalsIgnoreCase(edt_qty.getText().toString()) ||
            edt_location.getText().toString().equalsIgnoreCase(edt_qty.getText().toString())*/){
            val = false;
            return val;
        }else {
            val = true;
            return val;
        }
    }

    public static String updateTime(int hours, int mins) {
        String timeSet = "";
        if (hours > 12) {
            hours -= 12;
            timeSet = "PM";
        } else if (hours == 0) {
            hours += 12;
            timeSet = "AM";
        } else if (hours == 12)
            timeSet = "PM";
        else
            timeSet = "AM";

        String minutes = "";
        if (mins < 10)
            minutes = "0" + mins;
        else
            minutes = String.valueOf(mins);

        // Append in a StringBuilder
        String aTime = new StringBuilder().append(hours).append(':')
                .append(minutes).append(" ").append(timeSet).toString();

        return aTime;
    }

    private void printerConfiguration() {

        mPrinterConfiguration = Util_Wifi_print.getWifiConfiguration(PIEntryPrintingActivity.this, Constants_wifi.CONTROLLER_PRINTER);

        if (mPrinterConfiguration == null) {
            showWifiListActivity(Constants_wifi.REQUEST_CODE_PRINTER);

        } else {

            boolean isPrinterAvailable = false;

            mWifiManager.startScan();

            for (int i = 0; i < mScanResults.size(); i++) {
                if (mPrinterConfiguration.SSID.equals("\"" + mScanResults.get(i).SSID + "\"")) {
                    isPrinterAvailable = true;
                    break;
                }
            }

            if (isPrinterAvailable) {

                connectToWifi(mPrinterConfiguration);

                doPrint();

            } else {
                showWifiListActivity(Constants_wifi.REQUEST_CODE_PRINTER);
            }

        }
    }

    private void connectToWifi(WifiConfiguration mWifiConfiguration) {
        mWifiManager.enableNetwork(mWifiConfiguration.networkId, true);
    }

    private void showWifiListActivity(int requestCode) {
        Intent iWifi = new Intent(this, WifiListActivity.class);
        startActivityForResult(iWifi, requestCode);
    }

    public void doPrint() {
        mPrintStartHandler.postDelayed(new Runnable() {
            @SuppressLint("LongLogTag")
            @Override
            public void run() {

                Log.d("PrinterConnection Status", "" + mPrinterConfiguration.status);

                mPrintStartHandler.postDelayed(this, 3000);

                if (mPrinterConfiguration.status == WifiConfiguration.Status.CURRENT) {
                   /* if (Util_Wifi_print.computePDFPageCount(pdfFile) > 0) {
                        printDocument(pdfFile);
                       // printReceipt();
                    } else {
                        Toast.makeText(PIEntryPrintingActivity.this, "Can't print, Page count is zero.", Toast.LENGTH_LONG).show();
                    }*/

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                       // printReceipt();
                    }

                    // printDocument();

                    mPrintStartHandler.removeCallbacksAndMessages(null);
                } else if (mPrinterConfiguration.status == WifiConfiguration.Status.DISABLED) {
                    Toast.makeText(PIEntryPrintingActivity.this, "Failed to connect to printer!.", Toast.LENGTH_LONG).show();
                    mPrintStartHandler.removeCallbacksAndMessages(null);
                }
            }
        }, 3000);
    }

    public void printDocument(File pdfFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mPrintManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        }

        String jobName = getString(R.string.app_name) + " Document";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mCurrentPrintJob = mPrintManager.print(jobName, new PrintServicesAdapter(PIEntryPrintingActivity.this, pdfFile), null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private String createPdf1(File sometext){
        // create a new document
        PdfDocument document = new PdfDocument();
        // crate a page description
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        // start a page
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawCircle(50, 50, 30, paint);
        paint.setColor(Color.BLACK);
        canvas.drawText(String.valueOf(sometext), 80, 50, paint);
        //canvas.drawt
        // finish the page
        document.finishPage(page);
// draw text on the graphics object of the page
        // Create Page 2
        pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 2).create();
        page = document.startPage(pageInfo);
        canvas = page.getCanvas();
        paint = new Paint();
        paint.setColor(Color.BLUE);
        canvas.drawCircle(100, 100, 100, paint);
        document.finishPage(page);
        // write the document content
        String directory_path = Environment.getExternalStorageDirectory().getPath() + "/mypdf/";
        File file = new File(directory_path);
        if (!file.exists()) {
            file.mkdirs();
        }
        String targetPdf = directory_path+"test-2.pdf";
        File filePath = new File(targetPdf);
        try {
            document.writeTo(new FileOutputStream(filePath));
            Toast.makeText(this, "Done", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("main", "error "+e.toString());
            Toast.makeText(this, "Something wrong: " + e.toString(),  Toast.LENGTH_LONG).show();
        }
        // close the document
        document.close();

        return String.valueOf(filePath);
    }

    private void getPrevCountedByName() {
        String qry = "Select CountedBy from "+db.TABLE_PI_GENERATION;
        Cursor c = sql.rawQuery(qry,null);
        if(c.getCount() > 0){
            c.moveToLast();
            edt_countedby.setText(c.getString(c.getColumnIndex("CountedBy")));
        }else {
            edt_countedby.setText("");
        }
    }

    private Bitmap createBarcodeBitmap(String data, int width, int height) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        String finalData = Uri.encode(data);

        // Use 1 as the height of the matrix as this is a 1D Barcode.
        BitMatrix bm = writer.encode(finalData, BarcodeFormat.CODE_128, width, 1);
        int bmWidth = bm.getWidth();

        Bitmap imageBitmap = Bitmap.createBitmap(bmWidth, height, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < bmWidth; i++) {
            // Paint columns of width 1
            int[] column = new int[height];
            Arrays.fill(column, bm.get(i, 0) ? Color.BLACK : Color.WHITE);
            imageBitmap.setPixels(column, 0, 1, i, 0, 1, height);
        }

        return imageBitmap;
    }

    Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        String contentsToEncode = contents;
        if (contentsToEncode == null) {
            return null;
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    class DownloadBatchlistData extends AsyncTask<String, Void, String> {
        Object res;
        String response;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                if (progressDialog_1 == null) {
                    progressDialog_1 = new ProgressDialog(PIEntryPrintingActivity.this);
                    progressDialog_1.setMessage("Loading Please wait...");
                    progressDialog_1.setIndeterminate(false);
                    progressDialog_1.setCancelable(false);

                }
                progressDialog_1.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected String doInBackground(String... params) {

            String url = CompanyURL + WebUrlClass.api_GetBatchList;

            try {
                res = ut.OpenConnection(url, PIEntryPrintingActivity.this);
                if (res != null) {
                    response = res.toString();
                    batchListArrayList=new ArrayList<>();
                    batchListArrayList.clear();
                    JSONArray jResults = new JSONArray(response);
                    BeanTagList beanList = new BeanTagList();
                    ArrayList<BeanTag> beanTagArrayList = new ArrayList<>();
                    for (int i = 0; i < jResults.length(); i++) {
                        BatchList batchList = new BatchList();
                        JSONObject jorder = jResults.getJSONObject(i);

                        batchList.setPIHdrId(jorder.getString("PIHdrId"));
                        batchList.setCode(jorder.getString("Code"));
                        batchList.setUsername(jorder.getString("Username"));
                        batchList.setReleaseStatus(jorder.getString("ReleaseStatus"));
                        batchList.setTagStartNo(jorder.getInt("TagStartNo"));
                        batchList.setTagEndNo(jorder.getInt("TagEndNo"));
                        batchList.setTagCurrentNo(jorder.getInt("TagCurrentNo"));
                        batchListArrayList.add(batchList);

                       batchHDR = jorder.getString("PIHdrId");
                       batchCODE = jorder.getString("Code");

                        try {
                            billNoClass.setBill_no(jorder.getInt("TagCurrentNo"));

                            String billingObj = new Gson().toJson(billNoClass);

                            BeanTag beanTag = new BeanTag(batchCODE, batchHDR, jorder.getInt("TagCurrentNo"));
                            beanTagArrayList.add(beanTag);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    beanList.setBeanTagArrayList(beanTagArrayList);
                    String billingObj = new Gson().toJson(beanList);
                    AppCommon.getInstance(PIEntryPrintingActivity.this).setBillNo_print(billingObj);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String integer) {
            super.onPostExecute(integer);

             progressDialog_1.dismiss();

            if (response.contains("[]")) {
                Toast.makeText(PIEntryPrintingActivity.this, "Data not found", Toast.LENGTH_SHORT).show();
            } else {

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                Gson gson = new Gson();

                String json = gson.toJson(batchListArrayList);
                editor.putString("batch", json);
                editor.commit();

                selectedBatchPosition = 0;
                selectedBatchFlag = batchListArrayList.get(0).getCode();
                selectedtchRelease = true;

                editor.putInt("selectedBatchPosition", selectedBatchPosition);
                editor.putString("selectedBatchFlag", selectedBatchFlag);
                editor.putBoolean("selectedtchRelease", selectedtchRelease);
                editor.commit();

                batchListAdapter = new BatchListAdapter(PIEntryPrintingActivity.this, batchListArrayList);
               // spinner_pinumber.setAdapter(batchListAdapter);

              //  spinner_pinumber.setEnabled(true);

                callLocationListAPI();

               /* if (locationListArrayList == null) {
                    callLocationListAPI();
                }else {
                    if (locationListArrayList.size() > 0) {
                        locationListAdapter = new LocationListAdapter(PIEntryPrintingActivity.this, locationListArrayList);
                        spinner_location.setAdapter(locationListAdapter);
                    }
                }*/
            }

        }
    }

    class DownloadGetLocationData extends AsyncTask<String, Void, String> {
        Object res;
        String response;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /*try {
                if (progressDialog_1 == null) {
                    progressDialog_1 = new ProgressDialog(PIEntryPrintingActivity.this);
                    progressDialog_1.setMessage("Loading Please wait...");
                    progressDialog_1.setIndeterminate(false);
                    //  progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    //  progressDialog.setContentView(R.layout.vwb_progress_lay);
                    progressDialog_1.setCancelable(false);

                }
                progressDialog_1.show();
            } catch (Exception e) {
                e.printStackTrace();
            }*/

        }

        @Override
        protected String doInBackground(String... params) {

            String url = CompanyURL + WebUrlClass.api_GetLocation+"?WareHouseMasterId=1";

            try {
                res = ut.OpenConnection(url, PIEntryPrintingActivity.this);
                if (res != null) {
                    response = res.toString();
                    response = res.toString().replaceAll("\\\\", "");
                    response = response.replaceAll("\\\\\\\\/", "");
                    response = response.substring(1, response.length() - 1);
                    locationListArrayList=new ArrayList<>();
                    locationListArrayList.clear();
                    JSONArray jResults = new JSONArray(response);

                    for (int i = 0; i < jResults.length(); i++) {
                        LocationList locationList = new LocationList();
                        JSONObject jorder = jResults.getJSONObject(i);

                        locationList.setLocationMasterId(jorder.getString("LocationMasterId"));
                        locationList.setLocationCode(jorder.getString("LocationCode"));
                        locationListArrayList.add(locationList);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String integer) {
            super.onPostExecute(integer);

          //  progressDialog_1.dismiss();
            if (response.contains("[]")) {
                Toast.makeText(PIEntryPrintingActivity.this, "Data not found", Toast.LENGTH_SHORT).show();
            } else {

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                Gson gson = new Gson();

                String json = gson.toJson(locationListArrayList);
                editor.putString("location", json);
                editor.commit();

                locationListAdapter = new LocationListAdapter(PIEntryPrintingActivity.this, locationListArrayList);
                //spinner_location.setAdapter(locationListAdapter);

            }
        }

    }

    class DownloadReleaseData extends AsyncTask<String, Void, String> {
        Object res;
        String response;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                if (progressDialog_1 == null) {
                    progressDialog_1 = new ProgressDialog(PIEntryPrintingActivity.this);
                    progressDialog_1.setMessage("Loading Please wait...");
                    progressDialog_1.setIndeterminate(false);
                    progressDialog_1.setCancelable(false);
                }
                progressDialog_1.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        protected String doInBackground(String... params) {

            String url = CompanyURL + WebUrlClass.api_UpdateRelaseStatus+"?PIHdrId="+BatchHdID+"&ReleaseStatus=20";

            try {
                res = ut.OpenConnection(url, PIEntryPrintingActivity.this);
                if (res != null) {
                    response = res.toString();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String integer) {
            super.onPostExecute(integer);
            progressDialog_1.dismiss();

            if (response.contains("[]")) {
                Toast.makeText(PIEntryPrintingActivity.this, "Data not found", Toast.LENGTH_SHORT).show();
            } else {

               Toast.makeText(PIEntryPrintingActivity.this,"Batch released successfully",Toast.LENGTH_SHORT);

               batchListArrayList.clear();

                String remark = "Batch released successfully";
                String url = CompanyURL + WebUrlClass.api_PostPIdetail;

                String op = "true";
                CreateOfflineRelease(url, null, WebUrlClass.GETFlAG, remark, op);

                selectedtchRelease = true;

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(PIEntryPrintingActivity.this);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putInt("selectedBatchPosition", selectedBatchPosition);
                editor.putString("selectedBatchFlag", selectedBatchFlag);
                editor.putBoolean("selectedtchRelease", selectedtchRelease);
                editor.commit();

                callBatchListAPI();
            }
        }
    }

    private void CreateOfflinePrintOfflineActivity(final String url, final String parameter,
                                                   final int method, final String remark, final String op) {
        //final DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        long a = cf.addofflinedata(url, parameter, method, remark, op);
        if (a != -1) {
            Toast.makeText(PIEntryPrintingActivity.this, "Record Saved Successfully, will update after getting network!", Toast.LENGTH_LONG).show();

            Intent intent1 = new Intent(getApplicationContext(), SendOfflineData.class);
            intent1.putExtra(WebUrlClass.INTENT_SEND_OFFLINE_DATA_FLAG_KEY,
                    WebUrlClass.INTENT_SEND_OFFLINE_DATA_FLAG_VALUE);
            startService(intent1);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PIEntryPrintingActivity.this,
                            "Record Saved Successfully, will update after getting network!", Toast.LENGTH_LONG).show();
                 //   progressDialog_1.dismiss();
                }
            });

        } else {
            Toast.makeText(PIEntryPrintingActivity.this, "Data not Saved", Toast.LENGTH_LONG).show();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog_1.dismiss();
                    Toast.makeText(PIEntryPrintingActivity.this, "Record not Saved.", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    private void CreateOfflineRelease(final String url, final String parameter,
                                             final int method, final String remark, final String op) {
        //final DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        long a = cf.addofflinedata(url, parameter, method, remark, op);
        if (a != -1) {
            Toast.makeText(getApplicationContext(), "Record Saved Successfully, will update after getting network!", Toast.LENGTH_LONG).show();
            Intent intent1 = new Intent(getApplicationContext(), SendOfflineData.class);
            intent1.putExtra(WebUrlClass.INTENT_SEND_OFFLINE_DATA_FLAG_KEY,
                    WebUrlClass.INTENT_SEND_OFFLINE_DATA_FLAG_VALUE);
            startService(intent1);

        } else {
            Toast.makeText(getApplicationContext(), "Data not Saved", Toast.LENGTH_LONG).show();

        }

    }

    class PostPIData extends AsyncTask<String, Void, String> {
        Object res;
        String response;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                if (progressDialog_1 == null) {
                    progressDialog_1.setMessage("Loading Please wait...");
                    progressDialog_1.setIndeterminate(false);
                    progressDialog_1.setCancelable(false);

                }
                progressDialog_1.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @SuppressLint("WrongThread")
        @Override
        protected String doInBackground(String... params) {

            String url = CompanyURL + WebUrlClass.api_PostPIdetail;

            try {
                res = ut.OpenPostConnection(url,FinalObj, PIEntryPrintingActivity.this);
                if (res != null) {
                    response = res.toString();

                }
            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnprint.setEnabled(true);
                        btnprint.setText("PRINT");
                        btnprint.setAlpha(1);
                    }
                });

            }
            return null;
        }

        @Override
        protected void onPostExecute(String integer) {
            super.onPostExecute(integer);

            progressDialog_1.dismiss();
            if (response.contains("[]")) {
                Toast.makeText(PIEntryPrintingActivity.this, "Data not found", Toast.LENGTH_SHORT).show();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnprint.setEnabled(true);
                        btnprint.setText("PRINT");
                        btnprint.setAlpha(1);
                    }
                });

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                   //printReceipt(billNO);              //Teklogic Printer
                   // LTKPrint(billNO, viewforprint);       //LTK Printer
                    getPrintData(billNO);           //QSPrinter
                }
            }
        }
    }

    public void showalert() {
        // TODO Auto-generated method stub

        final Dialog myDialog = new Dialog(PIEntryPrintingActivity.this);
        myDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        myDialog.setContentView(R.layout.tbuds_dialog_message);
        myDialog.setCancelable(true);

        // myDialog.getWindow().setGravity(Gravity.BOTTOM);
        // myDialog.setTitle("Complete Activity");

        String msg = "Duplicate record found of Part Code - "+edt_itemcode.getText().toString()+" of Location - "+edt_location.getText().toString()
                +" with Quantity - "+edt_qty.getText().toString()+" Counted By - "+duplicateCountedBy+". Do you want to submit same record again";

        final TextView quest = myDialog.findViewById(R.id.textMsg);
        quest.setText(Html.fromHtml(msg));      //details message here

        Button btnyes = myDialog.findViewById(R.id.btn_yes);
        Button btnno = myDialog.findViewById(R.id.btn_no);
        btnno.setVisibility(View.VISIBLE);
        btnyes.setBackgroundColor(Color.parseColor("#016a97"));
        btnno.setBackgroundColor(Color.parseColor("#FD8E2A"));

        btnyes.setText("YES");
        btnyes.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("NewApi")
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //procedd further
                sendDataToServer();
                myDialog.dismiss();
            }
        });

        btnno.setText("NO");
        btnno.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                //cancel submition and cleart edittext
                myDialog.dismiss();
                btnprint.setEnabled(true);
                btnprint.setText("PRINT");
                btnprint.setAlpha(1);
            }
        });

        myDialog.show();
    }

    public boolean validate() {
        boolean val = false;

        if (edt_itemcode.getText().toString().equalsIgnoreCase("")
                && edt_description.getText().toString().equalsIgnoreCase("")
                && edt_location.getText().toString().equalsIgnoreCase("")
                // && edt_area.getText().toString().equalsIgnoreCase("")
                && edt_weight.getText().toString().equalsIgnoreCase("")
                && edt_qty.getText().toString().equalsIgnoreCase("")
            /* && edt_countedby.getText().toString().equalsIgnoreCase("")*/
            /*  && edt_tag_desc.getText().toString().equalsIgnoreCase("")*/) {
            Toast.makeText(PIEntryPrintingActivity.this, "Please fill all details", Toast.LENGTH_SHORT).show();
            val = false;
            return val;
        }else if (CONV_factor > 1) {
            if (edt_weight.getText().toString().equalsIgnoreCase("") ||
                    edt_weight.getText().toString().equalsIgnoreCase(null)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Please enter weight", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else if (edt_qty.getText().toString().equalsIgnoreCase("") ||
                    edt_qty.getText().toString().equalsIgnoreCase(null)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else if (edt_itemcode.getText().toString().equalsIgnoreCase(LocationCode)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Part code and location should not be same.", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else {
                val = true;
                return val;
            }
        } else if (CONV_factor == 1) {
            if (edt_qty.getText().toString().equalsIgnoreCase("") ||
                    edt_qty.getText().toString().equalsIgnoreCase(null)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else if (edt_itemcode.getText().toString().equalsIgnoreCase(LocationCode)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Part code and location should not be same.", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else {
                val = true;
                return val;
            }
        }else {
            if (edt_qty.getText().toString().equalsIgnoreCase("") ||
                    edt_qty.getText().toString().equalsIgnoreCase(null)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Please enter quantity", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else if (edt_itemcode.getText().toString().equalsIgnoreCase(LocationCode)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Part code and location should not be same.", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            }else if (edt_location.getText().toString().equalsIgnoreCase("") ||
                    edt_location.getText().toString().equalsIgnoreCase(null)) {
                Toast.makeText(PIEntryPrintingActivity.this, "Please select location", Toast.LENGTH_SHORT).show();
                val = false;
                return val;
            } else {
                val = true;
                return val;
            }
        }
    }

    private boolean isDuplicate() {
        boolean val = false;

        float qty_new = 0.0f, qty_old = 0.0f;

        String newItemcode = edt_itemcode.getText().toString().trim();
        String newLocCode = edt_location.getText().toString().trim();
        qty_new = Float.parseFloat(edt_qty.getText().toString());
        String newQty = String.format("%.2f",qty_new);

        String oldItemcode = "", oldLocCode ="", oldQty = "";
        //check current filled details and records from table are same or not
        String qry = "Select ItemCode, LocationCode, ActualQty, CountedBy from "+db.TABLE_PI_GENERATION;
        Cursor c = sql.rawQuery(qry,null);
        if(c.getCount() > 0){
            c.moveToFirst();
            do{
                oldItemcode = c.getString(c.getColumnIndex("ItemCode"));
                oldLocCode = c.getString(c.getColumnIndex("LocationCode"));
                qty_old = Float.parseFloat(c.getString(c.getColumnIndex("ActualQty")));
                oldQty = String.format("%.2f",qty_old);
                duplicateCountedBy = c.getString(c.getColumnIndex("CountedBy"));

                if(newItemcode.equalsIgnoreCase(oldItemcode) && newLocCode.equalsIgnoreCase(oldLocCode) && newQty.equalsIgnoreCase(oldQty)){
                    //duplicate found show alert dialogue
                    val = true;
                    return val;

                }else {
                  // no duplicates
                    //val = false;
                }

            }while (c.moveToNext());
        }
        return val;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.e("Scan*******", "Cancelled scan");

            } else {
                Log.e("Scan", "Scanned");

                edt_itemcode.setText(result.getContents());
                GetItemCode(result.getContents());
                //Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            BluetoothClass.pairPrinter(getApplicationContext(), PIEntryPrintingActivity.this);
            //isConnected = true;

        }else if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == RESULT_OK) {
            String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
           // isConnected = true;

            BluetoothClass.pairedPrinterAddress(getApplicationContext(), PIEntryPrintingActivity.this,address);

            /*LTK*/
            /*if (isConnected & (printPP_cpcl != null)) {
                printPP_cpcl.disconnect();
                isConnected = false;
            }

            String sdata = data.getExtras().getString(Devicelist_LablelPrint.EXTRA_DEVICE_ADDRESS);
            String address = sdata.substring(sdata.length() - 17);
            String name = sdata.substring(0, (sdata.length() - 17));

            if (!isConnected) {
                if(printPP_cpcl.connect(name,address)) {
                    isConnected = true;
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(name);
                    Toast.makeText(this,"Connect Successful",Toast.LENGTH_SHORT).show();
                } else {
                    isConnected = false;
                }
            }*/

        }else if (requestCode == Constants_wifi.REQUEST_CODE_PRINTER && resultCode == Constants_wifi.RESULT_CODE_PRINTER) {
            mPrinterConfiguration = Util_Wifi_print.getWifiConfiguration(PIEntryPrintingActivity.this, Constants_wifi.CONTROLLER_PRINTER);
            doPrint();
        }else if(requestCode == REQ_PARTCODE && resultCode == REQ_PARTCODE){
            edt_itemcode.setText(data.getStringExtra("PartCode"));
            edt_description.setText(data.getStringExtra("PartName"));
            ItemPlantId=data.getStringExtra("ItemPlantId");
            CONV_factor = Double.parseDouble(data.getStringExtra("ConvFactor"));
            LocationID = data.getStringExtra("LocationMasterID");
            LocationCode = data.getStringExtra("LocationCode");
            edt_location.setText(LocationCode);
            UOMVAL = data.getStringExtra("uomval");
            txtuom.setText(UOMVAL);
            //Toast.makeText(this, "getting back frm ocr",Toast.LENGTH_SHORT).show();

            if(CONV_factor == 1 || CONV_factor == 1.0 || CONV_factor == 1.00 || CONV_factor == 1.000 || CONV_factor == 1.0000){

                edt_qty.setEnabled(true);
                edt_qty.setClickable(true);
                addressLayout.setVisibility(View.GONE);
                addressLayout.setAlpha((float) 0.3);
                edt_weight.setEnabled(false);
                edt_weight.setFocusable(false);
                edt_weight.setClickable(false);

            }else if(CONV_factor > 1 ||CONV_factor > 1.0 || CONV_factor > 1.00 || CONV_factor > 1.000 || CONV_factor > 1.0000) {
                edt_qty.setEnabled(false);
                addressLayout.setVisibility(View.VISIBLE);
                addressLayout.setAlpha(1);
                edt_weight.setEnabled(true);
                edt_weight.setFocusable(true);
                edt_weight.setClickable(true);
            }else {

                edt_qty.setEnabled(true);
                edt_qty.setClickable(true);
                addressLayout.setVisibility(View.GONE);
            }

        }else if(requestCode == REQ_PARTNAME && resultCode == REQ_PARTNAME){
            edt_itemcode.setText(data.getStringExtra("PartCode"));
            edt_description.setText(data.getStringExtra("PartName"));
            ItemPlantId=data.getStringExtra("ItemPlantId");
            CONV_factor = Double.parseDouble(data.getStringExtra("ConvFactor"));
            LocationID = data.getStringExtra("LocationMasterID");
            LocationCode = data.getStringExtra("LocationCode");
            edt_location.setText(LocationCode);
            UOMVAL = data.getStringExtra("uomval");
            txtuom.setText(UOMVAL);

            if(CONV_factor == 1){
                edt_qty.setEnabled(true);
                edt_qty.setClickable(true);
                addressLayout.setVisibility(View.GONE);
                edt_weight.setEnabled(false);
                edt_weight.setFocusable(false);
                edt_weight.setClickable(false);
            }else if(CONV_factor > 1) {
                edt_qty.setEnabled(false);
                addressLayout.setVisibility(View.VISIBLE);
                edt_weight.setEnabled(true);
                edt_weight.setFocusable(true);
                edt_weight.setClickable(true);
            }
        }else if(requestCode == REQ_LOCATION && resultCode == REQ_LOCATION){
            LocationCode = data.getStringExtra("LocationCode");
            LocationID = data.getStringExtra("LocationMasterID");
            edt_location.setText(LocationCode);

        }
    }

    /*label printer print class*/

    public abstract class PrintLabel {
        private Context context;
        private String itemcode;
        private String itemdesc;
        private String area;
        private String Location;
        private String qtyprint;
        private String wtprint;
        private String datetime;
        private String produce_company;
        private int barcode;
        private String desc;
        private String desc1;
        private String desc2;
        private String countedBy, verifyBy;

        public PrintLabel(Context context) {
            this.context = context;
        }

        public abstract void onReceived(boolean successed);


       /* public void label2(PrintPP_CPCL iPrinter, Bitmap rawBitmap) {
            int height = rawBitmap.getHeight();
            iPrinter.pageSetup(576, height + 70);
            iPrinter.drawGraphic2(0, 0, rawBitmap.getWidth(), rawBitmap.getHeight(), rawBitmap);
            iPrinter.print(0, 0);
        }*/

        public void label1(PrintPP_CPCL iPrinter, View view, final int billNO) {
            int x = 10;
            int y = 10;
            int space =40;
            initValue(billNO);
            iPrinter.pageSetup(576, 480);

            iPrinter.drawLine(2, 0, 0, 574, 0, false);

            //iPrinter.drawText(x, y += 10, getSringbyID(view, R.string.number_label), 2, 0, 0, false, false);
            iPrinter.drawText(x + 10, y, itemcode, 3, 0, 1, false, false);

            iPrinter.drawText(x, y += space, getSringbyID(view, R.string.tag_desc), 2, 0, 0, false, false);
            iPrinter.drawText(x + 2, y,  itemdesc, 2, 0, 0, false, false);

            if(!edt_tag_desc.getText().toString().equalsIgnoreCase("") ||
                    !edt_tag_desc.getText().toString().equalsIgnoreCase(null)){
                if(edt_tag_desc.getText().toString().length() > 36){
                    iPrinter.drawText(x, y += space, getSringbyID(view, R.string.desc), 2, 0, 0, false, false);
                    iPrinter.drawText(x + 2, y, desc, 2, 0, 0, false, false);
                    iPrinter.drawText(x, y += space, getSringbyID(view, R.string.desc), 2, 0, 0, false, false);
                    iPrinter.drawText(x + 2, y, desc1, 2, 0, 0, false, false);
                }else {
                    iPrinter.drawText(x, y += space, getSringbyID(view, R.string.desc), 2, 0, 0, false, false);
                    iPrinter.drawText(x + 2, y, desc, 2, 0, 0, false, false);
                }
            }else {
                // do not print
            }

            iPrinter.drawText(x, y += space, getSringbyID(view, R.string.area), 2, 0, 0, false, false);
            iPrinter.drawText(x + 80, y, area, 2, 0, 0, false, false);
            iPrinter.drawText(x + 190, y, getSringbyID(view, R.string.loc), 2, 0, 0, false, false);
            iPrinter.drawText(x + 250, y, Location, 2, 0, 0, false, false);

            iPrinter.drawLine(2, 0, y += (space + 10), 574, y, false);

            if (CONV_factor == 1) {
                //do not show weight
                iPrinter.drawText(x, y += 10, getSringbyID(view, R.string.qty), 2, 0, 0, false, false);
                iPrinter.drawText(x + 50, y, qtyprint, 3, 0, 1, false, false);
            } else if (CONV_factor > 1) {
                iPrinter.drawText(x, y += 10, getSringbyID(view, R.string.qty), 2, 0, 0, false, false);
                iPrinter.drawText(x + 50, y, qtyprint, 3, 0, 1, false, false);

                iPrinter.drawText(x + 150, y, getSringbyID(view, R.string.weight), 2, 0, 0, false, false);
                iPrinter.drawText(x + 250, y, wtprint, 2, 0, 0, false, false);
            } else {
                iPrinter.drawText(x, y += 10, getSringbyID(view, R.string.qty), 2, 0, 0, false, false);
                iPrinter.drawText(x + 50, y, qtyprint, 3, 0, 1, false, false);
            }

            // iPrinter.drawText(x, y += space, getSringbyID(view, R.string.produce_date), 2, 0, 0, false, false);
            // iPrinter.drawText(x + 150, y, produce_date, 2, 0, 0, false, false);

         /*   iPrinter.drawText(x + 280, y, 105, 80, getSringbyID(view, R.string.produce_company), 2, 0, 0, false, false);
            iPrinter.drawText(x + 370, y, 210, 80, produce_company, 2, 0, 0, false, false);
*/
            //  iPrinter.drawText(x, (y += space) + 35, getSringbyID(view, R.string.barcode), 3, 0, 1, false, false);

            iPrinter.drawText(x, (y += space) + 5, countedBy, 2, 0, 0, false, false);
            iPrinter.drawText(x, (y += space) + 10, verifyBy, 2, 0, 0, false, false);


            iPrinter.drawText(x, (y += space) + 10, billnumber, 3, 0, 1, false, false);
            iPrinter.drawBarCode(x + 120, y + 5, billnumber, 1, 0, 3, 56);
            iPrinter.drawBarCode(x + 120, y + 5,billnumber, 1, 0, 3, 56);

            iPrinter.drawText(x, (y += space) +35, datetime, 2, 0, 0, false, false);

//            iPrinter.drawText(x + 150, y, produce_date, 2, 0, 0, false, false);
            //iPrinter.drawText(x + 150, y, produce_date, 2, 0, 0, false, false);


            iPrinter.drawLine(2, 0, y += (space + 50), 574, y, false);
            iPrinter.drawLine(2, 0, 0, 0, y, false);
            iPrinter.drawLine(2, 574, 0, 574, y, false);
            iPrinter.print(0, 0);

            PIEntryPrintingActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        billNoClass.setBill_no(billNO);
                        Log.e("Tag aftr print - ", String.valueOf(billNoClass.getBill_no()));

                        if(beanTagObj!= null ){
                            if(beanTagObj.getBeanTagArrayList() != null){
                                for(int i = 0 ; i<beanTagObj.getBeanTagArrayList().size() ;i++ ){
                                    if(batchCODE.equalsIgnoreCase(beanTagObj.getBeanTagArrayList().get(i).getBatchNo())){
                                        beanTagObj.getBeanTagArrayList().get(i).setTagNo(billNO);
                                    }
                                }
                            }
                        }
                        AppCommon.getInstance(PIEntryPrintingActivity.this).setBillNo_print( new Gson().toJson(beanTagObj));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (AppCommon.getInstance(PIEntryPrintingActivity.this).getBillNo_print().equals("")) {

                    } else {
                        billNoClass = new Gson().fromJson(AppCommon.getInstance(PIEntryPrintingActivity.this).getBillNo_print(), BillNoClass.class);
                        beanTagObj = new Gson().fromJson(AppCommon.getInstance(PIEntryPrintingActivity.this).getBillNo_print(), BeanTagList.class);
                        if(beanTagObj!= null ){
                            if(beanTagObj.getBeanTagArrayList() != null){
                                for(BeanTag beanTag : beanTagObj.getBeanTagArrayList() ){
                                    if(batchCODE.equalsIgnoreCase(beanTag.getBatchNo())){
                                        glBeanObject = beanTag;
                                        //    Toast.makeText(this,"Selected tag is "+glBeanObject.getTagNo(), Toast.LENGTH_SHORT).show();

                                        String  newTag = "0000" + String.valueOf(glBeanObject.getTagNo()+1);
                                        //StringUtils.leftPad("129018", 10, "0");   //padding
                                        if (newTag.length() == 5) {
                                            newTag = newTag.substring(newTag.length() - 5, 5);
                                        } else if (newTag.length() == 6) {
                                            newTag = newTag.substring(newTag.length() - 5, 6);
                                        } else if (newTag.length() == 7) {
                                            newTag = newTag.substring(newTag.length() - 5, 7);
                                        } else if (newTag.length() == 8) {
                                            newTag = newTag.substring(newTag.length() - 5, 8);
                                        }else if (newTag.length() == 9) {
                                            newTag = newTag.substring(newTag.length() - 5, 9);
                                        }
                                        txt_runtag.setText(newTag);
                                    }
                                }
                            }
                        }
                    }

                    addressLayout.setVisibility(View.VISIBLE);
                    edt_weight.setText("");
                    edt_weight.setEnabled(true);
                    edt_weight.setClickable(true);
                    edt_weight.setFocusable(true);
                    edt_qty.setText("");
                    edt_itemcode.setText("");
                    edt_description.setText("");
                    edt_area.setText("");
                }
            });

        }

        public void readThread(final PrintPP_CPCL iPrinter) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int times = 0;
                    boolean flag = true;
                    while (flag) {
                        byte[] temp = iPrinter.readPrintResult();
                        times++;
                        if (times >= 50) {
                            flag = false;
                            onReceived(false);
                            break;
                        }
                        if (temp != null) {
                            Log.e("get" + String.valueOf(times), String.valueOf(ByteArrToHex(temp)));
                            if (temp[0] == 0x4F) {
                                flag = false;
                                onReceived(true);
                                break;
                            }
                            if (temp[0] == 0x45) {
                                flag = false;
                                onReceived(false);
                                break;
                            }

                        }

                    }

                }
            }).start();
        }

        private void initValue(int billNO) {

            getDataFromLocal();

            double weight = 0, qty = 0;

            final Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);

            String yr = String.valueOf(year);

            if(yr.equals("2020")){
                try{
                    Date today1 = new Date();

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(today1);

                    calendar.add(Calendar.MONTH, 1);
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.add(Calendar.DATE, -1);

                    Date lastDayOfMonth = calendar.getTime();
                    DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

                    String a = sdf.format(lastDayOfMonth);

                    date = a;
                    DATE = date;

                }catch (Exception e) {
                    e.printStackTrace();

                    date = day + "-" + String.format("%02d", (month + 1)) + "-" + year;
                    DATE = date;
                }
            }else {
                date = day + "-" + String.format("%02d", (month + 1)) + "-" + year;
                DATE = date;
            }

            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);

            time = updateTime(hour, minute);
            TIME = time;

            itemcode = edt_itemcode.getText().toString().trim();
            itemdesc = edt_description.getText().toString().trim();
            countedBy = "Counted By : "+edt_countedby.getText().toString().trim();
            verifyBy = "Verify By : "+edt_verifyby.getText().toString().trim();
            // area = edt_area.getText().toString().trim();
            area = warehousecode;

            if (CONV_factor == 1) {
                weight = 0;
            } else if (CONV_factor > 1) {
                weight = Double.parseDouble(edt_weight.getText().toString().trim());
            } else {
                weight = 0;
            }

            int quantity = 0;
            if (CONV_factor == 1) {
                qty = Double.parseDouble(edt_qty.getText().toString().trim());

            } else if (CONV_factor > 1) {
                qty = Double.parseDouble(String.valueOf(weight * CONV_factor));
            } else {
                qty = Double.parseDouble(edt_qty.getText().toString().trim());
            }

            //itemcode = "V5002891";
            //itemdesc = "(TILT METER) SUPPORT";
           // area= "Pune";
            Location = LocationCode;
            qtyprint = String.valueOf(qty);
            wtprint = String.valueOf(weight);
            datetime = date + " " + time;
            produce_company = "Vritti";
           // billnumber= String.valueOf(billNO);

            if(edt_tag_desc.getText().toString().length() > 36){
                desc = edt_tag_desc.getText().toString().trim().substring(36);
                desc1 = edt_tag_desc.getText().toString().trim().substring(37,edt_tag_desc.getText().toString().length());

            }else if(edt_tag_desc.getText().toString().length() < 36){
                desc = edt_tag_desc.getText().toString().trim();
            }
          //  desc = edt_tag_desc.getText().toString().trim().substring(36);

        }

        private String getSringbyID(View view, int id) {
            return view.getContext().getResources().getString(id);
        }

        private String Byte2Hex(Byte inByte) {
            return String.format("%02x", inByte).toUpperCase();
        }

        private String ByteArrToHex(byte[] inBytArr) {
            StringBuilder strBuilder = new StringBuilder();
            int j = inBytArr.length;
            for (int i = 0; i < j; i++) {
                strBuilder.append(Byte2Hex(inBytArr[i]));
                strBuilder.append(" ");
            }
            return strBuilder.toString();
        }
    }

    public void LTKPrint(int billNO, View viewforprint){
        /*  if (!isSending) {
                      //  showprogress();
                        pl = new PrintLabel(PIEntryPrintingActivity.this) {
                            @Override
                            public void onReceived(boolean successed) {
                            //    closeProgress();
                            }
                        };
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                isSending = true;
                                if (isConnected) {
                                    pl.label1(printPP_cpcl, viewforprint, billNO);
                                    pl.readThread(printPP_cpcl);
                                }

                                try {
                                    interval = 0;
                                    Thread.sleep(interval);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                isSending = false;
                            }
                        }).start();
                    }*/
    }

   /* @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled
        // setupChat() will then be called during onActivityRe//sultsetupChat

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }*/

}
