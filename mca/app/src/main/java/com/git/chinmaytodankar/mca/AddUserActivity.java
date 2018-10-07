package com.git.chinmaytodankar.mca;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import java.util.Timer;
import java.util.TimerTask;

public class AddUserActivity extends AppCompatActivity {
    boolean responseReceived = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);
        Button addUser = findViewById(R.id.adduser);
        final EditText userName = findViewById(R.id.username);
        final EditText fname = findViewById(R.id.fname);
        final EditText lname = findViewById(R.id.lname);
        final EditText password = findViewById(R.id.pass);
        final CheckBox adminRights = findViewById(R.id.isAdmin);
        String clientId = MqttClient.generateClientId();
        final MqttAndroidClient client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://io.adafruit.com:1883",
                        clientId);

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            options.setUserName("ChinmayTodankar");
            options.setPassword("70baad8ab6704f28980fa730936411aa".toCharArray());
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        IMqttToken subToken = client.subscribe("ChinmayTodankar/feeds/mca_loginresponse", 1);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {

                                // The message was published
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not

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

                    // Something went wrong e.g. connection timeout or firewall problems
                    //Log.d(TAG, "onFailure");

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
        Button backBtn = findViewById(R.id.back);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        addUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usrString = userName.getText().toString();
                String fnameString = fname.getText().toString();
                String lnameString = lname.getText().toString();
                String passString = password.getText().toString();
                boolean adminStat = adminRights.isChecked();
                String adminString = null;
                if (adminStat) {
                    adminString = "Y";
                } else {
                    adminString = "N";
                }
                if (usrString.matches("") || fnameString.matches("") || lnameString.matches("") || passString.matches("")) {
                    Toast.makeText(AddUserActivity.this, "Text Fields Cannot be Empty.", Toast.LENGTH_SHORT).show();
                } else {
                    responseReceived = false;
                    final Timer timer = new Timer();
                    final TimerTask timertask = new TimerTask() {
                        @Override
                        public void run() {
                            if (!responseReceived) {
                                AddUserActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(AddUserActivity.this, "Failed to add user please try again later.", Toast.LENGTH_LONG).show();
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
                    String topic = "ChinmayTodankar/feeds/adduser";
                    String payload = usrString + "," + encryptedPass + "," + fnameString + "," + lnameString + "," + adminString;
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(AddUserActivity.this,"Connection Lost!",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                responseReceived = true;
                String msg = new String(message.getPayload());
                String[] status = msg.split(",");
                if(status[0].equals("UserCreated")) {
                    Toast.makeText(AddUserActivity.this,"User Added!",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}
