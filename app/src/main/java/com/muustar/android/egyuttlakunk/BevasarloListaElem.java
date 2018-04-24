package com.muustar.android.egyuttlakunk;

public class BevasarloListaElem {
    private boolean checked;
    private String value;
    private String uId;


    public BevasarloListaElem() {

    }

    public BevasarloListaElem(boolean checked, String value) {
        this.checked = checked;
        this.value = value;

    }

    public BevasarloListaElem(boolean checked, String value, String uId) {
        this.checked = checked;
        this.value = value;
        this.uId = uId;
    }

    public String getUId() {
        return uId;
    }

    public void setUId(String uId) {
        this.uId = uId;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
