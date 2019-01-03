package com.example.rfidtester;

import java.io.IOException;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;

import android.R.integer;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.text.format.Time;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
        
        public static byte seekForTag_TX[] = new byte[] { (byte) 255, (byte) 0, (byte) 1, (byte) 130, (byte) 131 };
        public static byte haltEt_ilk_TX[] = new byte[] { (byte) 255, (byte) 0, (byte) 1, (byte) 147, (byte) 148 };
        public static byte haltEt_son_TX[] = new byte[] { (byte) 0xff, (byte) 0x00, (byte) 0x02, (byte) 0x93, (byte) 0x4c, (byte) 0xe1 };

        public static UsbManager manager;
        public static UsbSerialDriver driver;
        
        public static String seekForTag_RX = "FF0002824CD0";
        public static String tagFound_RX = "FF000682";
        
                
        private static SQLiteDatabase myData = null;
        //private static String db_path = "/data/data/com.example.rfidtester/databases/rfdatabase.sqlite";     
        private static String db_path = "/mnt/sdcard/Android/data/com.example.rfidtester/databases/rfdatabase.sqlite";

        private static String bilgiBas;
        private static String persBas;
        private static String statusBas;
        
        public static boolean okumaAktif = false;
        private static boolean yemekVar = false;
        private static boolean threadBitti = false;
        private static boolean portAcik = false;
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
        //Date date = new Date();//
        
        private Thread th;

        ImageView indOn;
        ImageView indOff;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        indOn = (ImageView) findViewById(R.id.imgOn);
        Drawable drawOn = getResources().getDrawable(R.raw.indgre);
        indOn.setImageDrawable(drawOn);
        indOn.setVisibility(View.INVISIBLE);
        
        indOff = (ImageView) findViewById(R.id.imgOff);
        Drawable drawOff = getResources().getDrawable(R.raw.indred);
        indOff.setImageDrawable(drawOff);
        //indOff.setVisibility(View.INVISIBLE);        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public void onPause() {
    	super.onPause();
        //System.exit(0); 
        //android.os.Process.killProcess(android.os.Process.myPid());    
    }

    @Override
    public void onResume() {
    	//System.exit(0);
    }
    /*
    ************************************* BUTONLAR PORT AC / KAPAT ******************************
    */
    public void Button_portAc(View view) {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap hm = new HashMap();
        hm = manager.getDeviceList();
        System.out.println(manager.getDeviceList().values());
        //UsbDeviceConnection usbConn = manager.openDevice(usbDevice);
        
        
        
        try {
                        Button acButon = (Button)findViewById(R.id.ButonAc);
                        Button kapatButon = (Button)findViewById(R.id.ButonKapat);
                        acButon.setEnabled(false);
                        //Toast.makeText(getApplicationContext(), "bana tost yap", Toast.LENGTH_LONG).show();
                        while (!portAcik) {
                        if (seriPortAc()) {
                
                                System.out.println("Seri Port Açýldý");
                                okumaAktif = true;
                            runThread(); //-----> Kart Okuma Baslat
                        }               
                        }    
                kapatButon.setEnabled(true);      

                indOn.setVisibility(View.VISIBLE);
                indOff.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
                
        }
    }
    
    public void Button_portKapat(View view) throws InterruptedException{
        try {
                        Button acButon = (Button)findViewById(R.id.ButonAc);
                        Button kapatButon = (Button)findViewById(R.id.ButonKapat);              
                        kapatButon.setEnabled(false);
                                                
                        okumaAktif = false;
                        while (!threadBitti) {
                        }
                        
                        if (cihazReset()) {
                                driver.close();
                        
                                portAcik = false;
                        
                        acButon.setEnabled(true);
                        //kapatButon.setEnabled(false);
                        
                        System.out.println("Seri Port Kapatýldý");
                        }
                                                
                        EditText txtBox = (EditText)findViewById(R.id.statusText);
                        txtBox.setText("Kapalý. Kart Okuma Aç tuþuna basýnýz.");
                        
                indOn.setVisibility(View.INVISIBLE);
                indOff.setVisibility(View.VISIBLE);
                        //if(portAcik) kapatButon.setEnabled(true);
                /*
                        if (cihazReset()) {
                        okumaAktif = false;
                                driver.close();
                        
                        Button acButon = (Button)findViewById(R.id.ButonAc);
                        Button kapatButon = (Button)findViewById(R.id.ButonKapat);
                        
                        acButon.setEnabled(true);
                        kapatButon.setEnabled(false);
                        
                        System.out.println("Seri Port Kapatýldý");
                        }
                */
        } catch (Exception e){
                System.out.println("Sürücü Kapatma Hatasý: " + e.getMessage()); 
        }
    }    
    /*
    ********************************************************************************************
    */
    
    
    
    
    /*
    ************************************* PORT AC / CýHAZ RESET ********************************
    */
    public boolean seriPortAc() {
        portAcik = false;

        try {
                        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                        driver = UsbSerialProber.acquire(manager);
        
        
                        if (driver != null) 
                        {
                                        driver.open();
                                        driver.setBaudRate(115200);
                                        
                                        portAcik = true;
                                        //okumaAktif = true;
                        }
            } catch (Exception e) {
                System.out.println("Seri Port Açma Hatasý: " + e.getMessage());
                portAcik = false;
            }
        return portAcik;
    }
    
    public boolean cihazReset() {
        boolean cihazResetlendi = false;
        
                try {
                                driver.write(haltEt_ilk_TX, 1000);
                                        Thread.sleep(100); //----------------> Sleep Here
                                        
                                driver.write(haltEt_son_TX, 1000);
                                        Thread.sleep(100); //----------------> Sleep Here
                        
                                cihazResetlendi = true;
                                
                                System.out.println("Cihaz Resetlendi");
                } catch (Exception e){
                        System.out.println("Cihaz Resetleme Hatasý: " + e.getMessage());
                }
                return cihazResetlendi;
        }
    /*
    ***********************************************************************************************
    */
    
    
    
    /*
    ************************************* DATABASE KONTROL ****************************************
    */
    private String[] kartBul(String kartId) {
        //SQLiteDatabase myData = null;
        //String db_path = "/data/data/com.example.rfidtester/databases/rfdatabase.sqlite"; 
        Cursor cursor = null;
        String userInfo[];// = new String[7];
        
        try {                   
                                myData = SQLiteDatabase.openDatabase(db_path, null, SQLiteDatabase.OPEN_READWRITE);
                                //cursor = myData.query("USERS", new String[]{"ID","KARTID","AD","SOYAD","BIRIM"}, "KARTID=?", new String[]{kartId.trim()}, null, null, null, null);
                                cursor = myData.rawQuery("SELECT * FROM USERS WHERE KARTID=?", new String[]{kartId.trim()});
                                
                                if (cursor != null && cursor.getCount()!=0) {
                                cursor.moveToFirst();
                                //String kartBilgi = cursor.getString(0) + "  " + cursor.getString(1) + "  " + cursor.getString(2) + "  " + cursor.getString(3) + "  " + cursor.getString(4);

                                //bilgiBas = cursor.getString(0) + "  " + cursor.getString(1) + "  " + cursor.getString(2) + "  " + cursor.getString(3) + "  " + cursor.getString(4);
                                //data_Insert(cursor.getString(0), cursor.getString(1), "0", "0", "0", dateFormat.format(new Date()));
                                //userId = Long.getLong(cursor.getString(0));
                                //kayitVar = true;
                                userInfo = new String[cursor.getColumnCount()];
                                //int rowCount = cursor.getCount();
                                for (int i = 0; i < userInfo.length; i++) {
                                        userInfo[i] = cursor.getString(i);
                                        System.out.println(cursor.getString(i));
                                }                               
                                                                
                                int ogunsay = Integer.parseInt(cursor.getString(cursor.getColumnIndex("OGUNSAY")));
                                int misafirsay = Integer.parseInt(cursor.getString(cursor.getColumnIndex("MISAFIRSAY")));
                                
                                persBas = cursor.getString(cursor.getColumnIndex("AD")) + " " + cursor.getString(cursor.getColumnIndex("SOYAD")) + " " + cursor.getString(cursor.getColumnIndex("BIRIM"));
                                
                                if (ogunsay > 0) {
                                        ogunsay--;
                                        bilgiBas = "1 yemek hakký kullanýldý. Kalan: " + ogunsay;
                                } else {
                                        if (misafirsay > 0) {
                                                misafirsay--;
                                                bilgiBas = "1 misafir yemek hakký kullanýldý. Kalan: " + misafirsay;
                                        } else {
                                                bilgiBas = "Yemek hakký yok.";
                                        }
                                }
                                System.out.println(ogunsay + " " + misafirsay);
                                data_Update(cursor.getString(cursor.getColumnIndex("ID")), ogunsay, misafirsay);
                                //System.out.println("1 hakkýn kaldý daha dikkatli olmalýsýn");
                                } else {
                                        System.out.println("Kart tanýmsýz.");                   
                                        bilgiBas = "Kart tanýmsýz.";
                                        //kayitVar = false;
                                        //userId = -1;
                                        userInfo = null;
                                }
                                
        } catch (Exception e)
        {
                System.out.println(e.getMessage());
                userInfo = null;
        } finally {
                if (cursor != null) {
                        while (!cursor.isClosed())
                        cursor.close();                         
                }
                
                if (myData != null) {
                        while (myData.isOpen())
                        myData.close();
                        myData = null;
                }               
        }
        return userInfo;
    } 
    
 
    private long yemekBul(String[] userInfo) {
        /*
        if (userId < 0) {
                return -1;
        }
        */
        if (userInfo == null) {
                return -1;
        }
        
        Cursor cursor = null;
        
        long kayitId = -1;
        
        try {                   
                                myData = SQLiteDatabase.openDatabase(db_path, null, SQLiteDatabase.OPEN_READWRITE);
                                //cursor = myData.query("YEMEKLER", new String[]{"ID","TARIH"}, "USERID=? AND KARTID=? AND TARIH=?", new String[]{String.valueOf(userId), kartId.trim(), dateFormat.format(new Date())}, null, null, "ID DESC", null);
                                //cursor = myData.query("YEMEKLER", new String[]{"ID","TARIH"}, "USERID=? AND KARTID=?", new String[]{String.valueOf(userId), kartId.trim()}, null, null, "ID ASC", null);
                                cursor = myData.rawQuery("SELECT * FROM YEMEKLER WHERE USERID=? AND KARTID=? AND STRFTIME('%Y-%m-%d',TARIH)=?", new String[]{userInfo[0], userInfo[1].trim(), dateFormat2.format(new Date())});
                                
                                if (cursor != null && cursor.getCount() != 0)
                                {
                                cursor.moveToFirst();
                                bilgiBas = "Yemek hizmeti verilemez";
                                yemekVar = false;
                                } 
                                if (cursor != null && cursor.getCount() == 0) {
                                        //kayitId = data_Insert(userInfo[0], userInfo[1], "0", "0", "0", dateFormat.format(new Date()));
                                        
                                        if (kayitId > 0) {
                                                bilgiBas = userInfo[0] + " " + userInfo[1] + " " + userInfo[2] + " " + userInfo[3] + " " + userInfo[4];
                                                yemekVar = true;
                                        }
                                }
                                
        } catch (Exception e)
        {
                System.out.println(e.getMessage());
        } finally {
                if (cursor != null) {
                        while (!cursor.isClosed())
                        cursor.close();                         
                }
                
                if (myData != null) {
                        while (myData.isOpen())
                        myData.close();
                        myData = null;
                }               
        }
        return kayitId;
    }
    
    
    private long data_Insert( String _userId, String _kartId, String _oguntur, String _misafir, String _ozelizin, String _tarih ) {
        long insertedRowID = -1;
        try {
                        myData = SQLiteDatabase.openDatabase(db_path, null, SQLiteDatabase.OPEN_READWRITE);
                        
                        //Cursor cursor = myData.rawQuery("INSERT INTO YEMEKLER (USERID, KARTID, OGUNTUR, MISAFIR, OZELIZIN, TARIH) VALUES(?,?,?,?,?,DATETIME('NOW'))", new String[]{_userId, _kartId, _oguntur, _misafir, _ozelizin});
                        //cursor.moveToFirst(); // Bu kod ve alttaki cursor.close çaðýrýlmadan kayýt eklemiyor :S
                        //cursor.close();
                        
                        //myData.execSQL("INSERT INTO YEMEKLER (USERID, KARTID, OGUNTUR, MISAFIR, OZELIZIN, TARIH) VALUES("
                        //+ _userId + "," + _kartId + "," + _oguntur + "," + _misafir + "," + _ozelizin + ",DATETIME('NOW'))");
                                                
                        
                        ContentValues insertValues = new ContentValues();
                                insertValues.put("USERID", _userId);
                                insertValues.put("KARTID", _kartId);
                                insertValues.put("OGUNTUR", _oguntur);
                                insertValues.put("MISAFIR", _misafir);
                                insertValues.put("OZELIZIN", _ozelizin);
                                insertValues.put("TARIH", dateFormat.format(new Date()));
                                
                        insertedRowID = myData.insertOrThrow("YEMEKLER", null, insertValues);
                        /*
                        if (insertedRowID > 0) {
                                System.out.println("Kayýt Baþarýyla Eklendi");
                        } else {
                                System.out.println("Kayýt Eklenemedi");
                        }
                        */
        } catch (Exception e) {
                System.out.println("Kayýt Ekleme Hatasý: " + e.getMessage());
        } finally {
                //myData.endTransaction();
                if (myData != null) {
                        while (myData.isOpen())
                        myData.close();
                        myData = null;
                }
        }
        return insertedRowID;
    }    
    
        
    private long data_Update(String _userid, int _ogunsay, int _misafirsay){
        long updatedRowID = -1;
        try {
                        myData = SQLiteDatabase.openDatabase(db_path, null, SQLiteDatabase.OPEN_READWRITE);
                                                
                        ContentValues updateValues = new ContentValues();
                                updateValues.put("OGUNSAY", _ogunsay);
                                updateValues.put("MISAFIRSAY", _misafirsay);
                        
                        updatedRowID = myData.update("USERS", updateValues, "ID=?", new String[]{_userid});
        } catch (Exception e)
        {
                System.out.println("Kayýt Güncelleme Hatasý: " + e.getMessage());
        } finally {
                if (myData != null) {
                        while (myData.isOpen())
                        myData.close();
                        myData = null;
                }
        }
        return updatedRowID;
    }
    
    public void resetDB(View view) {
        try {
                myData = SQLiteDatabase.openDatabase(db_path, null, SQLiteDatabase.OPEN_READWRITE);
                                        
                ContentValues updateValues = new ContentValues();
                        updateValues.put("OGUNSAY", 3);
                        updateValues.put("MISAFIRSAY", 1);
                
                myData.update("USERS", updateValues, "ID=?", new String[]{"1"});
                
                ContentValues updateValues2 = new ContentValues();
                                updateValues2.put("OGUNSAY", 1);
                                updateValues2.put("MISAFIRSAY", 1);
                
                        myData.update("USERS", updateValues2, "ID=?", new String[]{"3"});
                
                EditText txtBox = (EditText)findViewById(R.id.kartText);
                txtBox.setText("");
                System.out.println("Tamamdýr..");
        } catch (Exception e)
        {
                System.out.println("Kayýt Güncelleme Hatasý: " + e.getMessage());
        } finally {
                if (myData != null) {
                        while (myData.isOpen())
                        myData.close();
                        myData = null;
                }
        }       
    }
    /*
    **************************************************************************************************
    */
    /*
    public static final int S1 = R.raw.s1;
    public static void initSounds(Context context) {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        soundPoolMap = new HashMap(1);
        
        soundPoolMap.put(S1, soundPool.load(context, R.raw.S1, 1));
    }*/

    
          
    
    /*
    ********************************************** THREAD / KART OKUMA *******************************
    */
    // MINOVA ýýýN;
    private void runThread() 
    {
        new Thread() 
        {
                
            public void run() 
            {
                
                try {
                                byte buffer[] = new byte[100];
                                threadBitti = false;
                                                                                
                            while (okumaAktif) //---------> 001 
                            {
                                runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                        EditText txtBox = (EditText)findViewById(R.id.statusText);
                                                                txtBox.setText("Kartý Okutunuz..");
                                                                }
                                                });
                                                        
                                        for (int i = 0; i < buffer.length; i++){
                                                buffer[i] = (byte)32;
                                        }
                                        
                                        driver.read(buffer, 1000);
                                        
                                        long kartLong = 0;
                                        String kartOkunanId = new String(buffer).trim();
                                        //System.out.println("KArt okunan bilgi: " + kartOkunanId);
                                        if (kartOkunanId.length() == 8) {
                                                 kartLong = tersineCevir(kartOkunanId);
                                                 System.out.println("Kart: " + kartLong);
                                                 }
                                        
                                        kartOkunanId = Long.toString(kartLong);
                                        
                                                if (okumaAktif && kartOkunanId.length() > 2) {
                                                                                                                
                                                        yemekBul(kartBul(kartOkunanId));
                                                        
                                                        runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        EditText txtBox = (EditText)findViewById(R.id.kartText);
                                                                                txtBox.setText(bilgiBas);
                                                                        EditText txtBox2 = (EditText)findViewById(R.id.statusText);
                                                                                txtBox2.setText("Kart Okundu.");
                                                                        EditText txtBox3 = (EditText)findViewById(R.id.persText);
                                                                                txtBox3.setText(persBas);
                                                                                }
                                                                });
                                                                   
                                                                for (int i = 0; i < buffer.length; i++){
                                                                        buffer[i] = 0;
                                                                }
                                                                
                                                                //if (yemekVar) //
                                                                        //{
                                                                            MediaPlayer mp = MediaPlayer.create(getApplicationContext(),  R.raw.beep);
                                                                            mp.start();
                                                                                //Thread.sleep(500);
                                                                                mp.stop();
                                                                                mp.reset();
                                                                                mp.release();
                                                                        //}
                                                                //Thread.sleep(1500);
                                                                
                                                }
                                                
                            } //---------> 001
                            System.out.println("Thread bitti");
                            threadBitti = true;
                } catch (Exception e) {
                        System.out.println("Thread içinde Hata: " + e.getMessage());
                        }
            }
        }.start();
        //th.start();
    }
    
    /* SONMICRO ýýýN;
    private void runThread() 
    {
  
        //if (th == null)
        //th = new Thread() {
        new Thread() 
        {
                
            public void run() 
            {
                
                try {
                                if (!cihazReset()) return;
                                
                                byte buffer[] = new byte[100];
                                threadBitti = false;
                                                                                
                            while (okumaAktif) //---------> 001 
                            {
                                runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                        EditText txtBox = (EditText)findViewById(R.id.editText);
                                                                txtBox.setText("Kartý Okutunuz..");
                                                                }
                                                });
                                                
                                        while (okumaAktif && !HexDump.toHexString(buffer).contains(seekForTag_RX))  //---------> 002
                                                {
                                                        
                                                        driver.write(seekForTag_TX, 100);
                                                        
                                                                for (int i = 0; i < buffer.length; i++){
                                                                        buffer[i] = 0;
                                                                }
                                                                
                                                                        Thread.sleep(100);
                                                                
                                                                driver.read(buffer, 2000);
                                                                System.out.println(">>>>  " + HexDump.toHexString(buffer));
                                                } //---------> 002
                                        
                                        
                                        int syc1=0;

                                        while (okumaAktif && !HexDump.toHexString(buffer).contains(tagFound_RX)) //---------> 003
                                            {
                                            //KART OKUYUCU ýýýN
                                                syc1++;
                                                
                                                if(syc1 % 250 == 0) {
                                                        //System.out.println("Attempt to Okuma");
                                                          driver.write(seekForTag_TX, 100);
                                                          syc1=0;
                                                  }
                                                  
                                                driver.read(buffer, 1000); 
                                                
                                                } //---------> 003

                                        
                                            int origin = HexDump.toHexString(buffer).indexOf(tagFound_RX, 0);
                                            
                                            long tersi = tersineCevir(HexDump.toHexString(buffer).substring(origin + 10, origin + 18));
                                            
                                                final String kartOkunanId = Long.toString(tersi);
                
                                                System.out.println("Tersi:  " + kartOkunanId);
                                                                   
                                                if (okumaAktif ) {
                                                        yemekBul(kartBul(kartOkunanId));
                                                        
                                                        runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                        EditText txtBox = (EditText)findViewById(R.id.editText);
                                                                                txtBox.setText(bilgiBas);
                                                                                }
                                                                });
                                                                   
                                                                for (int i = 0; i < buffer.length; i++){
                                                                        buffer[i] = 0;
                                                                }
                                                                
                                                                if (yemekVar) 
                                                                        {
                                                                            MediaPlayer mp = MediaPlayer.create(getApplicationContext(),  R.raw.beep);
                                                                            mp.start();
                                                                                Thread.sleep(500);
                                                                                mp.stop();
                                                                                mp.reset();
                                                                                mp.release();
                                                                        }
                                                                Thread.sleep(1500);                                                     
                                                }
                            } //---------> 001
                            System.out.println("Thread bitti");
                            threadBitti = true;
                } catch (Exception e) {
                        System.out.println("Thread içinde Hata: " + e.getMessage());
                        }
            }
        }.start();
        //th.start();
    }
    */
    
        
    
        
        long tersineCevir(String str){
                String kartKimlik = new String();
                //System.out.println("1: " + str);
                
                for (int i = 6; i > -1; i -= 2){
                        //System.out.println(i + " , " + str.substring(i, i+2));
                        kartKimlik += str.substring(i, i+2);
                        //System.out.println("00000000000");
                }
                
                return Long.parseLong(kartKimlik,16);
        }
        //-----------------------------------
    
  
        
        /* DENE BiR ARA***************************
         private Handler mHandler = new Handler();

//code to make text appear...

    mHandler.postDelayed(makeTextDisapear , 3000); // Replace 3000 with the number of milliseconds you want the text to display.

    private Runnable makeTextDisapear = new Runnable() {
            public void run() {
                // code to make text dissapear

                }
            };
         */
    
}