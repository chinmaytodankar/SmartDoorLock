package com.git.chinmaytodankar.mca;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
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

public class ManageUserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_user);
        Button backBtn = findViewById(R.id.back);
        Button updateBtn = findViewById(R.id.update);
        final CheckBox delete = findViewById(R.id.deleteUsr);
        final TextView connStat = findViewById(R.id.connstat);
        final EditText username = findViewById(R.id.userName);
        final EditText entry = findViewById(R.id.entry);
        final Spinner dropdown = findViewById(R.id.selectcolumn);
        String[] items = new String[]{"First Name", "Last Name", "Admin Rights","Password"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
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
                    connStat.setText("Connection : Connected");
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
                    connStat.setText("Connection : Not Connected");
                    // Something went wrong e.g. connection timeout or firewall problems
                    //Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = username.getText().toString();
                String entryString = entry.getText().toString();
                String[] valueToUpdate = {"fname","lname","adminrights","pass","delete"};
                boolean deleteHim = delete.isChecked();
                if(userName.matches("")) {
                    Toast.makeText(ManageUserActivity.this,"User Name Cannot be empty",Toast.LENGTH_SHORT).show();
                }
                else if((entryString.matches("") && !deleteHim)) {
                    Toast.makeText(ManageUserActivity.this,"please enter the new value.",Toast.LENGTH_SHORT).show();
                }
                else {
                    int dataToModify = dropdown.getSelectedItemPosition();
                    if (dataToModify == 3) {
                        try {
                            entryString = encrypt.encryptPass(entryString);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(deleteHim) {
                        dataToModify = 4;
                        entryString = "...";
                    }
                    String topic = "ChinmayTodankar/feeds/adduser";
                    String payload = userName + "," + valueToUpdate[dataToModify] + "," + entryString;
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
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                connStat.setText("Connection : Not Connected");
                Toast.makeText(ManageUserActivity.this,"Connection Lost!",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String msg = new String(message.getPayload());
                String[] status = msg.split(",");
                if(status[0].equals("ChangesDone")) {
                    Toast.makeText(ManageUserActivity.this,"Success",Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}
