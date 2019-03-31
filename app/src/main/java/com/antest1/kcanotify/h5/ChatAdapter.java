package com.antest1.kcanotify.h5;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ChatAdapter extends ArrayAdapter {
    private int resourceId;
    public ChatAdapter(@NonNull Context context, int resource, @NonNull List<ChatMsgObject> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 获取当前项的Fruit实例
        ChatMsgObject msgObject = (ChatMsgObject)getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null){
            view = LayoutInflater.from(getContext()).inflate(resourceId, null);

            viewHolder = new ViewHolder();
            viewHolder.userName = (TextView) view.findViewById(R.id.chat_name);
            viewHolder.msg = (TextView) view.findViewById(R.id.chat_msg);
            view.setTag(viewHolder);
        }else{
            view = convertView;
            // 取出缓存
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.userName.setText(msgObject.getUserName());
        viewHolder.msg.setText(msgObject.getMsg());

        return view;
    }

    // 内部类
    class ViewHolder{
        TextView userName;
        TextView msg;
    }
}
