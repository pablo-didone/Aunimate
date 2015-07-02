package mubbi.aunimate.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import mubbi.aunimate.interfaces.PlayPauseListener;
import java.util.ArrayList;

import mubbi.aunimate.R;

import mubbi.aunimate.model.Sound;

public class SoundAdapter extends ArrayAdapter<Sound> {

    private Activity context;
    private ArrayList<Sound> data;
    private PlayPauseListener listener;
    private int playedButton = -1;


    public SoundAdapter(Activity context, ArrayList<Sound> data) {
        super(context, R.layout.list_item_sound, data);
        this.context = context;
        this.data = data;
    }

    public void attachListener(PlayPauseListener listener){
        this.listener = listener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View item = convertView;
        final ViewHolder holder;

        if (item == null){
            LayoutInflater inflater = context.getLayoutInflater();
            item = inflater.inflate(R.layout.list_item_sound, null);

            holder = new ViewHolder();
            holder.soundName = (TextView)item.findViewById(R.id.lblSoundName);
            holder.soundId = (TextView)item.findViewById(R.id.lblSoundId);
            holder.playSound = (ImageButton)item.findViewById(R.id.btnPlay);
            holder.pauseSound = (ImageButton)item.findViewById(R.id.btnPause);

            item.setTag(holder);
        }else{
            holder = (ViewHolder)item.getTag();
        }

        holder.soundName.setText(data.get(position).getAutor() + " - " + data.get(position).getTitle());
        holder.soundId.setText(data.get(position).getId());
        holder.playSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onPlaySound(data.get(position).getId());
                //ListView listView  = (ListView)v.getParent().getParent();
                //listView.getChildAt(position).findViewById(R.id.btnPlay).setVisibility(View.GONE);
                //listView.getChildAt(position).findViewById(R.id.btnPause).setVisibility(View.VISIBLE);
            }
        });
        holder.pauseSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onPauseSound();
            }
        });


        return item;
    }

    static class ViewHolder{
        TextView soundName;
        TextView soundId;
        ImageButton playSound;
        ImageButton pauseSound;
    }
}
