package com.example.ryo.blescanner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Handler;

import static android.R.attr.delay;
import static android.R.attr.textViewStyle;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String BEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String UUID = "AD3AC312-E845-4A35-89C9-48E3000BA810";

    private BeaconManager beaconManager;
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    private Vibrator vib;

    private TextView tv;
    private TextView tvArea1, tvArea2, tvArea3, tvArea4;
    private TextView tvNumber1, tvNumber2, tvNumber3, tvNumber4;
    private TextView tvDistance1, tvDistance2, tvDistance3, tvDistance4;
    private TextView tvBattery1, tvBattery2, tvBattery3, tvBattery4;

    private String batteryFlag = "", stringTemp="", text="";
    private String[] textArea = new String[20];
    private String[] textNumber = new String[20];
    private String[] textBattery = new String[20];
    private double[] textDistance = new double[20];
    private String[] area = {
        "不明", "ホワイティうめだ", "ディアモール大阪", "阪急電鉄", "阪神電鉄", "大阪市交通局", "⻄日本旅客鉄道", "ドージマ地下センター"
    };

    private int major, minor, beaconArea=0;
    private double distance, doubleTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter Bt = BluetoothAdapter.getDefaultAdapter();
        boolean btEnable = Bt.isEnabled();
        if(btEnable == true){
            //BluetoothがONだった場合の処理
        }else{
            //OFFだった場合、ONにすることを促すダイアログを表示する画面に遷移
            Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btOn, REQUEST_ENABLE_BLUETOOTH);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},1);
            }
        }
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_FORMAT));
        beaconManager.bind(this);

        vib = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        tv = (TextView) findViewById(R.id.textView);;
        tvArea1 = (TextView) findViewById(R.id.areaView1);
        tvArea2 = (TextView) findViewById(R.id.areaView2);
        tvArea3 = (TextView) findViewById(R.id.areaView3);
        tvArea4 = (TextView) findViewById(R.id.areaView4);
        tvNumber1 = (TextView) findViewById(R.id.numberView1);
        tvNumber2 = (TextView) findViewById(R.id.numberView2);
        tvNumber3 = (TextView) findViewById(R.id.numberView3);
        tvNumber4 = (TextView) findViewById(R.id.numberView4);
        tvDistance1 = (TextView) findViewById(R.id.distanceView1);
        tvDistance2 = (TextView) findViewById(R.id.distanceView2);
        tvDistance3 = (TextView) findViewById(R.id.distanceView3);
        tvDistance4 = (TextView) findViewById(R.id.distanceView4);
        tvBattery1 = (TextView) findViewById(R.id.batteryView1);
        tvBattery2 = (TextView) findViewById(R.id.batteryView2);
        tvBattery3 = (TextView) findViewById(R.id.batteryView3);
        tvBattery4 = (TextView) findViewById(R.id.batteryView4);

        tv.setText("周囲のBeaconを検索中です\n動かない場合はアプリを再起動してください");

    }

    @Override
    protected void onActivityResult(int requestCode, int ResultCode, Intent date){
        if(requestCode == REQUEST_ENABLE_BLUETOOTH){
            if(ResultCode == Activity.RESULT_OK){
                Log.d("BeaconInfo", "BluetoothをONにしてもらえました");
            }else{
                Log.d("BeaconInfo", "BluetoothがONにしてもらえませんでした");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // サービスの開始
        beaconManager.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // サービスの停止
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        final Identifier uuid = Identifier.parse(UUID);
        Region mRegion = new Region("Beacon", uuid, null, null);

        // BeaconManagerクラスのモニタリング設定
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.d("BeaconArea", "Beacon領域侵入");
                beaconArea = 1;
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText("Beaconのエリアに入りました");
                    }
                });
                try{
                    vib.vibrate(1000);
                    Thread.sleep(2000);
                }catch(InterruptedException e){}
                try {
                    // レンジングの開始
                    beaconManager.startRangingBeaconsInRegion(new Region("Beacon", uuid, null, null));
                } catch(RemoteException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void didExitRegion(Region region) {
                Log.d("BeaconArea", "Beacon領域退出");
                beaconArea = 0;
                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText("Beaconのエリアから出ました");
                    }
                });
                try{
                    vib.vibrate(1000);
                    Thread.sleep(2000);
                }catch(InterruptedException e){}
                try {
                    // レンジングの停止
                    beaconManager.stopRangingBeaconsInRegion(new Region("Beacon", uuid, null, null));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                // 領域に対する状態が変化
            }
        });

        // BeaconManagerクラスのレンジング設定
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                int a=0;

                for(Beacon beacon : beacons) {

                    batteryFlag = "No";

                    major = beacon.getId2().toInt();
                    minor = beacon.getId3().toInt();
                    distance = beacon.getDistance();

                    // エラー (major > 800A or 000A <= major <= 8002)
                    if ((major >= 32778) || (10 <= major && major <= 32770)){
                        major = 2;
                    }
                    // 電池交換必要 (8003 <= major <= 8009) 8000 = 32768
                    else if (32771 <= major && major <= 32777){
                        major -= 32768;
                        batteryFlag = "必要";
                    }

                    textArea[a] = area[major-2];
                    textNumber[a] = major + "-" + String.valueOf(minor);
                    textDistance[a] = Double.parseDouble(String.format("%.2f", distance));
                    textBattery[a] = batteryFlag;

                    Log.d("BeaconInfo", "エリア:" + area[major-2] + "  Beacon No:" + textNumber[a] + "  major:" + major + "  minor:" + minor + "  距離:" + distance + "  電池フラグ:" + textBattery[a]);

                    a++;
                    if (a >= 19) break;

                }

                if(a >= 1){
                    for(int i=0; i<a-1; i++){
                        for(int j=a-1; j>i; j--){
                            if(textDistance[j] < textDistance[j-1]){

                                stringTemp = textArea[j];
                                textArea[j] = textArea[j-1];
                                textArea[j-1] = stringTemp;

                                stringTemp = textNumber[j];
                                textNumber[j] = textNumber[j-1];
                                textNumber[j-1] = stringTemp;

                                doubleTemp = textDistance[j];
                                textDistance[j] = textDistance[j-1];
                                textDistance[j-1] = doubleTemp;

                                stringTemp = textBattery[j];
                                textBattery[j] = textBattery[j-1];
                                textBattery[j-1] = stringTemp;

                            }
                        }
                    }
                }

                if (a < 4){
                    for(int i=4; i>=a; i--){
                        textArea[i] = "";
                        textNumber[i] = "";
                        textDistance[i] = 0;
                        textBattery[i] = "";
                    }
                }

                if(a >= 1){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tv.setText("Beaconのエリア内にいます");
                        }
                    });
                }
                if(a==0 && beaconArea == 0){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tv.setText("周囲のBeaconを検索中です\n動かない場合はアプリを再起動してください");
                        }
                    });
                }

                // 別スレッドで読み込む
                runOnUiThread(new Runnable() {
                    public void run() {
                        tvArea1.setText(textArea[0]);
                        tvArea2.setText(textArea[1]);
                        tvArea3.setText(textArea[2]);
                        tvArea4.setText(textArea[3]);
                        tvNumber1.setText(textNumber[0]);
                        tvNumber2.setText(textNumber[1]);
                        tvNumber3.setText(textNumber[2]);
                        tvNumber4.setText(textNumber[3]);
                        tvDistance1.setText(String.valueOf(textDistance[0]));
                        tvDistance2.setText(String.valueOf(textDistance[1]));
                        tvDistance3.setText(String.valueOf(textDistance[2]));
                        tvDistance4.setText(String.valueOf(textDistance[3]));
                        tvBattery1.setText(textBattery[0]);
                        tvBattery2.setText(textBattery[1]);
                        tvBattery3.setText(textBattery[2]);
                        tvBattery4.setText(textBattery[3]);
                        try{
                            Thread.sleep(1000);
                        }catch(InterruptedException e){}
                    }
                });

            }
        });

        try {
            // モニタリングの開始
            beaconManager.startMonitoringBeaconsInRegion(mRegion);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}

