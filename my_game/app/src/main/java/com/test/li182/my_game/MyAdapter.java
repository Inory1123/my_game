package com.test.li182.my_game;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by li182 on 2018/1/20.
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

    private List<Player> mData;

    public MyAdapter(List<Player> data) {
        this.mData = data;
    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 实例化展示的view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item, parent, false);
        // 实例化viewholder
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 绑定数据
        Player player = mData.get(position);
        if (player.getName()!=null){
            holder.tvUser.setText(player.getName());
        }
        if (player.getScore()>=0){
            holder.tvScore.setText(""+player.getScore());
        }
        if (player.ready){
            holder.tvState.setText("准备");
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvUser;
        TextView tvScore;
        TextView tvState;
        public ViewHolder(View itemView) {
            super(itemView);
            tvUser = (TextView) itemView.findViewById(R.id.item_user);
            tvScore = (TextView) itemView.findViewById(R.id.item_score);
            tvState = (TextView) itemView.findViewById(R.id.item_state);
        }
    }
}