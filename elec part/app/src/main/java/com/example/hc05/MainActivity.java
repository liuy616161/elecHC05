package com.example.hc05;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // 常量
    private static final int REQ_PERMISSION_CODE = 1;
    // 实例化蓝牙控制器
    public com.example.hc05.BlueToothController btController = new BlueToothController();
    // 弹窗
    private Toast mToast;
    // 蓝牙权限列表
    public ArrayList<String> requestList = new ArrayList<>();
    // 搜索蓝牙广播
    private IntentFilter foundFilter;
    //
    public ArrayAdapter adapter1;
    //定义一个列表，存蓝牙设备的地址。
    public ArrayList<String> arrayList=new ArrayList<>();
    //定义一个列表，存蓝牙设备地址，用于显示。
    public ArrayList<String> deviceName=new ArrayList<>();
    // 蓝牙状态改变广播
    private BroadcastReceiver receiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            switch (state){
                case BluetoothAdapter.STATE_OFF:
                    showToast("STATE_OFF");
                    break;
                case BluetoothAdapter.STATE_ON:
                    showToast("STATE_ON");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    showToast("STATE_TURNING_OFF");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    showToast("STATE_TURNING_ON");
                    break;
                default:
                    showToast("UnKnow STATE");
                    unregisterReceiver(this);
                    break;
            }
        }
    };

    // 搜索蓝牙广播
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                String s;
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == 12) {
                    s = "设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：已配对" + "\n";
                }
                else if (device.getBondState() == 10){
                    s = "设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未配对" +"\n";
                }else{
                    s = "设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未知" + "\n";
                }
                if (!deviceName.contains(s)) {
                    deviceName.add(s);//将搜索到的蓝牙名称和地址添加到列表。
                    arrayList.add(device.getAddress());//将搜索到的蓝牙地址添加到列表。
                    adapter1.notifyDataSetChanged();//更新
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                showToast("搜索结束");
                unregisterReceiver(this);
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                showToast("开始搜索");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 蓝牙状态改变信息
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        // 注册广播
        registerReceiver(receiver, filter);
        //搜索蓝牙的广播
        foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        foundFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        foundFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // 获取ListView组件
        ListView listView = (ListView) findViewById(R.id.listview1);
        // 实例化ArrayAdapter对象
        adapter1 = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, deviceName);
        // 添加到ListView组件中
        listView.setAdapter(adapter1);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CharSequence content = ((TextView) view).getText();
                String con = content.toString();
                String[] conArray = con.split("\n");
                String rightStr = conArray[1].substring(5, conArray[1].length());
                BluetoothDevice device = btController.find_device(rightStr);
                if (device.getBondState() == 10) {
                    btController.cancelSearch();
                    String s = "设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未配对"  + "\n";
                    deviceName.remove(s);
                    device.createBond();
                    s = "设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：已配对"  + "\n";
                    deviceName.add(s);
                    adapter1.notifyDataSetChanged();
                    showToast("配对：" + device.getName());
                }
                else{
                    btController.cancelSearch();
                    String s2 = "设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：已配对" + "\n";
                    if(deviceName.contains(s2)) {
                        Intent intent = new Intent(MainActivity.this, BTReadAndWrite.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("deviceAddr", device.getAddress());
                        intent.putExtras(bundle);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });


        // 通过id获取“是否支持蓝牙”按钮
        Button button_1 = (Button) findViewById(R.id.button3);
        // 绑定按钮点击事件处理函数
        button_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取蓝牙权限
                getPermision();
                // 判断是否支持蓝牙
                boolean ret = btController.isSupportBlueTooth();
                // 弹窗显示结果
                showToast("是否支持蓝牙" + ret);
            }
        });

        // 通过id获取“当前蓝牙状态”按钮
        Button button_2 = (Button) findViewById(R.id.button4);
        // 绑定按钮点击事件处理函数
        button_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取蓝牙权限
                getPermision();
                // 判断当前蓝牙状态
                boolean ret = btController.getBlueToothStatus();
                // 弹窗显示结果
                showToast("当前蓝牙状态：" + ret);
            }
        });

        // 通过id获取"打开蓝牙"按钮
        Button button_3 = (Button) findViewById(R.id.button7);
        // 绑定按钮点击事件处理函数
        button_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnONbt();
            }
        });

        // 通过id获取”关闭蓝牙“按钮
        Button button_4 = (Button) findViewById(R.id.button8);
        // 绑定按钮点击事件处理函数
        button_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取蓝牙权限
                getPermision();
                // 关闭蓝牙
                btController.turnOffBlueTooth();
            }
        });

        // 通过id获取”使蓝牙可见“按钮
        Button button_5 = (Button) findViewById(R.id.button9);
        // 绑定按钮点击事件处理函数
        button_5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 蓝牙可见
                BTVisible();
            }
        });

        // 通过id获取”搜索可见蓝牙“按钮
        Button button_6 = (Button) findViewById(R.id.button10);
        // 绑定按钮点击事件处理函数
        button_6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 获取蓝牙权限
                getPermision();
                // 注册广播
                registerReceiver(bluetoothReceiver, foundFilter);
                // 初始化各列表
                arrayList.clear();
                deviceName.clear();
                adapter1.notifyDataSetChanged();
                // 开始搜索
                btController.findDevice();
            }
        });

        // 通过id获取”查看已绑定蓝牙“按钮
        Button button_7 = (Button) findViewById(R.id.button11);
        // 绑定按钮点击事件处理函数
        button_7.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                // 获取蓝牙权限
                getPermision();
                // 初始化各列表
                deviceName.clear();
                arrayList.clear();
                adapter1.notifyDataSetChanged();
                // 获取已绑定蓝牙
                ArrayList<BluetoothDevice> bluetoothDevices = btController.getBondedDeviceList();
                // 更新列表
                for (int i = 0; i < bluetoothDevices.size(); i++){
                    BluetoothDevice device = bluetoothDevices.get(i);
                    arrayList.add(device.getAddress());
                    if (device.getBondState() == 12) {
                        deviceName.add("设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：已配对" + "\n");
                    }
                    else if (device.getBondState() == 10){
                        deviceName.add("设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未配对" +"\n");
                    }else{
                        deviceName.add("设备名：" + device.getName() + "\n" + "设备地址：" + device.getAddress() + "\n" + "连接状态：未知" + "\n");
                    }
                    adapter1.notifyDataSetChanged();
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            showToast("open successfully");
        }
        else{
            showToast("open unsuccessfully");
        }
    }

    /**
     * 尝试取消配对
     * @param device
     */
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开蓝牙
     */
    public void turnONbt(){
        // 获取蓝牙权限
        getPermision();
        // 打开蓝牙
        btController.turnOnBlueTooth(this,1);
    }

    /**
     * 设置蓝牙可见
     */
    public void BTVisible(){
        // 获取蓝牙权限
        getPermision();
        // 打开蓝牙可见
        btController.enableVisibly(this);
    }

    /**
     * 动态申请权限
     */
    public void getPermision(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            requestList.add(Manifest.permission.BLUETOOTH_SCAN);
            requestList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            requestList.add(Manifest.permission.BLUETOOTH_CONNECT);
            requestList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            requestList.add(Manifest.permission.BLUETOOTH);
        }
        if(requestList.size() != 0){
            ActivityCompat.requestPermissions(this, requestList.toArray(new String[0]), REQ_PERMISSION_CODE);
        }
    }

    /**
     * Toast弹窗显示
     * @param text  显示文本
     */
    public void showToast(String text){
        // 若Toast控件未初始化
        if( mToast == null){
            // 则初始化
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        }
        // 否则
        else{
            // 修改显示文本
            mToast.setText(text);
        }
        // 显示
        mToast.show();
    }
}
