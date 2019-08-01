package com.vidyo.vidyoconnector;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.HashMap;
import java.util.List;

public class ParticipantFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View V = inflater.inflate(R.layout.fragment_participant_list, container, false);

        CallFragment cf = ((CallFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Call"));
        HashMap<String,Participant> participants = cf.getParticipants();
        for(Participant p : participants.values()){
            TableLayout tl = V.findViewById(R.id.participant_table);
            TableRow tr = new TableRow(getActivity());

            TextView tv = new TextView(getActivity());
            tv.setText(p.name);
            tr.setTag(p.id);
            tr.addView(tv);

            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
            tl.addView(tr);
        }
        return V;
    }
    public void addParticipant(Participant participant){
        String name = participant.name;
        TableLayout tl = getView().findViewById(R.id.participant_table);
        TableRow tr = new TableRow(getActivity());
        tr.setTag(participant.id);
        TextView tv = new TextView(getActivity());
        tv.setText(name);
        tr.addView(tv);
        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        tl.addView(tr);
    }
    public void removeParticipant(Participant participant){
        View tr = getView().findViewWithTag(participant.id);

        ((ViewGroup) tr.getParent()).removeView(tr);
    }

}