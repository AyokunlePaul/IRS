package i.am.eipeks.eccohub.test;

import android.app.Application;
import android.widget.Toast;
import com.laiqian.print.model.PrintManager;
import com.laiqian.print.util.PrintUtils;

/**
 * Created by raid on 2016/5/5.
 */
public class App extends Application {
    private static final String TEST_ID = "6d9926930ca987e83bdfa1f3013c6be1";

    @Override public void onCreate() {
        super.onCreate();
        PrintManager.INSTANCE.setCallback(new PrintManager.InitializeResultCallback() {
            @Override public void onResult(int code, final String msg) {
                PrintUtils.runInMainThread(new Runnable() {
                    @Override public void run() {
                        Toast.makeText(App.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        PrintManager.INSTANCE.init(this, TEST_ID);
    }
}
