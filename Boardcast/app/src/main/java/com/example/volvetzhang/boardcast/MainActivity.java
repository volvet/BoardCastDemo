package com.example.volvetzhang.boardcast;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;


public class MainActivity extends AppCompatActivity implements OnClickListener, Runnable {
    static String  TAG = "BoardCast";

    private Button  mBtnBoradCast;
    private TextView mTextView;
    private String  mDiscoveryData;
    final private int SEND_PORT = 53214;
    final private int LISTEN_PORT = 53215;
    DatagramSocket mSendSocket;
    DatagramPacket mPingpacket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnBoradCast = (Button)findViewById(R.id.btnBoardCast);
        mBtnBoradCast.setOnClickListener(this);
        mTextView = (TextView)findViewById(R.id.TextView);
        mDiscoveryData = getLocalAddress().toString();
        mDiscoveryData = mDiscoveryData.substring(mDiscoveryData.lastIndexOf("/") + 1);
        try {
            mSendSocket = new DatagramSocket(SEND_PORT);
            mPingpacket = new DatagramPacket(mDiscoveryData.getBytes(), mDiscoveryData.getBytes().length);
            mSendSocket.setBroadcast(true);
        }
        catch (java.net.SocketException e){
            Log.e(TAG, "Create socket fail:" + e.toString());
        }
        new Thread(this).start();
    }

     class BroadCastSender implements Runnable {
        @Override
        public void run() {
            InetAddress boardCastAddress = getBoardCastAddress();
            InetAddress localAddress = getLocalAddress();
            Log.i(TAG, "Local address is: " + localAddress.toString());
            Log.i(TAG, "BoardCast address is: " + boardCastAddress.toString() );

            try {
                mPingpacket.setAddress(boardCastAddress);
                mPingpacket.setPort(LISTEN_PORT);
                mSendSocket.send(mPingpacket);
            }
            catch(java.net.SocketException e) {
                Log.e(TAG, "Send fail:" + e.toString());
            }
            catch(java.io.IOException e) {
                Log.e(TAG, "Send fail:" + e.toString());
            }
        }
    }

    public InetAddress getBoardCastAddress() {
        try {
            final InetAddress defaultAddr = InetAddress.getByName("255.255.255.255");
            WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (null == dhcpInfo) {
                return defaultAddr;
            }
            int boardcast = (dhcpInfo.ipAddress & dhcpInfo.netmask) | ~dhcpInfo.netmask;
            byte[] quads = new byte[4];
            for (int i = 0; i < 4; i++) {
                quads[i] = (byte)((boardcast >> i * 8) & 0xff);
            }
            return InetAddress.getByAddress(quads);
        }
        catch (java.net.UnknownHostException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    public InetAddress getLocalAddress() {
        try {
            final InetAddress defaultAddr = InetAddress.getByName("0.0.0.0");
            WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            if (null == dhcpInfo) {
                return defaultAddr;
            }
            int address = dhcpInfo.ipAddress;
            byte[] quads = new byte[4];
            for (int i = 0; i < 4; i++) {
                quads[i] = (byte)((address >> i * 8) & 0xff);
            }
            return InetAddress.getByAddress(quads);
        }
        catch (java.net.UnknownHostException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        new Thread(new BroadCastSender()).start();
    }

    class UpdateTextViewRunnable implements Runnable {
        String mMsg;
        UpdateTextViewRunnable(String msg) {
            mMsg = msg;
        }
        @Override
        public void run() {
            mTextView.setText(mMsg);
        }
    }

    @Override
    public void run() {
        Log.i(TAG, "Listen on port: " + Integer.toString(LISTEN_PORT));
        try {
            DatagramSocket socket = new DatagramSocket(LISTEN_PORT);
            socket.setBroadcast(true);
            byte data[] = new byte[4096];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            while (true) {
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                Log.i(TAG, "Received: " + msg);
                mTextView.post(new UpdateTextViewRunnable(msg));
            }
        }

        catch( java.net.SocketException e) {
            Log.e(TAG, "Recv fail" + e.toString());
            return;
        }
        catch (IOException e) {
            Log.e(TAG, "Recv fail" + e.toString());
        }
    }
}
