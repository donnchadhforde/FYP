package com.example.donnchadhforde.fyp;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class FragmentTest extends Fragment {

    private static final String TAG = "Assessment";
    private static final String ARG_PARAM1 = "btData";

    private BluetoothService btService;
    private Handler handler;

    private Button sendMessage;
    private Button finalOrientation;
    private TextView btLabel;

    public static FragmentTest newInstance(String param1) {
        FragmentTest fragment = new FragmentTest();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Assess", "launched");
        btService = ((MainActivity)getActivity()).getBtService();
        handler = ((MainActivity) getActivity()).mHandler;

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.assessment_fragment, container, false);
        btLabel = view.findViewById(R.id.bt_label);
        sendMessage = view.findViewById(R.id.send_data);
        finalOrientation = view.findViewById(R.id.measure_rom);
        String test = getArguments().getString(ARG_PARAM1);

        btLabel.setText(test);

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            btService.write(Constants.INITIAL_ORIENTATION);
            btLabel.setText(getString(R.string.measure_orientation));
            sendMessage.setVisibility(v.INVISIBLE);
            finalOrientation.setVisibility(v.VISIBLE);

            }
        });

        finalOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btService.write(Constants.FINAL_ORIENTATION);
                btLabel.setText(getString(R.string.measure_orientation) + " ..final");
            }
        });

        return view;
    }
}
