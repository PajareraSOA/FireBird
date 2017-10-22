package com.example.lauta.interfazopajarito;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1010;
    private static final int YES_RESULT = -1;
    private static final int NO_RESULT = 0;

    private ListView listView;
    private ListView listEmparejados;

    private static final String TAG = "Bluetooth";
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();

    private ProgressDialog prd;

    /*private AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Toast.makeText(getApplicationContext(), (String) parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
            Intent it = new Intent(MainActivity.this, ConectarDispositivo.class);
            it.putExtra("aConectar", (String) ((String) parent.getItemAtPosition(position)).split("\n")[1]);
            startActivity(it);

        }
    };
    private AdapterView.OnItemClickListener clickListenerVinculado = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Toast.makeText(getApplicationContext(), (String) parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
        }
    };

*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        listEmparejados = (ListView) findViewById(R.id.listEmparejados);

        //
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        prd = new ProgressDialog(this);
        prd.setMessage("Buscando dispositivos...");

        // Verificar si el dispositivo tiene bluetooth
        if(bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "THERE IS NO BLUETOOTH", Toast.LENGTH_LONG).show();

        } else {
            // Indicamos que nuestro objeto Receiver se subscribe a los 3 eventos de Bluetooth del sistema
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, action);
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                list.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                Log.i(TAG, "Encontre uno!");
            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                list.clear();
                prd.show();
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                prd.dismiss();
/*                Intent intentsearch = new Intent(MainActivity.this, Dispositivos.class);
                intentsearch.putExtra("Lista", list);*/
                ArrayList<BluetoothDevice> paired = new ArrayList<BluetoothDevice>(bluetoothAdapter.getBondedDevices());
                /*intentsearch.putExtra("Vinculados", paired);
                startActivity(intentsearch);*/
                ArrayList<String> nameDevices = new ArrayList<String>();
                ArrayList<String> nameVinculados = new ArrayList<>();
                list.removeAll(paired);
                for (BluetoothDevice device : list) {
                    nameDevices.add(device.getName() + "\n" + device.getAddress());
                }
                ArrayAdapter<String> devicesAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, nameDevices);


                if (!paired.isEmpty()) {
                    for (BluetoothDevice device : paired) {
                        nameVinculados.add(device.getName() + "\n" + device.getAddress());
                    }
                }

                ArrayAdapter<String> devicesAdapterVinculados = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, nameVinculados);

                listView.setAdapter(devicesAdapter);
                listView.setOnItemClickListener(clickListener);

                listEmparejados.setAdapter(devicesAdapterVinculados);
                listEmparejados.setOnItemClickListener(clickListener);

            }
        }
    };

    private AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            Toast.makeText(getApplicationContext(), (String) parent.getItemAtPosition(position), Toast.LENGTH_SHORT).show();
            Intent it = new Intent(MainActivity.this, Index.class);
            it.putExtra("aConectar", (String) ((String) parent.getItemAtPosition(position)).split("\n")[1]);
            startActivity(it);
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == YES_RESULT) {
                    bluetoothAdapter.startDiscovery();
                }
                else {
                    Toast.makeText(getApplicationContext(), "No es posible utilizar la aplicación con el bluetooth desactivado.", Toast.LENGTH_SHORT).show();
                    finish();
                }
        }

    }

    @Override
    public void onResume(){
        super.onResume();
        checkBtConection();
    }

    @Override
    public void onDestroy(){
        unregisterReceiver(mReceiver);
        if(bluetoothAdapter != null) {
            bluetoothAdapter.disable();
        }
        super.onDestroy();
    }

    // Verificar si el bluetooth está activado
    private void checkBtConection() {
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        } else {
            bluetoothAdapter.startDiscovery();
        }
    }
}
