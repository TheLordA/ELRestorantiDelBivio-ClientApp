package com.project.clientapp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.clientapp.Common.Common;
import com.project.clientapp.Modal.Addon;
import com.project.clientapp.Modal.EventBus.AddOnEventChange;
import com.project.clientapp.R;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyAddonAdapter extends RecyclerView.Adapter<MyAddonAdapter.MyviewHolder> {

    Context context ;
    List<Addon> addonList ;

    public MyAddonAdapter(Context context, List<Addon> addonList) {
        this.context = context;
        this.addonList = addonList;
    }

    @NonNull
    @Override
    public MyviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyviewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_addon,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyviewHolder holder, int position) {
        holder.ckb_addon.setText(new StringBuilder(addonList.get(position).getName())
                .append(" + ("+context.getString(R.string.money_sign))
                .append(addonList.get(position).getExtraPrice())
                .append(")")
        );

        holder.ckb_addon.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean b) {
                if (b){
                    Common.addonList.add(addonList.get(position));
                    EventBus.getDefault().postSticky(new AddOnEventChange(true,addonList.get(position)));
                }
                else {
                    Common.addonList.remove(addonList.get(position));
                    EventBus.getDefault().postSticky(new AddOnEventChange(false,addonList.get(position)));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return addonList.size();
    }

    public class MyviewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.ckb_addon)
        CheckBox ckb_addon;

        Unbinder unbinder;

        public MyviewHolder(@NonNull View itemView) {
            super(itemView);

            unbinder = ButterKnife.bind(this,itemView);
        }
    }
}
