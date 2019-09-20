package com.physicomtech.kit.physis_flaggame_app.helper;

import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class PlayManager {

    private static final String TAG = "PlayManager";

    public static final int PLAY_START = 221;                   // Handler Message ID
    public static final int PLAY_STOP = 222;
    public static final int PLAY_FLAG = 223;
    public static final int PLAY_SCORE = 224;

    private List<String> flagStates = new ArrayList<>();;       // 깃발 상태 리스트
    private List<String> flagMsgs = new ArrayList<>();;         // 깃발 상태 메시지 리스트
    private Handler handler;                                    // Handler
    private Timer playTimer;                                    // 게임 진행 타이머

    private int roundTime = 3000;                               // 라운드 진행 시간
    private int totalRound = 10;                                // 총 게임 라운드
    private int playingCount = 0;                               // 현재 게임 라운드
    private String flagState;                                   // 현재 라운드의 깃발 상태
    private int score;                                          // 게임 점수
    private long startTime;                                     // 라운드 시작 시작 ( 게임 점수 환산을 위한 변수 )
    private boolean isStayed = false;                           // 특정 상태("33", 깃발 상태 수신하지 않는 상태) 처리를 위한 변수

    public PlayManager(Handler handler){
        this.handler = handler;
        setFlagIGametems();
    }

    /*
        # 깃발 게임 항목 설정
     */
    private void setFlagIGametems() {
        // UP = 1 / DOWN = 2 / MID = 3
        flagStates.add("11");
        flagMsgs.add("백기 올리고 청기 올려");
        flagStates.add("13");
        flagMsgs.add("백기 올리고 청기 올리지마");
        flagStates.add("13");
        flagMsgs.add("백기 올리고 청기 내리지마");
        flagStates.add("12");
        flagMsgs.add("백기 올리고 청기 내려");

        flagStates.add("31");
        flagMsgs.add("백기 올리지말고 청기 올려");
        flagStates.add("31");
        flagMsgs.add("백기 내리지말고 청기 올려");
        flagStates.add("33");
        flagMsgs.add("백기 올리지말고 청기 올리지마");
        flagStates.add("33");
        flagMsgs.add("백기 내리지말고 청기 내리지마");
        flagStates.add("33");
        flagMsgs.add("백기 내리지말고 청기 올리지마");
        flagStates.add("33");
        flagMsgs.add("백기 올리지말고 청기 내리지마");
        flagStates.add("32");
        flagMsgs.add("백기 올리지말고 청기 내려");
        flagStates.add("32");
        flagMsgs.add("백기 내리지말고 청기 내려");

        flagStates.add("21");
        flagMsgs.add("백기 내리고 청기 올려");
        flagStates.add("23");
        flagMsgs.add("백기 내리고 청기 올리지마");
        flagStates.add("23");
        flagMsgs.add("백기 내리고 청기 내리지마");
        flagStates.add("22");
        flagMsgs.add("백기 내리고 청기 내려");
    }

    /*
        # 라운드 진행 시간 설정
     */
    public void setRoundTime(int time){
        roundTime = time;
    }

    /*
        # 총 게임 라운드 설정
     */
    public void setTotalRound(int totalRound){
        this.totalRound = totalRound;
    }

    /*
        # 다음 라운드 진행
        - 현재 플레이 타이머(라운드)를 종료하고 다음 플레이 타이머 실행
     */
    private void nextFlag(){
        playTimer.cancel();
        playTimer = null;
        playTimer = new Timer();
        playTimer.schedule(roundTask(), 0, roundTime);
    }

    /*
        # 게임 시작
        - 게임 상태 변수 초기화
        - 플레이 타이머 실행
     */
    public void start(){
        playingCount = 0;
        score = 0;
        isStayed = false;

        playTimer = new Timer();
        playTimer.schedule(roundTask(), 0, roundTime);
        handler.obtainMessage(PLAY_START).sendToTarget();                   // 플레이 액티비티로 게임 시작 메시지 전송
    }

    /*
       # 게임 종료
       - 플레이 타이머 종료
    */
    public void stop(){
        playTimer.cancel();
        playTimer = null;
        handler.obtainMessage(PLAY_STOP).sendToTarget();                    // 플레이 액티비티로 게임 종료 메시지 전송
    }

    /*
        # 게임 라운드 실행
     */
    private TimerTask roundTask(){
        return new TimerTask() {
            @Override
            public void run() {
                if(isStayed){                                       // 특정 상태에 따른 점수 합산
                    score += 500;
                    isStayed = false;
                }

                if(playingCount == totalRound){                     // 현재 라운드가 총 라운드와 같을 경우
                    stop();                                             // 게임 종료
                }else{                                              // 현재 라운드가 총 라운드보다 작을 경우
                    playingCount++;                                     // 라운드 수 증가
                    startTime = System.currentTimeMillis();             // 라운드 시작 시간(ms) 저장
                    sendFlagState();                                    // 깃발 상태 전송
                }
                sendPlayScore();                                    // 게임 점수 전송
            }
        };
    }

    /*
        # 깃발 상태 설정 및 전송
        - 임의의 깃발 상태 설정하고 상태메시지를 핸들러를 통해 플레이 액티비티로 전송
     */
    private void sendFlagState(){
        int itemIndex = new Random().nextInt(flagStates.size());    // 임의의 인덱스 설정
        flagState = flagStates.get(itemIndex);                      // 현재 라운드의 깃발 상태를 저장
        if(flagState.equals("33"))
            isStayed = true;
        handler.obtainMessage(PLAY_FLAG, flagMsgs.get(itemIndex)).sendToTarget();
    }

    /*
        # 플레이 점수 전송
        - 핸들러를 통해 현재 점수를 플레이 액티비티로 전송
     */
    private void sendPlayScore(){
        handler.obtainMessage(PLAY_SCORE, score).sendToTarget();
    }

    /*
        # 깃발 상태 비교
        - 현재 라운드의 깃발 상태와 PHYSIs Maker Kit로부터 수신받은 제어 메시지를 비교
        - 깃발 상태와 제어 메시지가 일치할 경우,
        - 라운드 시작 시간(ms)과 데이터 수신 시간에 따른 라운드 점수를 환산하여 총점에 합산
     */
    public void compareFlagState(String state){
        if(isStayed)
            isStayed = false;

        if(state.equals(flagState)){
            score += roundTime + startTime - System.currentTimeMillis();
            nextFlag();
        }
    }
}
