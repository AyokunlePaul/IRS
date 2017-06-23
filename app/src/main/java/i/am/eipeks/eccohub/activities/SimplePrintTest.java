package i.am.eipeks.eccohub.activities;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.text.format.Time;
import android.widget.Toast;

import com.laiqian.print.model.IPrinterDiscoverySession;
import com.laiqian.print.model.PrintContent;
import com.laiqian.print.model.PrintJob;
import com.laiqian.print.model.PrintManager;
import com.laiqian.print.model.Printer;
import com.laiqian.print.model.type.serial.SerialPrintManager;
import com.laiqian.print.util.NetUtils;
import com.laiqian.print.util.PrintUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android_serialport_api.SerialPortFinder;
import i.am.eipeks.eccohub.R;
import i.am.eipeks.eccohub.adapters.SimplePrinterAdapter;

public class SimplePrintTest extends Activity implements
        AdapterView.OnItemClickListener{

    private SimplePrinterAdapter adapter;
//    private ContentView contentView;
    private static final String usbPropertyFileName = "printer.json";
    private ArrayList<Printer> printers = new ArrayList<>();
    private PrintContent printContent;
    private PrintJob.StatusObserver statusObserver;

    private Printer printer;
    private PrintJob job;

    private IPrinterDiscoverySession netSession;
    private IPrinterDiscoverySession usbSession;
    private IPrinterDiscoverySession bluetoothSession;
    private IPrinterDiscoverySession serialSession;

    private SerialPrintManager serialPrintManager;


    private IPrinterDiscoverySession.PrinterDiscoveryObserver discoveryObserver;

    private ListView lv;
    private Button button1;
    private Button button2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_print);

        serialPrintManager = SerialPrintManager.INSTANCE;
