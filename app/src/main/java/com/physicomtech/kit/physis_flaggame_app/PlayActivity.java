package com.physicomtech.kit.physis_flaggame_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.physicomtech.kit.physis_flaggame_app.helper.PlayManager;
import com.physicomtech.kit.physis_flaggame_app.helper.TextSpeech;
import com.physicomtech.kit.physislibrary.PHYSIsBLEActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PlayActivity extends PHYSIsBLEActivity {

    // region Check Bluetooth Permission
    private static final int REQ_APP_PERMISSION = 1000;
    private static final List<String> appPermissions
            = Collections.singletonList(Manifest.permission.ACCESS_COARSE_LOCATION);

    /*
        # 애플리케이션의 정상 동작을 위한 권한 체크
        - 안드로이드 마시멜로우 버전 이상에서는 일부 권한에 대한 사용자의 허용이 필요
        - 권한을 허용하지 않을 경우, 관련 기능의 정상 동작을 보장하지 않음.
        - 권한 정보 URL : https://developer.android.com/guide/topics/security/permissions?hl=ko
        - PHYSIs Maker Kit에서는 블루투스 사용을 위한 위치 권한이 필요.
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> reqPermissions = new ArrayList<>();
            for(String permission : appPermissions){
                if(checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED){
                    reqPermissions.add(permission);
                }
            }
            if(reqPermissions.size() != 0){
                requestPermissions(reqPermissions.toArray(new String[reqPermissions.size()]), REQ_APP_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQ_APP_PERMISSION){
            boolean accessStatus = true;
            for(int grantResult : grantResults){
                if(grantResult == PackageManager.PERMISSION_DENIED)
                    accessStatus = false;
            }
            if(!accessStatus){
                Toast.makeText(getApplicationContext(), "위치 권한 거부로 인해 애플리케이션을 종료합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    // endregion

    private final String SERIAL_NUMBER = "XXXXXXXXXXXX";            // PHYSIs Maker Kit 시리얼번호

    Button btnConnect, btnDisconnect, btnStart;                     // 액티비티 위젯
    TextView tvScore, tvFlagState, tvTotalRound, tvRoundTime;
    ProgressBar pgbConnect;

    private PlayManager playManager;                                // 게임 관리 클래스
    private TextSpeech textSpeech;                                  // 음성 출력 클래스

    private boolean isConnected = false;                            // BLE 연결 상태 변수
    private boolean isPlaying = false;                              // 게임 동작 상태 변수

    private int totalRound = 10;                                    // 총 라운드 수
    private int roundTime = 3000;                                   // 라운드 진행(유지) 시간
    private int playRound = 1;                                      // 현재 게임 라운드

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        checkPermissions();                 // 앱 권한 체크 함수 호출
        init();                             // 위젯 및 기능 클래스 생성 함수 호출
        setEventListener();                 // 이벤트 리스너 설정 함수 호출
    }

    /*
        # 위젯 및 기능 클래스 생성/초기화
     */
    private void init() {
        playManager = new PlayManager(physisHandle);            // 게임 관리 클래스 생성
        playManager.setTotalRound(totalRound);                  // 게임 라운드 설정
        playManager.setRoundTime(roundTime);

        textSpeech = new TextSpeech(getApplicationContext(),
                Locale.KOREA, 1.0f, 1.7f);   // 스피치 클래스 생성

        btnConnect = findViewById(R.id.btn_connect);            // 버튼 생성
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnStart = findViewById(R.id.btn_start);
        pgbConnect = findViewById(R.id.pgb_connect);            // 프로그래스 생성
        tvScore = findViewById(R.id.tv_score);                  // 텍스트 뷰 생성
        tvFlagState = findViewById(R.id.tv_flag_state);
        tvTotalRound = findViewById(R.id.tv_total_round);
        tvRoundTime = findViewById(R.id.tv_round_time);

        tvTotalRound.setText(String.valueOf(totalRound));
        tvRoundTime.setText(String.valueOf(roundTime));
    }

    /*
        # 뷰 (버튼) 이벤트 리스너 설정
     */
    private void setEventListener() {
        btnConnect.setOnClickListener(new View.OnClickListener() {              // 연결 버튼
            @Override
            public void onClick(View v) {                   // 버튼 클릭 시 호출
                btnConnect.setEnabled(false);                       // 연결 버튼 비활성화 설정
                pgbConnect.setVisibility(View.VISIBLE);             // 연결 프로그래스 가시화 설정
                connectDevice(SERIAL_NUMBER);                       // PHYSIs Maker Kit BLE 연결 시도
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {           // 연결 종료 버튼
            @Override
            public void onClick(View v) {
                disconnectDevice();                             // PHYSIs Maker Kit BLE 연결 종료
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {                // 게임 시작 버튼
            @Override
            public void onClick(View v) {
                if(isConnected) {                                 // BLE 연결 상태일 경우
                    playRound = 1;                                  // 게임 라운드 초기화
                    playManager.start();                            // 게임 시작 함수 호출
                }
            }
        });
    }

    /*
       # BLE 연결 결과 수신
       - 블루투스 연결에 따른 결과를 전달받을 때 호출 (BLE 연결 상태가 변경됐을 경우)
       - 연결 결과 : CONNECTED(연결 성공), DISCONNECTED(연결 종료/실패), NO_DISCOVERY(디바이스 X)
     */
    @Override
    protected void onBLEConnectedStatus(int result) {
        super.onBLEConnectedStatus(result);
        setConnectedResult(result);                             // BLE 연결 결과 처리 함수 호출
    }

    /*
        # BLE 연결 결과 처리
     */
    private void setConnectedResult(int result){
        pgbConnect.setVisibility(View.INVISIBLE);               // 연결 프로그래스 비가시화 설정
        isConnected = result == CONNECTED;              // 연결 결과 확인

        String toastMsg;                                        // 연결 결과에 따른 Toast 메시지 출력
        if(result == CONNECTED){
            toastMsg = "Physi Kit와 연결되었습니다.";
        }else if(result == DISCONNECTED){
            toastMsg = "Physi Kit 연결이 실패/종료되었습니다.";
        }else{
            toastMsg = "연결할 Physi Kit가 존재하지 않습니다.";
        }
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();

        btnConnect.setEnabled(!isConnected);                     // 연결 버튼 활성화 상태 설정
        btnDisconnect.setEnabled(isConnected);
    }

    /*
        # BLE 메시지 수신
        - 연결된 PHYSIs Maker Kit로부터 BLE 메시지 수신 시 호출
     */
    @Override
    protected void onBLEReceiveMsg(String msg) {
        super.onBLEReceiveMsg(msg);
        if(isPlaying){                                         // 게임이 진행 중일 경우
            playManager.compareFlagState(msg);                      // 깃발상태 비교 함수 호출
        }
    }

    /*
        # 핸들러 메시지 수신
        - PlayManager로부터 전달되는 메시지 수신 및 기능(데이터 출력 및 제어) 처리
     */
    @Override
    protected void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what){
            case PlayManager.PLAY_START:                    // 게임 시작 시,
                btnStart.setEnabled(false);                     // 시작 버튼 비활성화
                isPlaying = true;                               // 게임 동작 상태로 전환
                break;
            case PlayManager.PLAY_STOP:                     // 게임 종료 시,
                btnStart.setEnabled(true);                      // 시작 버튼 활성화
                isPlaying = false;                              // 게임 준비 상태로 전환
                break;
            case PlayManager.PLAY_FLAG:                     // 게임 데이터 수신 시,
                // 깃발 움직임 및 라운드 Text 생성 및 출력
                String data = "Round " + playRound++ + "\n" + msg.obj;
                tvFlagState.setText(data);
                textSpeech.speak(msg.obj.toString());           // 깃발 움직임 음성 출력
                break;
            case PlayManager.PLAY_SCORE:                    // 게임 점수 수신 시,
                tvScore.setText(msg.obj.toString());             // 게임 스코어 출력
                break;
        }
    }
}
