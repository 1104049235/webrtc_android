package com.dds.skywebrtc.engine;


import android.view.View;

import java.util.List;

/**
 * rtc基类
 */
public interface IEngine {


    /**
     * 初始化
     */
    void init(EngineCallback callback);

    /**
     * 加入房間
     */
    void joinRoom(List<String> userIds);

    /**
     * 有人进入房间
     */
    void userIn(String userId);


    /**
     * receive Offer
     */
    void receiveOffer(String userId, String description);

    /**
     * receive Answer
     */
    void receiveAnswer(String userId, String sdp);

    /**
     * receive IceCandidate
     */
    void receiveIceCandidate(String userId, String id, int label, String candidate);

    /**
     * 离开房间
     */
    void leaveRoom();

    /**
     * 开启本地预览
     */
    View startPreview(boolean isOverlay);

    /**
     * 关闭本地预览
     */
    void stopPreview();

    /**
     * 开始远端推流
     */
    void startStream();

    /**
     * 停止远端推流
     */
    void stopStream();

    /**
     * 开始远端预览
     */
    View setupRemoteVideo(boolean isOverlay);

    /**
     * 关闭远端预览
     */
    void stopRemoteVideo();


    /**
     * 释放所有内容
     */
    void release();

}
