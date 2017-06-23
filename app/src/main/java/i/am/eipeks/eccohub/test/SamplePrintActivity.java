package i.am.eipeks.eccohub.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.laiqian.print.model.IPrinterDiscoverySession;
import com.laiqian.print.model.IPrinterDiscoverySession.PrinterDiscoveryObserver;
import com.laiqian.print.model.PrintContent;
import com.laiqian.print.model.PrintJob;
import com.laiqian.print.model.PrintJob.StatusObserver;
import com.laiqian.print.model.PrintManager;
import com.laiqian.print.model.PrintManager.PrinterConnectionResultObserver;
import com.laiqian.print.model.Printer;
import com.laiqian.print.util.NetUtils;
import com.laiqian.print.util.PrintUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import i.am.eipeks.eccohub.R;
import i.am.eipeks.eccohub.test.format.FixedSpacePrintContentBuilder;
import i.am.eipeks.eccohub.test.format.IFormatedPrintContentBuilder;

/**
 * @author Raid
 */
@SuppressLint("SetTextI18n") public class SamplePrintActivity extends Activity {

    private ContentView content;
    private IPrinterDiscoverySession usbSession;
    private IPrinterDiscoverySession netSession;
    private IPrinterDiscoverySession bluetoothSession;
    private ArrayList<Printer> printers = new ArrayList<>();
    private SimplePrinterAdapter adapter = null;
    private PrintContent testContent = null;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = ContentView.attachTo(getWindow());

        //copy printer property file to be parsed
        copyUsbDevicePropertyFile();

        setupView();
        setListeners();

