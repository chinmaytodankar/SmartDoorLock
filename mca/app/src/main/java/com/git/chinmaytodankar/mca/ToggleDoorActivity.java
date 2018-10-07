package com.git.chinmaytodankar.mca;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
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

public class ToggleDoorActivity extends AppCompatActivity {
    String userString = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toggle_door);
        Button quitApp = findViewById(R.id.quit);
        final TextView connStat = findViewById(R.id.connstat);
        final Switch doorToggle = findViewById(R.id.doortoggle);
        String clientId = MqttClient.generateClientId();
        Bundle prevVals = getIntent().getExtras();
        userString = prevVals.getString("username");
        TextView userDisp = findViewById(R.id.userdisp);
        userDisp.setText("User Name : "+userString);
        if (prevVals.getString("doorStat").matches("ON")) {
            doorToggle.setChecked(true);
        }
        else {
            doorToggle.setChecked(false);
        }
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
                    connStat.setText("Connection : Connected");
                    try {
                        IMqttToken subToken = client.subscribe("ChinmayTodankar/feeds/doorstat", 1);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {

                                // The message was published
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                Toast.makeText(ToggleDoorActivity.this,"Subscription Failed!",Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ToggleDoorActivity.this,"Connection Failed!",Toast.LENGTH_SHORT).show();
                    // Something went wrong e.g. connection timeout or firewall problems
                    //Log.d(TAG, "onFailure");

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
        doorToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (isNetworkAvailable()) {
                    String payload = null;
                    if (isChecked) {
                        payload = doorToggle.getTextOn().toString();
                    } else {
                        payload = doorToggle.getTextOff().toString();
                    }
                    String topic = "ChinmayTodankar/feeds/doorstat";

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
                    Toast.makeText(ToggleDoorActivity.this, "Check Your Internet Connection!", Toast.LENGTH_LONG).show();
                }
            }
        });
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                connStat.setText("Connection : Not Connected");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if(topic.matches("ChinmayTodankar/feeds/doorstat")) {
                    String msg = new String(message.getPayload());
                    if(msg.matches("ON")) {
                        doorToggle.setChecked(true);
                    }
                    else {
                        doorToggle.setChecked(false);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        quitApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
