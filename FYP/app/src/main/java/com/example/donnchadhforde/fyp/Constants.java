package com.example.donnchadhforde.fyp;

public interface Constants {

    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_TOAST = 3;
    public static final int MESSAGE_STATE_CHANGE = 4;
    public static final int MESSAGE_DEVICE_NAME = 5;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    //Bluetooth Message Constants
    public static final byte[] INITIAL_ORIENTATION = {'1'};
    public static final byte[] FINAL_ORIENTATION = {'2'};
    public static final byte[] CALIBRATE = {'3'};
    public static final byte[] SESSION_ACTIVE = {'4'};

}
