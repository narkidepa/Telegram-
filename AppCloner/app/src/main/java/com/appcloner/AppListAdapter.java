package com.appcloner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class AppListAdapter extends ArrayAdapter<MainActivity.AppInfo> {

    public AppListAdapter(Context context, List<MainActivity.AppInfo> apps) {
        super(context, 0, apps);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MainActivity.AppInfo app = getItem(position);
        
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_app, parent, false);
        }
        
        ImageView imgIcon = convertView.findViewById(R.id.item_icon);
        TextView txtName = convertView.findViewById(R.id.item_name);
        TextView txtPackage = convertView.findViewById(R.id.item_package);
        
        imgIcon.setImageDrawable(app.icon);
        txtName.setText(app.appName);
        txtPackage.setText(app.packageName);
        
        return convertView;
    }
}
