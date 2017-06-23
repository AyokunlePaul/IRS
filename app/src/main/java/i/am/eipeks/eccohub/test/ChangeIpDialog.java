package i.am.eipeks.eccohub.test;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.laiqian.print.model.PrintJob;
import com.laiqian.print.model.PrintJob.StatusObserver;
import com.laiqian.print.model.PrintJobs;
import com.laiqian.print.model.PrintManager;
import com.laiqian.print.model.Printer;

import i.am.eipeks.eccohub.R;

public class ChangeIpDialog extends Dialog {
	
	Printer printer;
	TextView tvOriginal;
	EditText etNew;
	Button btnCancel;
	Button btnOK;
	
	public ChangeIpDialog(Context context, Printer printer) {
		super(context);
		this.printer = printer;
		setContentView(R.layout.dialog_change_ip);
		tvOriginal = (TextView) findViewById(R.id.tv_original);
		etNew = (EditText) findViewById(R.id.et_new);
		btnCancel = (Button) findViewById(R.id.btn_cancel);
		btnOK = (Button) findViewById(R.id.btn_ok);
		
		tvOriginal.setVisibility(View.GONE);
		
		setListeners();
	}
	
	private void setListeners() {
		btnCancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		
		btnOK.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String newIP = etNew.getText().toString();
				try {
					PrintJob job = PrintJobs.newEscPosChangeIpJob(printer, newIP);
					job.setStatusObserver(new StatusObserver() {
						
						@Override
						public void onStatus(PrintJob job, int newStatus) {
							if(job.isEnded()) {
								if(newStatus == PrintJob.STATUS_COMPLETED) {
									dismiss();
								} else {
									Toast.makeText(getContext(), job.getErrorMessage(), Toast.LENGTH_SHORT).show();
								}
							}
 						}
					});
					PrintManager.INSTANCE.print(job);
				} catch (IllegalArgumentException e) {
					Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	
	
	
	
}
