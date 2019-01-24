package com.jario.arduino;

import android.annotation.SuppressLint;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    private static final int REQUEST_ENABLE_BT = 1;

    BluetoothAdapter bluetoothAdapter;

    ArrayList<BluetoothDevice> pairedDeviceArrayList;

    ListView listViewPairedDevice;
    TextView inputField;
    Button btnSend;
    ImageView img;
    //Tiempo para de reconocimiento
    private final int REQUEST_SPEECH_RECOGNIZER = 3000;
    String mAnswer = "";

    ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;

    //Esto es la dirección del Bluetoooh dependiendo de si está en placa o no
    private UUID myUUID;
    private final String UUID_STRING_WELL_KNOWN_SPP =
            "00001101-0000-1000-8000-00805F9B34FB";

    ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listViewPairedDevice = findViewById(R.id.pairedlist);

        //Identificamos los objetos
        inputField = findViewById(R.id.input);
        btnSend = findViewById(R.id.send);
        img = findViewById(R.id.imageView);

        //Comprobamos si soporta bluetooh
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NO es soportable",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //using the well-known SPP UUID
        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        //Comprueba si la placa es compatible con el móvil
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth no es compatible con esta plataforma de hardware",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        //Activas el reconocimiento de vox
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                escuchar();
            }
        });

    }
    //Metodo para ecuchar voz
    private void escuchar() {
        Intent intent = new Intent
                (RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"A tus órdenes");
        startActivityForResult(intent, REQUEST_SPEECH_RECOGNIZER);
    }
    @SuppressLint("SetTextI18n")
    @Override
    //Método del resultado del conocimiento de voz
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Comprueba si se reconoce la voz
        if (requestCode == REQUEST_SPEECH_RECOGNIZER) {
            //Si la voz es ok avanz<
            if (resultCode == RESULT_OK) {
                //Guarda lo que reconoce en un array de String
                List<String> results = data.getStringArrayListExtra
                        (RecognizerIntent.EXTRA_RESULTS);
                //Finalmente el resultado lo guardamos en una varibale
                mAnswer = results.get(0);
                //Mostramos aquello que hemos dicho
                inputField.setText(mAnswer);
                //Botón para enviar la información al arduino
                btnSend.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {
                        if(myThreadConnected!=null){
                            //transformamos el string en bytes
                            byte[] bytesToSend = inputField.getText().toString().getBytes();
                            //Se lo enviamos al Arduino
                            myThreadConnected.write(bytesToSend);
                            //Muestra mensaje de que se ha enviado
                            Toast.makeText(MainActivity.this, "Mensaje Enviado", Toast.LENGTH_LONG).show();
                        }
                    }});
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Con esto damos el permiso a la aplicación de utilizar el bluetooh
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        setup();
    }

    private void setup() {
        //Array donde Guardamos en un array los dispositvos emparejados
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        //Miramos si es mayor que cero el array
        if (pairedDevices.size() > 0) {
            pairedDeviceArrayList = new ArrayList<BluetoothDevice>();
            //Recorremos los dispositivos para guardarlos
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayList.add(device);
            }
            //Inflar el activity y mostrar la lista
            pairedDeviceAdapter = new ArrayAdapter<BluetoothDevice>(this,
                    android.R.layout.simple_list_item_1, pairedDeviceArrayList);
            listViewPairedDevice.setAdapter(pairedDeviceAdapter);
            //Cada elemento de la lista se puede clickear
            listViewPairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    //Si lo clicamos cogemos su posición
                    BluetoothDevice device =
                            (BluetoothDevice) parent.getItemAtPosition(position);
                    //Mostramos Todas las características del dispositivo al que vamos a conectarnos
                    Toast.makeText(MainActivity.this,
                            "Nombre: " + device.getName() + "\n"
                                    + "MAC: " + device.getAddress() + "\n"
                                    + "BondState: " + device.getBondState() + "\n"
                                    + "Tipo de Bluetooh: " + device.getBluetoothClass() + "\n"
                                    + "Class: " + device.getClass(),
                            Toast.LENGTH_LONG).show();
                    //Finalmente cogemos el dispositvo y lo añadimos al Thread
                    myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
                    //Iniciamos el hilo start
                    myThreadConnectBTdevice.start();
                }
            });
        }
    }
    //Si vemos que el dispositvo es nulo entonces se cancela la conexión
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }


    //Connectamos mediante el socket
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }


    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private final BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {

                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){

                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        //Mostramos la lista
                        listViewPairedDevice.setVisibility(View.GONE);
                    }});
                //Conectamos el Socket
                startThreadConnected(bluetoothSocket);
            }else{
                //fallo
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                //Socket de entrada y salida
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //Elementos de llegada
            connectedInputStream = in;
            //Elementos de salida
            connectedOutputStream = out;
        }
        //Escribe y envia al arduino
        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
