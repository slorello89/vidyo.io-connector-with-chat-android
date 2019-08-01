package com.vidyo.vidyoconnector;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.graphics.drawable.LayerDrawable;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.Activity;


import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Endpoint.ChatMessage;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View V = inflater.inflate(R.layout.fragment_chat, container, false);
        Button button = (Button) V.findViewById(R.id.sendMsgBtn);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                sendMsg(v);
            }
        });
        CallFragment cf = ((CallFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Call"));
        List<com.vidyo.vidyoconnector.ChatMessage> unDisplayedIncomingMessages = cf.getMessages();
        for (com.vidyo.vidyoconnector.ChatMessage message : unDisplayedIncomingMessages){
            TextView txt;
            if (message.getType() == com.vidyo.vidyoconnector.ChatMessage.ParticipantType.Local){
                txt = (TextView) View.inflate(getActivity(),R.layout.text_outgoing_layout,null);
            }
            else{
                txt = (TextView) View.inflate(getActivity(),R.layout.text_incoming_layout,null);
            }

            txt.setText(message.toString());
            LinearLayout ll = (LinearLayout)V.findViewById(R.id.messages_area);
            ll.addView(txt);
        }
        ((ScrollView)V.findViewById(R.id.chat_scroll)).fullScroll(View.FOCUS_DOWN);

        return V;
    }

    public void sendMsg(View v){
        EditText et = (EditText)getView().findViewById(R.id.input_box);
        String text_to_send = et.getText().toString();
        et.setText("");
        if (text_to_send.equals(""))
            return;
        TextView txt = (TextView) View.inflate(getActivity(),R.layout.text_outgoing_layout,null);
        txt.setText("You: " + text_to_send);
        LinearLayout ll = (LinearLayout)getView().findViewById(R.id.messages_area);
        ll.addView(txt);

        CallFragment cf = ((CallFragment)getActivity().getSupportFragmentManager().findFragmentByTag("Call"));
        cf.sendMessage(text_to_send);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        View focused_view = getActivity().getCurrentFocus();
        if (focused_view == null) {
            focused_view = new View(getActivity());
        }
        imm.hideSoftInputFromWindow(focused_view.getWindowToken(), 0);
        scrollToBottom();
        cf.addLocalChatMessage(text_to_send,"You");

    }

    public void receiveChatMessage(Participant participant, ChatMessage chatMessage) {
        if (! (chatMessage.type == ChatMessage.ChatMessageType.VIDYO_CHATMESSAGETYPE_Chat))
            return;
        TextView txt = (TextView) View.inflate(getActivity(),R.layout.text_incoming_layout,null);
        String theText = String.format("%1$s: %2$s",participant.name,chatMessage.body);
        txt.setText(theText);
        LinearLayout ll = (LinearLayout)getView().findViewById(R.id.messages_area);
        ll.addView(txt);
        scrollToBottom();
    }
    private void scrollToBottom(){
        ((ScrollView)getView().findViewById(R.id.chat_scroll)).fullScroll(View.FOCUS_DOWN);
    }
}
