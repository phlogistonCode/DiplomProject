package com.phlogiston.diplomproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "diplomProject";

    //Экземпляры классов наших кнопок
    Button btnOn, btnOff;

    //Сокет, с помощью которого мы будем отправлять данные на Arduino
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private String btnMsg = null;

    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-адрес Bluetooth модуля
    private static String address = "98:D3:31:FC:46:4F";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //"Соединям" вид кнопки в окне приложения с реализацией
        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);

        //Мы хотим использовать тот bluetooth-адаптер, который задается по умолчанию
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        View.OnClickListener ClckListn = new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.btnOn:
                            sendData("1");
                            if (btnMsg == null)
                            btnMsg = "Включаем LED";
                            Toast.makeText(getBaseContext(), btnMsg, Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btnOff:
                            sendData("0");
                            if (btnMsg == null)
                            btnMsg = "Выключаем LED";
                            Toast.makeText(getBaseContext(), btnMsg, Toast.LENGTH_SHORT).show();
                        break;
                }

            }
        };

        btnOn.setOnClickListener(ClckListn);
        btnOff.setOnClickListener(ClckListn);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...Возобновляем - попытка соединения...");

        // Настроить указатель на удаленный узел, используя его адрес.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        /*
            Две вещи нужны для создания соеденения:
            Это MAC-адресс, который мы указали выше.
            И Service ID или UUID.  В нашем случае мы используем
            UUID для SPP.
        */
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Критическая ошибка", "В методе <onResume()> возникла ошибка и не удалось установить соеденение по сокету; " + e.getMessage() + ".");
        }

        // Обнаружение является ресурсоемким процессом. Убедитесь, что этого
        // не происходит, когда вы пытаетесь подключиться и передать свое сообщение.
        btAdapter.cancelDiscovery();

        // Установка соединения. Этот вызов будет блокироваться до тех пор, пока не установится подключение.
        Log.d(TAG, "...Соединяемся...");
        try {
            btSocket.connect();
            Log.d(TAG, "Соединение установлено и готово к передаче данных.");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Критическая ошибка", "В методе <onResume()> возникла ошибка и не удается закрыть соединение; " + e2.getMessage() + ".");
            }
        }

        // Создаем поток данных, чтобы можно было разговаривать с сервером.
        Log.d(TAG, "...Создание Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Критическая ошибка", "В методе <onResume()> возникла ошибка и произошел сбой при создании выходного потока; " + e.getMessage() + ".");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...Останавливаем...");

        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Критическая ошибка", "В методе <onPause()> возникла ошибка и не удалось очистить выходной поток: " + e.getMessage() + ".");
            }
        }

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Критическая ошибка", "В методе <onPause()> возникла ошибка и не удалось закрыть сокет." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Проверить поддержку Bluetooth и затем убедиться, что он включен
        // Если эмулятор не поддерживает Bluetooth, то прилоежние вылетит с ошибкой
        if(btAdapter==null) {
            errorExit("Критическая ошибка", "Bluetooth не поддерживается.");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth включен...");
            } else {
                //Подсказать пользователю включить Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        Log.d(TAG, "...Посылаем данные: " + message + "...");

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "В методе <sendData()> возникла ошибка и не удалось отправить данные: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00")) {
                msg = msg + ".\n\nУ вас прописан 00:00:00:00:00:00, вам необходимо прописать реальный MAC-адрес Bluetooth модуля";
                errorExit("Критическая ошибка", msg);
            }
            btnMsg = msg;
        }
    }

}

