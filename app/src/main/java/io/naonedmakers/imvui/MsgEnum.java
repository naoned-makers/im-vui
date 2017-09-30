package io.naonedmakers.imvui;

/**
 * Created by dbatiot on 22/09/17.
 */

public enum  MsgEnum {
    MSG_HOT_DETECTED,
    MSG_HOT_ERROR,
    MSG_HOT_LEVEL,
    MSG_STT_TEXT,
    MSG_STT_ERROR,
    MSG_STT_LEVEL,
    MSG_TTS_START,
    MSG_TTS_PARTIAL_DONE,
    MSG_TTS_FINAL_DONE;

    public static MsgEnum getMsgEnum(int i) {
        return MsgEnum.values()[i];
    }
}
