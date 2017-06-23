package i.am.eipeks.eccohub.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.laiqian.print.model.IPrinterDiscoverySession;
import com.laiqian.print.model.PrintManager;
import com.laiqian.print.model.Printer;
import com.laiqian.print.model.type.net.NetPrinterDiscoverySession;
import com.laiqian.print.model.type.usb.PrinterProperty;
import com.laiqian.print.util.PrintUtils;

import i.am.eipeks.eccohub.R;

public class Home extends Activity {

    private static final String TEST_ID = "6d9926930ca987e83bdfa1f3013c6be1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        boolean isBluetoothAvailable = PrintManager.INSTANCE.isBluetoothPrintAvailable();
        if (isBluetoothAvailable){
            Toast.makeText(Home.this, "Device supports bluetooth printing", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(Home.this, "Device does not support bluetooth printing", Toast.LENGTH_SHORT).show();
        }

        PrintManager.INSTANCE.setCallback(
                new PrintManager.InitializeResultCallback() {
                    @Override
                    public void onResult(int i, final String s) {
                        PrintUtils.runInMainThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(Home.this, s, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    }
                }
        );

        PrintManager.INSTANCE.init(this, TEST_ID);

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Home.this, SimplePrintTest.class);
                        startActivity(intent);
                    }
                }, 2000);
    }
}
