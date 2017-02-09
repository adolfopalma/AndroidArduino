package com.example.adolfo.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

//https://github.com/patriotaSJ/Bluetooth
public class MainActivity extends Activity implements SensorEventListener{

    ImageButton btnArriba, btnAbajo, btnDerecha, btnIzquierda;
    TextView txtArduino, txtString, txtStringLength, sensorView0, sensorView1, sensorView2, sensorView3;
    TextView txtSendorLDR;
    Handler bluetoothIn;
    TextView tv_x, tv_y, tv_z;
    SensorManager sensorManager;
    Sensor ac;
    Boolean flag;

    final int handlerState = 0;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        flag = false;

        List<Sensor> listaSensores;
        listaSensores = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (!listaSensores.isEmpty()) {

            ac = listaSensores.get(0);
            sensorManager.registerListener(this, ac, SensorManager.SENSOR_DELAY_UI);
        }

        setContentView(R.layout.activity_main);

        //Link the buttons and textViews to respective views
        btnDerecha = (ImageButton) findViewById(R.id.bDerecha);
        btnIzquierda = (ImageButton) findViewById(R.id.bIzquierda);
        btnAbajo = (ImageButton) findViewById(R.id.bAbajo);
        btnArriba = (ImageButton) findViewById(R.id.bAbajo);



        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {										//if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);      								//keep appending to string until ~
                    int endOfLineIndex = recDataString.indexOf("~");                    // determine the end-of-line
                    if (endOfLineIndex > 0) {                                           // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                        txtString.setText("Datos recibidos = " + dataInPrint);
                        int dataLength = dataInPrint.length();							//get length of data received
                        txtStringLength.setText("Tamaño del String = " + String.valueOf(dataLength));

                        if (recDataString.charAt(0) == '#')								//if it starts with # we know it is what we are looking for
                        {
                            String sensor0 = recDataString.substring(1, 5);             //get sensor value from string between indices 1-5
                            String sensor1 = recDataString.substring(6, 10);            //same again...
                            String sensor2 = recDataString.substring(11, 15);
                            String sensor3 = recDataString.substring(16, 20);

                            if(sensor0.equals("1.00"))
                            sensorView0.setText("Encendido");	//update the textviews with sensor values
                            else
                                sensorView0.setText("Apagado");	//update the textviews with sensor values
                            sensorView1.setText(sensor1);
                            sensorView2.setText(sensor2);
                            sensorView3.setText(sensor3);
                            //sensorView3.setText(" Sensor 3 Voltage = " + sensor3 + "V");
                        }
                        recDataString.delete(0, recDataString.length()); 					//clear all string data
                        // strIncom =" ";
                        dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

    }

    public void pulsarAdelante(View view){
        mConnectedThread.write("U");    // Send "1" via Bluetooth
        Toast.makeText(getBaseContext(), "Adelante", Toast.LENGTH_SHORT).show();
    }
    public void pulsarAtras(View view){
        mConnectedThread.write("A");    // Send "1" via Bluetooth
        Toast.makeText(getBaseContext(), "Atras", Toast.LENGTH_SHORT).show();
    }
    public void pulsarDerecha(View view){
        mConnectedThread.write("D");    // Send "1" via Bluetooth
        Toast.makeText(getBaseContext(), "Derecha", Toast.LENGTH_SHORT).show();
    }
    public void pulsarIzquierda(View view){
        mConnectedThread.write("I");    // Send "1" via Bluetooth
        Toast.makeText(getBaseContext(), "Izquierda", Toast.LENGTH_SHORT).show();
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, ac, SensorManager.SENSOR_DELAY_NORMAL);
        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        //Log.i("ramiro", "adress : " + address);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        sensorManager.unregisterListener(this);
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    public void pulsarSensor(View v){
        if(flag)flag=false;
        else flag=true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        tv_x = (TextView)findViewById(R.id.tv_x);
        tv_y = (TextView)findViewById(R.id.tv_y);
        tv_z = (TextView)findViewById(R.id.tv_z);
       if(flag && sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER){

        tv_x.setText(sensorEvent.values[0]+"");
        tv_y.setText(sensorEvent.values[1]+"");
        tv_z.setText(sensorEvent.values[2]+"");

           Double x, y, z;
           x = Double.parseDouble(tv_x.getText().toString());
           y = Double.parseDouble(tv_y.getText().toString());
           z = Double.parseDouble(tv_z.getText().toString());


           if((x <=6 && x >= 0) && (y > -5 && y < 5) && (z > 0 && z < 20)){
               mConnectedThread.write("U");
           }
           if((x <=7 && x >= 4) && (y > 0 && y < 8) && (z > 0 && z < 20)){
               mConnectedThread.write("D");
           }
           if((x <=7 && x >= 4) && (y > -9 && y < 0) && (z > 0 && z < 20)){
               mConnectedThread.write("I");
           }

           //REPOSO  x=7.4    y= -0.5      z= 5.9
           // acelerar  x = 3    y = 0.5   z = 9
           // derecha  x = 5.5   y = 6.9   z 3.3
           // izquqierda x = 4   y = -8    z = 3.3
       }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);        	//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();

            }
        }


    }



}

