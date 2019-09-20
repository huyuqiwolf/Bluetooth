package com.ken.demo.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ken.demo.bluetooth.bluetooth.BluetoothServer;
import com.ken.demo.bluetooth.bluetooth.SendData;
import com.ken.demo.bluetooth.constant.Constant;
import com.ken.demo.bluetooth.util.AssetsUtil;

public class ClientActivity extends AppCompatActivity {
    private static final String TAG = "ClientActivity";
    public static final String DEVICE = "device";
    private BluetoothDevice device;
    private BluetoothServer msgService;
    private TextView tvState;
    private TextView tvReceived;
    private TextView tvPicPath;
    private EditText etMsg;
    private ImageView ivReceived;
    private Button btnChoosePic;
    private Button btnSendPic;
    private Button btnSendMsg;
    private Toast toast;
    private String picPath;


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Constant.BLUETOOTH_CONNECTED:
                    tvState.setText((String) msg.obj);
                    break;
                case Constant.BLUETOOTH_DISCONNECTED:
                    showToast("连接断开");
                    tvState.setText("连接断开");
                    break;
                case Constant.BLUETOOTH_SEND_ERROR:
                    showToast("发送失败");
                    break;
                case Constant.BLUETOOTH_SEND_FAILED:
                    showToast("未连接发送");
                    break;
                case Constant.BLUETOOTH_SEND_SUCCESS:
                    showToast("发送成功");
                    break;
                case Constant.BLUETOOTH_DATA_RECEIVED:
                    showToast("接受到数据");
                    SendData data = (SendData) msg.obj;
                    if (data.getType() == 0) {
                        tvReceived.setText(new String(data.getData()));
                    } else if (data.getType() == 1) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data.getData(), 0, data.getData().length);
                        ivReceived.setImageBitmap(bitmap);
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        this.device = getIntent().getParcelableExtra(DEVICE);
        tvState = findViewById(R.id.tv_state);
        tvReceived = findViewById(R.id.tv_received);
        tvPicPath = findViewById(R.id.tv_pic_path);
        etMsg = findViewById(R.id.et_msg);
        ivReceived = findViewById(R.id.iv_received);
        btnChoosePic = findViewById(R.id.btn_choose_pic);
        btnSendPic = findViewById(R.id.btn_send_pic);
        btnSendMsg = findViewById(R.id.btn_send_msg);

        msgService = BluetoothServer.getInstance(handler);

        btnChoosePic.setOnClickListener((v) -> {
            choosePicture();
        });
        btnSendPic.setOnClickListener((v) -> {
            sendPic();
        });
        btnSendMsg.setOnClickListener((v) -> {
            sendMsg();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (msgService != null) {
            msgService.connect(device);
        }
    }

    private void sendMsg() {
        String trim = etMsg.getText().toString().trim();
        if (TextUtils.isEmpty(trim)) {
            showToast("请输入");
            return;
        }
        msgService.sendData(new SendData((byte) 0, trim.getBytes()).getBytes());
    }


    private void sendPic() {
        byte[] bytes = AssetsUtil.openAssertFile(getAssets(), "hello.jpg");
        msgService.sendData(new SendData((byte) 1, bytes).getBytes());
    }

    private void choosePicture() {
//        String trim = etMsg.getText().toString().trim();
//        if (TextUtils.isEmpty(trim)) {
//            showToast("请输入");
//            return;
//        }
//        byte[] bytes = AssetsUtil.openAssertFile(getAssets(), "hello.jpg");
//
//        msgService.sendData(new SendData((byte) 3,trim, bytes).getBytes());
    }

    private void showToast(String msg) {
        if (toast == null) {
            toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        }
        toast.setText(msg);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        msgService.stop();
        super.onDestroy();
    }
}
