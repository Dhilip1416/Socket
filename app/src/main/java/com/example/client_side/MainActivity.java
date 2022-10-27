package com.example.client_side;

import static android.os.Build.VERSION.SDK_INT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static int SERVER_PORT;
    public static String SERVER_IP ;
    private ClientThread clientThread;
    private Thread thread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;
    private EditText edMessage,etIP,etPort;
    static ExecutorService threadPool;
    static String msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Client");
        threadPool = Executors.newFixedThreadPool(2);
        clientTextColor = ContextCompat.getColor(this, R.color.black);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        etIP=findViewById(R.id.etIP);
        etPort=findViewById(R.id.etPort);
    }
    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() + "]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }
    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }

    @Override
    public void onClick(View view) {
        try {
            if (view.getId() == R.id.connect_server) {
                Toast.makeText(this, "check your wifi connection", Toast.LENGTH_SHORT).show();
                SERVER_IP = etIP.getText().toString().trim();
                SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
                msgList.removeAllViews();
                showMessage("Connecting...", clientTextColor);
                if (SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        clientThread = new ClientThread();
                        thread = new Thread(clientThread);
                        thread.start();

                    } else {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", MainActivity.this.getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
                clientThread = new ClientThread();
                thread = new Thread(clientThread);
                thread.start();

                return;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "Please Enter IP address and Port Number", Toast.LENGTH_SHORT).show();
        }


        if (view.getId() == R.id.send_data) {
            String clientMessage = edMessage.getText().toString().trim();
            showMessage(clientMessage, Color.BLUE);
            if (null != clientThread) {
                clientThread.sendMessage(clientMessage);
            }
        }
    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVER_PORT);

                while (!Thread.currentThread().isInterrupted()) {

                    if (socket.isConnected()) {
                        showMessage("Connected to Server...", clientTextColor);

                        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        int message = input.read();

                        char value_char = (char) message;
                        msg = String.valueOf(value_char);
                        threadPool.execute(new TextThread());
                        showMessage("Server: " + msg, clientTextColor);
                        Log.d("messsage:", msg);
                        if (message == -1) {
                            Thread.currentThread().interrupt();
                            showMessage("Server Disconnected..........!", Color.RED);
                            break;
                        }

                    }else
                    {
                        showMessage("Check your network...", clientTextColor);
                    }

                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }
    String getTime() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }


    static class TextThread implements Runnable{

        @Override
        public void run() {

            String fileName = "Control" + ".txt";
            try {
                File root = new File(Environment.getExternalStorageDirectory() + File.separator + "Socket");
                root.mkdirs();
                File text_file = new File(root, fileName);
                FileWriter writer = new FileWriter(text_file, true);
                Log.d("File created", String.valueOf(writer));
                writer.append(msg).append("\n");
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}