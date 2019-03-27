import org.jgroups.*;
import org.jgroups.util.Util;

import java.io.*;
import java.util.HashMap;
import java.util.List;

public class DistributedMap extends ReceiverAdapter implements SimpleStringMap {
    private final HashMap<String, Integer> hashMap = new HashMap<>();
    private JChannel channel;

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized(hashMap) {
            Util.objectToStream(hashMap, new DataOutputStream(output));
        }
    }

    public void setState(InputStream input) throws Exception {
        HashMap<String, Integer> map;
        map =(HashMap<String, Integer>)Util.objectFromStream(new DataInputStream(input));
        synchronized(hashMap) {
            hashMap.clear();
            hashMap.putAll(map);
        }
        System.out.println("Got the hashmap, values:");
        hashMap.entrySet().forEach(System.out::println);
    }

    public void start(String channelName) throws Exception {
        channel=new JChannel();
        channel.setReceiver(this);
        channel.connect(channelName);
        channel.getState(null, 10000);
    }

    public void printAll() {
        hashMap.entrySet().forEach(System.out::println);
    }

    public void end(){
        channel.close();
    }


    public void send(String msg) throws Exception{
        channel.send(encodeMsg(msg));
    }


    @Override
    public void viewAccepted(View new_view) {
        handleView(channel, new_view);
    }

    @Override
    public void receive(Message msg) {
        Wrapper wrapper = (Wrapper) msg.getObject();
        if (msg.getSrc() != channel.getAddress()) {
            switch (wrapper.getType()) {
                case put:
                    synchronized (hashMap) {
                        hashMap.put(wrapper.getKey(), wrapper.getValue());
                    }
                    break;
                case remove:
                    synchronized (hashMap) {
                        hashMap.remove(wrapper.getKey());
                    }
                    break;
            }
        }

    }

    public boolean containsKey(String key){
        return hashMap.containsKey(key);
    }

    public Integer get(String key){
        return hashMap.get(key);
    }

    public void put(String key, Integer value) {
        synchronized (hashMap) {
            hashMap.put(key, value);
        }
        try {
            channel.send(encodeMsg("put " + key + " " + value));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Integer remove(String key){
        Integer rm;
        synchronized (hashMap) {
            rm = hashMap.remove(key);
        }
        try {
            channel.send(encodeMsg("remove " + key));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return rm;
    }

    private Message encodeMsg(String msg){
        if(msg.split(" ")[0].equals("put")){
            Wrapper wrapper = new Wrapper(Type.put, msg.split(" ")[1], Integer.valueOf(msg.split(" ")[2]));
            return new Message(null, wrapper);
        }
        else if(msg.split(" ")[0].equals("remove")){
            Wrapper wrapper = new Wrapper(Type.remove, msg.split(" ")[1]);
            return new Message(null, wrapper);
        }
        return new Message(null, null);
    }

    private static void handleView(JChannel ch, View new_view) {
        //System.out.println("Received view " + new_view);
        if(new_view instanceof MergeView) {
            ViewHandler handler=new ViewHandler(ch, (MergeView)new_view);
            // requires separate thread as we don't want to block JGroups
            handler.start();
        }
    }

    private static class ViewHandler extends Thread {
        JChannel ch;
        MergeView view;

        private ViewHandler(JChannel ch, MergeView view) {
            this.ch = ch;
            this.view = view;
        }

        public void run() {
            List<View> subgroups = view.getSubgroups();
            View tmp_view = subgroups.get(0); // picks the first
            Address local_addr = ch.getAddress();
            if (!tmp_view.getMembers().contains(local_addr)) {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will re-acquire the state");
                try {
                    ch.getState(null, 30000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            } else {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will do nothing");
            }
        }
    }
}
