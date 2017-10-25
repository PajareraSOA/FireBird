package com.example.lauta.interfazopajarito;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Index extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "ConectarDispositivos" ;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); ;
    private BluetoothSocket btSocket;
    private final int handlerState = 0;
    private Handler handlerBluetooth;
    private StringBuilder recDataString = new StringBuilder();

    UseDevice useDevice;

    // Sensor
    private SensorManager sensorManager;
    long ultimaActualizacion = 0, ultimoMovimiento = 0;
    float x = 0, y = 0, z = 0, xAnterior = 0, yAnterior = 0, zAnterior = 0;
    private TextView[] meassures = new TextView[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        meassures[0] = (TextView) findViewById(R.id.temperatura);
        meassures[1] = (TextView) findViewById(R.id.humo);
        meassures[2] = (TextView) findViewById(R.id.flama);
        meassures[3] = (TextView) findViewById(R.id.comida);
        meassures[4] = (TextView) findViewById(R.id.agua);
        handlerBluetooth = myHandler();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    private Handler myHandler() {
        return new Handler() {
            public void handleMessage(android.os.Message msg) {
                //si se recibio un msj del hilo secundario
                if (msg.what == handlerState) {
                    //voy concatenando el msj
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("\r\n");

                    //cuando recibo toda una linea la muestro en el layout
                    if (endOfLineIndex > 0) {
                        String[] dataInPrint = recDataString.substring(0, endOfLineIndex).split("-");

                        for (int i =0; i < dataInPrint.length; i++) {
                            meassures[i].setText(dataInPrint[i]);
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        BluetoothDevice dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice((String) getIntent().getExtras().get("aConectar"));
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        ConnectBT connectThread = new ConnectBT(dev);
    }

    @Override
    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    private class ConnectBT {
        public ConnectBT(BluetoothDevice device) {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                useDevice = new UseDevice(btSocket);
                useDevice.start();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "No pude crear socket", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private class UseDevice extends Thread {
        private InputStream in;
        private OutputStream out;

        public UseDevice(BluetoothSocket btSocket) {

            try {
                out = btSocket.getOutputStream();
                in = btSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error al obtener input/output", Toast.LENGTH_SHORT).show();
                return;
            }


        }

        public void write(String x) {
            byte[] msgBuffered = x.getBytes();
            try {
                out.write(msgBuffered);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error al enviar x", Toast.LENGTH_SHORT).show();
                finish();
            }
        }


        public void run() {
            byte[] buffer = new byte[512];
            int bytes;

            //el hilo secundario se queda esperando mensajes del HC05
            while (true)
            {
                try
                {
                    //se leen los datos del Bluethoot
                    bytes = in.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);


                    //se muestran en el layout de la activity, utilizando el handler del hilo
                    // principal antes mencionado
                    handlerBluetooth.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }

        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            float[] valores =  event.values;

            switch(event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    long tiempoActual = event.timestamp;

                    x = valores[0];
                    y = valores[1];
                    z = valores[2];

                    if(xAnterior == 0 && yAnterior == 0 && zAnterior == 0) {
                        ultimaActualizacion = tiempoActual;
                        ultimoMovimiento = tiempoActual;
                        xAnterior = x;
                        yAnterior = y;
                        zAnterior = z;
                    }

                    long diferencia = tiempoActual - ultimaActualizacion;
                    if(diferencia > 0) {
                        float movimiento = Math.abs((x + y + z) - (xAnterior - yAnterior - zAnterior)) / diferencia;
                        int limite = 1500;
                        float movimiento_min = 1E-8f;
                        if(movimiento > movimiento_min) {
                            if(tiempoActual - ultimoMovimiento >= limite) {
                                if(Math.abs(y - yAnterior) < 1 && Math.abs(x - xAnterior) > 10 && Math.abs(z - zAnterior) < 1) {
                                    Toast.makeText(getApplicationContext(), "El movimiento tan preciado", Toast.LENGTH_SHORT).show();
                                    //useDevice.write("1");
                                }
                            }
                        }

                        ultimoMovimiento = tiempoActual;
                    }

                    xAnterior = x;
                    yAnterior = y;
                    zAnterior = z;
                    ultimaActualizacion = tiempoActual;

                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
