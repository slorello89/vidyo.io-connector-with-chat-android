package com.vidyo.vidyoconnector;

public class ChatMessage {
    public enum ParticipantType{
        Remote,
        Local
    };
    private ParticipantType type;
    private String name;
    private String message;

    public ChatMessage(ParticipantType type, String name, String message){
        this.type = type;
        this.name = name;
        this.message = message;
    }

    public ParticipantType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString(){
        return String.format("%1$s: %2$s",name,message);
    }
}
