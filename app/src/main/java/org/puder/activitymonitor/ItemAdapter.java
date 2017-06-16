package org.puder.activitymonitor;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    interface ListItemClickListener {
        void onItemClicked(int position);
    }


    private List<String>          itemList;
    private ListItemClickListener listener;


    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView itemNameView;


        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClicked(getAdapterPosition());
                }
            });
            itemNameView = (TextView) v.findViewById(R.id.item_name);
        }
    }


    public ItemAdapter(List<String> itemList, ListItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.itemNameView.setText(itemList.get(position));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}