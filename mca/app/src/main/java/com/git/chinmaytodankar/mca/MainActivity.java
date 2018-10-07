package com.git.chinmaytodankar.mca;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import android.content.Intent;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    boolean responseReceived = false;
    int matchCode = 0;
    String userString;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSub = findViewById(R.id.btnSubscribe);
        final TextView connStat = findViewById(R.id.connstat);
        final Button quitApp = findViewById(R.id.quit);
        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://io.adafruit.com:1883",
                        clientId);

        try {
            if(isNetworkAvailable()) {
            }
            else {
                Toast.makeText(MainActivity.this,"Internet Unavailable",Toast.LENGTH_SHORT).show();
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            options.setUserName("ChinmayTodankar");
            options.setPassword("70baad8ab6704f28980fa730936411aa".toCharArray());
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    connStat.setText("Connection : Connected");
                    try {
                        IMqttToken subToken = client.subscribe("ChinmayTodankar/feeds/mca_loginresponse", 1);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Toast.makeText(MainActivity.this,"Subscription Success!",Toast.LENGTH_SHORT).show();
                                // The message was published
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                Toast.makeText(MainActivity.this,"Subscription Failed!",Toast.LENGTH_SHORT).show();
                                // authorized to subscribe on the specified topic e.g. using wildcards

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    // We are connected
                    //Log.d(TAG, "onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    connStat.setText("Connection : Not Connected");
                    Toast.makeText(MainActivity.this,"Connection Failed!",Toast.LENGTH_SHORT).show();
                    // Something went wrong e.g. connection timeout or firewall problems
                    //Log.d(TAG, "onFailure");

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
        btnSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView user = findViewById(R.id.userName);
                TextView pass = findViewById(R.id.password);
                userString = user.getText().toString();
                String passString = pass.getText().toString();
                matchCode = new Random().nextInt(100);
                if (userString.matches("") || passString.matches("")) {
                    Toast.makeText(MainActivity.this,"Username or password field cannot be empty!",Toast.LENGTH_SHORT).show();
                } else {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (isNetworkAvailable()) {
                        //we are connected to a network
                        user.setText("");
                        pass.setText("");
                        responseReceived = false;
                        final Timer timer = new Timer();
                        final TimerTask timertask = new TimerTask() {
                            @Override
                            public void run() {
                                if (!responseReceived) {
                                    MainActivity.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "SmartDoorLock System is not connected check the power and internet connection!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                                timer.cancel();
                            }
                        };
                        timer.schedule(timertask, 5000);
                        String encryptedPass = null;
                        try {
                            encryptedPass = encrypt.encryptPass(passString);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String topic = "ChinmayTodankar/feeds/mca_logincheck";
                        String payload = userString + "," + encryptedPass + "," + matchCode;
                        byte[] encodedPayload = new byte[0];
                        try {
                            encodedPayload = payload.getBytes("UTF-8");
                            MqttMessage message = new MqttMessage(encodedPayload);
                            client.publish(topic, message);
                        } catch (UnsupportedEncodingException | MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Check Your Internet Connection!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        quitApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                connStat.setText("Connection : Not Connected");
                Toast.makeText(MainActivity.this,"Connection Lost!",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                responseReceived = true;
                String msg = new String(message.getPayload());
                String[] status = msg.split(",");
                if(status[0].equals("Success") && matchCode == Integer.parseInt(status[2])) {
                    Toast.makeText(MainActivity.this,"Login Successful",Toast.LENGTH_SHORT).show();
                    if(status[1].matches("N") || status[1].matches("n")) {
                        Intent intent = new Intent(MainActivity.this, ToggleDoorActivity.class);
                        intent.putExtra("doorStat",status[3]);
                        intent.putExtra("username",userString);
                        startActivity(intent);
                    }
                    else if(status[1].matches("Y") || status[1].matches("y")) {
                        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                        intent.putExtra("doorStat",status[3]);
                        intent.putExtra("username",userString);
                        startActivity(intent);
                    }

                }
                else if(status[0].equals("NotSuccess")) {
                    Toast.makeText(MainActivity.this,"Password Incorrect! Check Your Password again.",Toast.LENGTH_SHORT).show();
                }
                else if(status[0].equals("Unknown")) {
                    Toast.makeText(MainActivity.this,"UserName not found! check your user name or contact admin to create a username.",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Toast.makeText(MainActivity.this,"Logging In!",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
