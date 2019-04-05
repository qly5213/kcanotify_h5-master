package com.antest1.kcanotify.h5;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatAdapter extends ArrayAdapter {
    private int resourceId;
    public ChatAdapter(@NonNull Context context, int resource, @NonNull List<ChatMsgObject> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
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
            viewHolder.imageMsg = (ImageView) view.findViewById(R.id.chat_image_msg);
            view.setTag(viewHolder);
        }else{
            view = convertView;
            // 取出缓存
            viewHolder = (ViewHolder) view.getTag();
        }

        if(msgObject.getMsgType() == ChatMsgObject.MsgTypeCont.MSG_TEXT) {
            viewHolder.userName.setText(msgObject.getUserName());
            viewHolder.msg.setText(msgObject.getMsg());
            viewHolder.msg.setVisibility(View.VISIBLE);
            viewHolder.imageMsg.setVisibility(View.GONE);
        } else if(msgObject.getMsgType() == ChatMsgObject.MsgTypeCont.MSG_IMAGE) {
            viewHolder.userName.setText(msgObject.getUserName());
            Bitmap bitmap = PictureUtils.base64ToBitmap(msgObject.getImageMsg());
            viewHolder.imageMsg.setImageBitmap(bitmap);
            viewHolder.msg.setVisibility(View.GONE);
            viewHolder.imageMsg.setVisibility(View.VISIBLE);
            viewHolder.imageMsg.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                   ChatDialogUtils.saveBmpToSd(file.getAbsolutePath(), PictureUtils.base64ToBitmap(msgObject.getImageMsg()), sdf.format(new Date()) + ".jpg", 100, true);
                    Toast.makeText(KcaApplication.getInstance(), "已保存文件到下载目录", Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }

        return view;
    }

    // 内部类
    class ViewHolder{
        TextView userName;
        TextView msg;
        ImageView imageMsg;
    }
}
