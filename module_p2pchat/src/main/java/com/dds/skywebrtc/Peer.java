package com.dds.skywebrtc;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by dds on 2020/3/11.
 * android_shuai@163.com
 */
public class Peer implements SdpObserver, PeerConnection.Observer {
    private final static String TAG = "dds_Peer";
    private PeerConnection pc;
    private String mUserId;
    private List<IceCandidate> queuedRemoteCandidates;
    private SessionDescription localSdp;
    private PeerConnectionFactory mFactory;
    private List<PeerConnection.IceServer> mIceLis;
    public ExecutorService executor;
    private IPeerEvent mEvent;
    public MediaStream _remoteStream;

    private boolean isOffer;

    public Peer(PeerConnectionFactory factory, List<PeerConnection.IceServer> list, String userId, IPeerEvent event) {
        mFactory = factory;
        mIceLis = list;
        mEvent = event;
        mUserId = userId;
        queuedRemoteCandidates = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
        this.pc = createPeerConnection();


    }

    public PeerConnection createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(mIceLis);
        return mFactory.createPeerConnection(rtcConfig, this);
    }

    public void setOffer(boolean isOffer) {
        this.isOffer = isOffer;
    }

    // 创建offer
    public void createOffer() {
        if (pc == null) return;
        pc.createOffer(this, offerOrAnswerConstraint());
    }

    // 创建answer
    public void createAnswer() {
        if (pc == null) return;
        pc.createAnswer(this, offerOrAnswerConstraint());

    }

    public void setRemoteDescription(SessionDescription sdp) {
        if (pc == null) return;
        pc.setRemoteDescription(this, sdp);
    }

    public void addLocalStream(MediaStream stream) {
        if (pc == null) return;
        pc.addStream(stream);
    }


    public void addRemoteIceCandidate(final IceCandidate candidate) {
        if (pc != null) {
            if (queuedRemoteCandidates != null) {
                queuedRemoteCandidates.add(candidate);
            } else {
                pc.addIceCandidate(candidate);
            }
        }
    }

    public void removeRemoteIceCandidates(final IceCandidate[] candidates) {
        if (pc == null) {
            return;
        }
        drainCandidates();
        pc.removeIceCandidates(candidates);
    }


    public void close() {
        if (pc != null) {
            pc.close();
        }
    }

    public MediaStream getRemoteStream() {
        return _remoteStream;
    }

    //------------------------------Observer-------------------------------------
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        Log.i(TAG, "onIceConnectionReceivingChange:" + receiving);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        Log.i(TAG, "onIceGatheringChange:" + newState.toString());
    }


    @Override
    public void onIceCandidate(IceCandidate candidate) {
        // 发送IceCandidate
        executor.execute(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mEvent.onSendIceCandidate(mUserId, candidate);
        });


    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.i(TAG, "onIceCandidatesRemoved:");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        Log.i(TAG, "onAddStream:");
        stream.audioTracks.get(0).setEnabled(true);
        stream.videoTracks.get(0).setEnabled(true);
        _remoteStream = stream;
        if (mEvent != null) {
            mEvent.onRemoteStream(mUserId, stream);
        }
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.i(TAG, "onRemoveStream:");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.i(TAG, "onDataChannel:");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded:");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        Log.i(TAG, "onAddTrack:" + mediaStreams.length);
    }


    //-------------SdpObserver--------------------
    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        Log.d(TAG, "sdp创建成功       " + origSdp.type);
        String sdpString = origSdp.description;
        final SessionDescription sdp = new SessionDescription(origSdp.type, sdpString);
        localSdp = sdp;
        executor.execute(() -> pc.setLocalDescription(this, sdp));
    }

    @Override
    public void onSetSuccess() {
        executor.execute(() -> {
            Log.d(TAG, "sdp连接成功   " + pc.signalingState().toString());
            if (pc == null) return;
            // 发送者
            if (isOffer) {
                if (pc.getRemoteDescription() == null) {
                    Log.d(TAG, "Local SDP set succesfully");
                    if (!isOffer) {
                        //接收者，发送Answer
                        mEvent.onSendAnswer(mUserId, localSdp);
                    } else {
                        //发送者,发送自己的offer
                        mEvent.onSendOffer(mUserId, localSdp);
                    }
                } else {
                    Log.d(TAG, "Remote SDP set succesfully");

                    drainCandidates();
                }

            } else {
                if (pc.getLocalDescription() != null) {
                    Log.d(TAG, "Local SDP set succesfully");
                    if (!isOffer) {
                        //接收者，发送Answer
                        mEvent.onSendAnswer(mUserId, localSdp);
                    } else {
                        //发送者,发送自己的offer
                        mEvent.onSendOffer(mUserId, localSdp);
                    }

                    drainCandidates();
                } else {
                    Log.d(TAG, "Remote SDP set succesfully");
                }
            }
        });


    }

    @Override
    public void onCreateFailure(String error) {
        Log.i(TAG, " SdpObserver onCreateFailure:" + error);
    }

    @Override
    public void onSetFailure(String error) {
        Log.i(TAG, "SdpObserver onSetFailure:" + error);
    }


    private void drainCandidates() {
        if (queuedRemoteCandidates != null) {
            Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
            for (IceCandidate candidate : queuedRemoteCandidates) {
                pc.addIceCandidate(candidate);
            }
            queuedRemoteCandidates = null;
        }
    }

    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    // ----------------------------回调-----------------------------------

    public interface IPeerEvent {
        void onSendIceCandidate(String userId, IceCandidate candidate);

        void onSendOffer(String userId, SessionDescription description);

        void onSendAnswer(String userId, SessionDescription description);

        void onRemoteStream(String userId, MediaStream stream);

    }

}