        //listen to printer connection result
        PrintManager.INSTANCE.setPrinterConnectionResultObserver(
            new PrinterConnectionResultObserver() {

                @Override public void onResult(String identifier, boolean result) {
                    Printer printer = findPrinter(identifier);
                    if (printer != null) {
                        printer.setConnected(result);
                        runOnUiThread(new Runnable() {

                            @Override public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
    }


    void setupView() {
        adapter = new SimplePrinterAdapter(this, printers);
        content.lv.setAdapter(adapter);
    }


    void setListeners() {
        content.button1.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                toggleSearch();
            }
        });

        content.button2.setOnClickListener(new OnClickListener() {

            @Override public void onClick(View v) {
                togglePrintAll();
            }
        });

        content.lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("tag", "onItemClick");
                adapter.notifyDataSetChanged();
                Printer printer = adapter.getItem(position);

                if (printer.isConnected()) {
                    //printer is connected, print
                    Log.i("tag", "connected");
                    PrintJob job = new PrintJob(printer, getTestContent());
                    job.setDelay(5000);
                    job.setStatusObserver(new StatusObserver() {

                        @Override public void onStatus(PrintJob job, int newStatus) {
                            if (job.isEnded()) {
                                String msg = job.getName() + " " + job.getStatusName() + "\n";
                                msg += "error message: " + job.getErrorMessage() + "\n";
                                msg += "wait " + job.getWaitTime() + "ms\n";
                                msg += "execution " + job.getExecutionTime() + "ms";
                                final String finalMsg = msg;
                                PrintUtils.runInMainThread(new Runnable() {

                                    @Override public void run() {
                                        Toast.makeText(SamplePrintActivity.this, finalMsg,
                                            Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                    PrintManager.INSTANCE.print(job);
                } else {
                    //printer not connected
                    Log.i("tag", "disconnected, try connect");
                    PrintManager.INSTANCE.connect(printer);
                }
            }
        });

        content.lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                long id) {
                Printer printer = adapter.getItem(position);
                showChangeIpDialog(printer);
                return true;
            }
        });
    }


    private void showChangeIpDialog(Printer printer) {
        ChangeIpDialog dialog = new ChangeIpDialog(this, printer);
        dialog.show();
    }


    private Printer findPrinter(String identifier) {
        for (Printer printer : printers) {
            if (printer.getIdentifier().equals(identifier)) {
                return printer;
            }
        }
        return null;
    }


    private static final String usbDevicePropertyFileName = "printer.json";


    private boolean copyUsbDevicePropertyFile() {
        boolean success;
        String folder = getApplicationInfo().dataDir + File.separator;
        File target = new File(folder + usbDevicePropertyFileName);

        AssetManager manager = this.getAssets();
        try {
            InputStream is = manager.open(usbDevicePropertyFileName);
            OutputStream os = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buf)) > 0) {
                os.write(buf, 0, bytesRead);
            }
            is.close();
            os.close();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }


    private void togglePrintAll() {
        int size = adapter.getCount();
        for (int i = 0; i < size; ++i) {
            Printer printer = adapter.getItem(i);
            PrintJob job = new PrintJob(printer, getTestContent());
            job.setStatusObserver(new StatusObserver() {

                @Override public void onStatus(final PrintJob job, int newStatus) {
                    if (job.isEnded()) {
                        String msg = job.getName() + " " + job.getStatusName() + "\n";
                        msg += "wait " + job.getWaitTime() + "ms\n";
                        msg += "execution " + job.getExecutionTime() + "ms";
                        final String finalMsg = msg;
                        runOnUiThread(new Runnable() {

                            @Override public void run() {
                                Toast.makeText(SamplePrintActivity.this, finalMsg,
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
            PrintManager.INSTANCE.print(job);
        }
    }


    private void toggleSearch() {
        if (isSearching()) {
            cancelPrinterSearch();
        } else {
            startPrinterSearch();
        }
    }


    private void cancelPrinterSearch() {
        if (usbSession != null) {
            usbSession.cancel();
        }
        if (netSession != null) {
            netSession.cancel();
        }
        if (bluetoothSession != null) {
            bluetoothSession.cancel();
        }
    }


    private void startPrinterSearch() {
        prepareSearch();

        if (usbSession != null) {
            usbSession.start();
        }
        if (netSession != null) {
            netSession.start();
        }
        if (bluetoothSession != null) {
            bluetoothSession.start();        //NOTE: bluetooth discovery may cause wifi disconnection
        }
    }


    private void prepareSearch() {
        clearSearchResult();

        if (PrintManager.INSTANCE.isUsbPrintAvailable()) {
            usbSession = PrintManager.INSTANCE.openUsbPrinterDiscoverySession();
            if (usbSession != null) {
                usbSession.setObserver(generalObserver);
            }
        } else {
            Toast.makeText(this, "USB function not available", Toast.LENGTH_SHORT).show();
        }

        if (NetUtils.isConnected(this) && PrintManager.INSTANCE.isNetPrintAvailable()) {
            netSession = PrintManager.INSTANCE.openNetPrinterDiscoverySession();
            if (netSession != null) {
                netSession.setObserver(generalObserver);
            }
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show();
        }

        if (PrintManager.INSTANCE.isBluetoothPrintAvailable()) {
            bluetoothSession = PrintManager.INSTANCE.openBluetoothPrinterDiscoverySession();
            if (bluetoothSession != null) {
                bluetoothSession.setObserver(generalObserver);
            }
        } else {
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show();
        }
    }


    private PrinterDiscoveryObserver generalObserver = new PrinterDiscoveryObserver() {

        @Override public void onPrinterAdded(final Printer printer) {
            runOnUiThread(new Runnable() {
                public void run() {
                    printers.add(printer);
                    adapter.notifyDataSetChanged();
                }
            });
        }


        @Override public void onDiscoveryFailed() {
            runOnUiThread(new Runnable() {

                @Override public void run() {
                    content.button1.setText("search failed");
                }
            });
        }


        @Override public void onDiscoveryCompleted() {
            if (isSearching()) {
                return;
            }
            runOnUiThread(new Runnable() {

                @Override public void run() {
                    content.button1.setText("search completed");
                }
            });
        }


        @Override public void onDiscoveryCancelled() {
            runOnUiThread(new Runnable() {

                @Override public void run() {
                    content.button1.setText("search cancelled");
                }
            });
        }


        @Override public void onDiscoveryStarted() {
            runOnUiThread(new Runnable() {

                @Override public void run() {
                    content.button1.setText("searching");
                }
            });
        }
    };


    private void clearSearchResult() {
        printers.clear();
        adapter.notifyDataSetChanged();
    }


    private boolean isSearching() {
        boolean netSearching = netSession != null && netSession.isSearching();
        boolean usbSearching = usbSession != null && usbSession.isSearching();
        boolean bluetoothSearching = bluetoothSession != null && bluetoothSession.isSearching();
        return netSearching || usbSearching || bluetoothSearching;
    }


    private PrintContent getBoringContent() {
        PrintContent.Builder builder = new PrintContent.Builder();

        builder.appendString("打印", PrintContent.FONT_DOUBLE_BOTH, PrintContent.ALIGN_CENTER);
        for (int i = 0; i < 5; ++i) {
            builder.appendString(String.valueOf(i));
        }

        builder.appendString("");
        builder.appendBeep();

        return builder.build();
    }


    private PrintContent getTestContent() {
        if (testContent != null) {
            return testContent;
        }

        PrintContent.Builder builder = new PrintContent.Builder();
        //will print new line
        builder.appendString("Title", PrintContent.FONT_DOUBLE_BOTH, PrintContent.ALIGN_CENTER);

        Time t = new Time();
        t.setToNow();
        builder.appendString("Time: " + t.format2445(), PrintContent.FONT_NORMAL,
            PrintContent.ALIGN_RIGHT);
        builder.appendString("Hello, printer!", PrintContent.FONT_DOUBLE_HEIGHT);

        //Wrapper for PrintContent.Builder, providing some formatting feature
        IFormatedPrintContentBuilder formatBuilder = new FixedSpacePrintContentBuilder(builder);
        formatBuilder.appendStrings(new String[] {
            "一二三四五六七八九一二三四五六七八九"
        });
        formatBuilder.appendStrings(new String[] {
            "1"
        });
        formatBuilder.appendStrings(new String[] {
            "2", "2"
        });
        formatBuilder.appendStrings(new String[] {
            "3", "3", "3"
        });
        formatBuilder.appendStrings(new String[] {
            "一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
            "1234567890123456789012345678901234567890"
        });
        formatBuilder.appendStrings(new String[] {
            "4", "4", "4", "4"
        });
        formatBuilder.appendStrings(new String[] {
            "一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
            "1234567890123456789012345678901234567890", "1234567890123456789012345678901234567890"
        });
        builder.appendString("");
        builder.appendString("");
        builder.appendString("");

        //print Bitmap
        InputStream is;
        try {
            is = getResources().getAssets().open("weixin_200_200.bmp");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            builder.appendBitmap(bmp, PrintContent.ALIGN_CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //cash drawer pulse signal
        builder.appendPulseSignal();

        builder.appendString("");
        builder.appendString("");
        builder.appendString("");

        formatBuilder.appendStrings(new String[] {
            "一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
            "1234567890123456789012345678901234567890", "1234567890123456789012345678901234567890"
        });
        builder.appendString("");
        builder.appendString("");
        builder.appendString("");
        formatBuilder.appendStrings(new String[] {
            "一二三四五六七八九一二三四五六七八九", "1234567890123456789012345678901234567890",
            "1234567890123456789012345678901234567890"
        });

        builder.appendString("正常大小Normal");
        builder.appendString("双倍宽度Double Width", PrintContent.FONT_DOUBLE_WIDTH);
        builder.appendString("双倍高度Double Height", PrintContent.FONT_DOUBLE_HEIGHT);
        builder.appendString("双倍字体Double Size", PrintContent.FONT_DOUBLE_BOTH);
        builder.appendString("正常大小粗体Normal Bold", true, false, PrintContent.ALIGN_LEFT, false,
            false);
        builder.appendString("双倍宽度粗体Double Width Bold", true, false, PrintContent.ALIGN_LEFT, true,
            false);
        builder.appendString("双倍高度粗体Double Height Bold", true, false, PrintContent.ALIGN_LEFT,
            false, true);
        builder.appendString("双倍字体粗体Double Size Bold", true, false, PrintContent.ALIGN_LEFT, true,
            true);

        builder.appendString("");
        builder.appendString("");
        builder.appendString("");

        testContent = builder.build();
        return testContent;
    }


    private static class SimplePrinterAdapter extends BaseAdapter {
        private ArrayList<Printer> mPrinters;
        private LayoutInflater inflater;


        public SimplePrinterAdapter(Context context, ArrayList<Printer> printers) {
            mPrinters = printers;
            inflater = LayoutInflater.from(context);
        }


        @Override public int getCount() {
            return mPrinters.size();
        }


        @Override public Printer getItem(int position) {
            return mPrinters.get(position);
        }


        @Override public long getItemId(int position) {
            return position;
        }


        @Override public View getView(int position, View convertView, ViewGroup parent) {
            ListItem item;
            Printer printer = getItem(position);

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_layout_printer, parent, false);
                item = new ListItem(convertView);
                convertView.setTag(item);
            } else {
                item = (ListItem) convertView.getTag();
            }

            item.setPrinter(printer);
            return convertView;
        }


        private static class ListItem {
            public TextView tvId;
            public TextView tvName;
            public TextView tvType;
            public TextView tvStatus;


            public ListItem(View view) {
                tvId = (TextView) view.findViewById(R.id.tv_id);
                tvName = (TextView) view.findViewById(R.id.tv_name);
                tvType = (TextView) view.findViewById(R.id.tv_type);
                tvStatus = (TextView) view.findViewById(R.id.tv_status);
            }


            public void setPrinter(Printer printer) {
                tvId.setText(printer.getIdentifier());
                tvName.setText(printer.getName());
                tvStatus.setText(printer.isConnected() ? "connected" : "disconnected");
                tvType.setText(printer.getTypeName());
            }
        }
    }

    private static class ContentView {
        public static final int LAYOUT_ID = R.layout.activity_sample_print;

        public View root;
        public ListView lv;
        public Button button1;
        public Button button2;


        public ContentView(View v) {
            root = v;
            lv = (ListView) v.findViewById(R.id.lv);
            button1 = (Button) v.findViewById(R.id.btn1);
            button2 = (Button) v.findViewById(R.id.btn2);
        }


        public static ContentView attachTo(Window window) {
            View v = View.inflate(window.getContext(), LAYOUT_ID, null);
            window.setContentView(v);
            return new ContentView(v);
        }
    }
}