package motionlogger_app.bigdata.net.motionloggerapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import net.bigdata.motionlogger.MotionLogger;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    // User states enumeration
    public enum UserState {
        SCREEN_ON("SCREEN_ON"),
        SCREEN_OFF("SCREEN_OFF");

        private final String text;

        UserState(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /** A messenger for receiving messages from the service */
    private final Messenger callbackMessenger = new Messenger(new CallbackHandler(this));

    /** A messenger for sending messages to the service */
    private Messenger sensorLoggerService;

    /** A broadcast receiver for screen off detection */
    private BroadcastReceiver broadcastReceiver;

    private KinesisClient kinesisClient;

    /**
     * Call-back handler class for communication with the SensorLoggerService
     */
    private static class CallbackHandler extends Handler {
        private MainActivity mainActivity;

        CallbackHandler(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        public void handleMessage(Message msg) {
            final int what = msg.what;
            final Bundle b = msg.getData();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (what == MotionLogger.MSG_FLUSH) {
                        if (b != null) {
                        /*
                        mainActivity.datasetCollector.collectMotion(
                                (HashMap<String, Map<String, List<Double>>>) b.getSerializable(MotionLogger.KEY_DATA));
                        */
                        }
                    }
                }
            }).start();
        }
    }

    private final ServiceConnection motionLoggerConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            sensorLoggerService = new Messenger(service);

            try {
                // Init motion logger's labeling
                setMotionLoggerUserState(UserState.SCREEN_ON);

                // Register client to receive data
                Message message = Message.obtain(null, MotionLogger.MSG_REGISTER_CLIENT);
                message.replyTo = callbackMessenger;
                sensorLoggerService.send(message);

                // Start motion logging
                message = Message.obtain(null, MotionLogger.MSG_START_LOGGING);
                sensorLoggerService.send(message);

                // Create and start data flush periodic task
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Message message = Message.obtain(null, MotionLogger.MSG_FLUSH);

                                /*
                                try {
                                    sensorLoggerService.send(message);
                                } catch (RemoteException e) {}
                                */
                            }
                        }).start();
                    }
                };

                Timer timer = new Timer();
                timer.schedule(timerTask, 500, 500);

            } catch (final RemoteException e) {
                System.out.println(e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            sensorLoggerService = null;
        }
    };

    public void setMotionLoggerUserState(UserState userState) {
        Message message = Message.obtain(null, MotionLogger.MSG_SET_LABEL);
        Bundle dataToSend = new Bundle();
        dataToSend.putString(MotionLogger.KEY_LABEL, userState.toString());
        message.setData(dataToSend);

        try {
            sensorLoggerService.send(message);
        } catch (RemoteException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /// KINESIS EXAMPLE

        // init KinesisClient and collect data
        String accessKey = "AKIAI7DA2HSJKOZ4I55Q";
        String secretKey = "BApT40kO8lbzfu13YPyOn1cuAmYExQcrhtW4JkP6";
        KinesisClient kinesisClient = new KinesisClient(this.getDir("kinesis_data_storage", 0), accessKey, secretKey);
        kinesisClient.collectData("kek".getBytes());

        ///

        // Add screen off/on listener
        /*
        broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        setMotionLoggerUserState(UserState.SCREEN_OFF);
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        setMotionLoggerUserState(UserState.SCREEN_ON);
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(broadcastReceiver, filter);

        // Bind motion logger service
        Intent motionLogger = new Intent(MainActivity.this, MotionLogger.class);
        motionLogger.putExtra(MotionLogger.KEY_DATA_REPR, MotionLogger.SensorDataRepr.DOUBLE_LIST.getValue());
        startService(motionLogger);
        bindService(motionLogger, motionLoggerConnection, Context.BIND_AUTO_CREATE);
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unbindService(motionLoggerConnection);
    }
}
