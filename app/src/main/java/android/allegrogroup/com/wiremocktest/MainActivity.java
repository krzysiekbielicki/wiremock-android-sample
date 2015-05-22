package android.allegrogroup.com.wiremocktest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.android.AndroidLog;
import retrofit.http.GET;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                RestAdapter adapter = new RestAdapter.Builder()
                        .setEndpoint("http://google.com")
                        .setLogLevel(LogLevel.FULL)
                        .setLog(new AndroidLog("RETORFIT"))
                        .build();
                PingService pingService = adapter.create(PingService.class);

                // when
                pingService.ping().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {

                    @Override
                    public void call(String s) {
                        ((TextView) findViewById(R.id.textView)).setText(s);
                    }
                });
            }
        });
    }

    public interface PingService {

        @GET("/ping")
        Observable<String> ping();
    }
}
