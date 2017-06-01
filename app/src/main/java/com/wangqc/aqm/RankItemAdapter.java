package com.wangqc.aqm;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by wang9 on 2017/5/3.
 */

public class RankItemAdapter extends RecyclerView.Adapter<RankItemAdapter.ViewHolder>{
    private List<RankItemUtil> mItemList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText;
        TextView cityText;
        TextView aqiText;
        TextView levelText;

        public ViewHolder (View view){
            super(view);
            rankText = (TextView) view.findViewById(R.id.text_rank);
            cityText = (TextView) view.findViewById(R.id.text_cityname);
            aqiText = (TextView) view.findViewById(R.id.text_cityaqi);
            levelText = (TextView) view.findViewById(R.id.text_airlevel);
        }
    }

    public RankItemAdapter(List<RankItemUtil> itemUtilList){
        mItemList = itemUtilList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ranklist_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RankItemUtil item = mItemList.get(position);
        holder.rankText.setText(String.valueOf(position + 1));
        holder.cityText.setText(item.getCityName());
        holder.aqiText.setText(item.getAqi());
        holder.levelText.setText(item.getAirLevel());
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }
}
