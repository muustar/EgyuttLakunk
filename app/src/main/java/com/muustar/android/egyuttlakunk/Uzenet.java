package com.muustar.android.egyuttlakunk;

public class Uzenet {
    private String uzenetSzovege;
    private String uId;

    public Uzenet() {
        this.uzenetSzovege = uzenetSzovege;
        this.uId = uId;
    }

    public Uzenet(String uzenetSzovege, String uId) {
        this.uzenetSzovege = uzenetSzovege;
        this.uId = uId;
    }

    public String getUzenetSzovege() {
        return uzenetSzovege;
    }

    public void setUzenetSzovege(String uzenetSzovege) {
        this.uzenetSzovege = uzenetSzovege;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }
}
