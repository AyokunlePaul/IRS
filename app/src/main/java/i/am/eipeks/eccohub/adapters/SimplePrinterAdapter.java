package i.am.eipeks.eccohub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.laiqian.print.model.Printer;

import java.util.ArrayList;

import i.am.eipeks.eccohub.R;

/**
 * Created by INVENTAR on 15/06/2017.
 */

public class SimplePrinterAdapter extends BaseAdapter {

    private ArrayList<Printer> printers;
    private LayoutInflater inflater;

    public SimplePrinterAdapter(Context context, ArrayList<Printer> printers){
        inflater = LayoutInflater.from(context);
        this.printers = printers;
    }

    @Override
    public int getCount() {
        return printers.size();
    }

    @Override
    public Printer getItem(int position) {
        return printers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListItem item;
        Printer printer = getItem(position);

        if (convertView == null){
            convertView = inflater.inflate(R.layout.activity_simple_print_test, parent, false);
            item = new ListItem(convertView);
            convertView.setTag(item);
        } else {
            item = (ListItem) convertView.getTag();
        }

        item.setPrinter(printer);

        return null;
    }

    private static class ListItem{

        public TextView tvId;
        public TextView tvName;
        public TextView tvType;
        public TextView tvStatus;


        public ListItem(View view){
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
