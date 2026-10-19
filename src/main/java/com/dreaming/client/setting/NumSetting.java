package com.dreaming.client.setting;

public class NumSetting extends Setting {
    public double value;
    public final double min, max, step;

    public NumSetting(String name, double def, double min, double max, double step) {
        super(name);
        this.value = def; this.min = min; this.max = max; this.step = step;
    }

    public void setFromFraction(double f) {
        f = Math.max(0, Math.min(1, f));
        double v = min + f * (max - min);
        value = Math.max(min, Math.min(max, Math.round(v / step) * step));
    }

    public double fraction() { return (value - min) / (max - min); }
}