//        serialPrintManager.
//        contentView = ContentView.attachTo(getWindow());
        statusObserver = new PrintJob.StatusObserver() {
            @Override
            public void onStatus(PrintJob printJob, int i) {
                if (printJob.isEnded()){
                    String msg = printJob.getName() + " " + printJob.getStatusName() + "\n";
                    msg += "error msg: " + printJob.getErrorMessage() + "\n";
                    msg += "wait time: " + printJob.getWaitTime() + "ms\n";
                    msg += "execution time: " + printJob.getExecutionTime() + "ms\n";

                    final String message = msg;

                    PrintUtils.runInMainThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SimplePrintTest.this, message, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
            }
        };

        discoveryObserver = new IPrinterDiscoverySession.PrinterDiscoveryObserver() {
            @Override
            public void onPrinterAdded(final Printer printer) {
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                printers.add(printer);
                            }
                        }
                );
            }

            @Override
            public void onDiscoveryCompleted() {
                if (isSearching()){
                    return;
                }
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                button1.setText("Search Completed");
                            }
                        }
                );
            }

            @Override
            public void onDiscoveryFailed() {
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                button1.setText("Search Failed");
                            }
                        }
                );
            }

            @Override
            public void onDiscoveryCancelled() {
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                button1.setText("Search Cancelled");
                            }
                        }
                );
            }

            @Override
            public void onDiscoveryStarted() {
                runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                button1.setText("Searching...");
                            }
                        }
                );
            }
        };

        copyUsbDevicePropertyFile();
        setUpView();
        setListeners();

        PrintManager.INSTANCE.setPrinterConnectionResultObserver(
                new PrintManager.PrinterConnectionResultObserver() {
                    @Override
                    public void onResult(String identifier, boolean result) {
                        Printer printer = findPrinter(identifier);
                        if (printer != null){
                            printer.setConnected(result);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }

    private boolean copyUsbDevicePropertyFile(){
        boolean success;
        String folder = getApplicationInfo().dataDir + File.separator;
        File target  = new File(folder + usbPropertyFileName);

        AssetManager assets = this.getAssets();
        try{
            InputStream inputStream = assets.open(usbPropertyFileName);
            OutputStream outputStream = new FileOutputStream(target);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while((bytesRead = inputStream.read(buffer)) > 0){
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            success = true;
        } catch(IOException e){
            e.printStackTrace();
            success = false;
        }
        return success;
    }

    void setUpView(){
        adapter = new SimplePrinterAdapter(this, printers);
        lv = (ListView) findViewById(R.id.lv);
        button1 = (Button) findViewById(R.id.btn1);
        button2 = (Button) findViewById(R.id.btn2);

        lv.setAdapter(adapter);
    }

    void setListeners(){
        lv.setOnItemClickListener(this);
        button1.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleSearch();
                    }
                }
        );
        button2.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePrintAll();
                    }
                }
        );
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (view.getId()){
            case R.id.lv:
                Log.i("Tag", "onItemClick");
                adapter.notifyDataSetChanged();

                printer = adapter.getItem(position);

                if (printer.isConnected()){
                    Log.i("Tag", "connected");
                    job = new PrintJob(printer, getTestContent());
                    job.setDelay(5000);
                    job.setStatusObserver(statusObserver);
                    PrintManager.INSTANCE.print(job);
                } else {
                    Log.i("Tag", "disconnected, try to connect");
                    PrintManager.INSTANCE.connect(printer);
                }
                break;
        }
    }

    private PrintContent getTestContent(){
        if (printContent != null){
            return printContent;
        }
        PrintContent.Builder builder = new PrintContent.Builder();
        builder.appendString("Title", PrintContent.FONT_DOUBLE_BOTH, PrintContent.ALIGN_CENTER);

        Time time = new Time();
        time.setToNow();
        builder.appendString("Time" + time.format2445(), PrintContent.FONT_NORMAL, PrintContent.ALIGN_RIGHT);

        builder.appendString("Hello Printer!!!", PrintContent.FONT_DOUBLE_HEIGHT);

        try{
            InputStream inputStream = getResources().getAssets().open("weixin_200_200.bmp");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            builder.appendBitmap(bitmap, PrintContent.ALIGN_CENTER);
        } catch (IOException e){
            e.printStackTrace();
        }
        printContent = builder.build();
        return printContent;
    }

    void toggleSearch(){
        if (isSearching()){
            cancelPrinterSearch();
        } else {
            startPrinterSearch();
        }
    }

    void togglePrintAll(){
        int size = adapter.getCount();
        for (int i = 0; i < size; i++){
            Printer printer = adapter.getItem(i);
            PrintJob job = new PrintJob(printer, getTestContent());
            job.setStatusObserver(
                    new PrintJob.StatusObserver() {
                        @Override
                        public void onStatus(final PrintJob printJob, int i) {
                            String msg = printJob.getName() + " " + printJob.getStatusName() + "\n";
                            msg += "wait: " + printJob.getWaitTime() + "ms\n";
                            msg += "execution: " + printJob.getExecutionTime() + "ms\n";
                            final String message = msg;

                            Toast.makeText(SimplePrintTest.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            PrintManager.INSTANCE.print(job);
        }
    }

    private Printer findPrinter(String identifier){
        for (Printer printer: printers){
            if (printer.getIdentifier().equals(identifier)){
                return printer;
            }
        }
        return null;
    }

    private boolean isSearching(){
        boolean netSearching = netSession != null && netSession.isSearching();
        boolean usbSearching = usbSession != null && usbSession.isSearching();
        boolean bluetoothSearching = bluetoothSession != null && bluetoothSession.isSearching();

        return netSearching || bluetoothSearching || usbSearching;

    }

    private void cancelPrinterSearch(){
        if (usbSession != null){
            usbSession.cancel();
        }
        if (netSession != null){
            netSession.cancel();
        }
        if (bluetoothSession != null){
            bluetoothSession.cancel();
        }
    }

    private void startPrinterSearch(){

        prepareForSearch();

        if (usbSession != null){
            usbSession.start();
        }
        if (netSession != null){
            netSession.start();
        }
        if (bluetoothSession != null){
            bluetoothSession.start();
        }
    }

    private void prepareForSearch(){
        clearSearchResult();

        if (PrintManager.INSTANCE.isSerialPrintAvailable()){
//            Printer printer = findPrinter();
//            serialSession = SerialPrintManager.INSTANCE.connect()
            Toast.makeText(this, "Supports serial printing", Toast.LENGTH_SHORT).show();
        }

        if (PrintManager.INSTANCE.isUsbPrintAvailable()){
            usbSession = PrintManager.INSTANCE.openUsbPrinterDiscoverySession();
            if (usbSession != null) {
                usbSession.setObserver(discoveryObserver);
            }
        } else {
            Toast.makeText(this, "USB Function not available", Toast.LENGTH_SHORT).show();
        }

        if (NetUtils.isConnected(this) && PrintManager.INSTANCE.isNetPrintAvailable()){
            netSession = PrintManager.INSTANCE.openNetPrinterDiscoverySession();
            if (netSession != null){
                netSession.setObserver(discoveryObserver);
            }
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }

        if (PrintManager.INSTANCE.isBluetoothPrintAvailable()){
            bluetoothSession = PrintManager.INSTANCE.openBluetoothPrinterDiscoverySession();
            if (bluetoothSession != null){
                bluetoothSession.setObserver(discoveryObserver);
            }
        }

    }

    private void clearSearchResult(){
        printers.clear();
        adapter.notifyDataSetChanged();
    }

//    private static class ContentView {
//        public static final int LAYOUT_ID = R.layout.activity_simple_print_test;
//
//        public View root;
//
//        public ContentView(View v) {
//            root = v;
//        }
//
//
//        public static ContentView attachTo(Window window) {
//            View v = View.inflate(window.getContext(), LAYOUT_ID, null);
//            window.setContentView(v);
//            return new ContentView(v);
//        }
//    }

}
