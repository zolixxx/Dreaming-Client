package com.dreaming.client.gui;

/** Frame-rate independent exponential easing toward a target value. */
public final class Smooth {
    public float cur, target;
    private final float speed;

    public Smooth(float value, float speed) {
        this.cur = this.target = value;
        this.speed = speed;
    }

    public void update(float dt) {
        cur += (target - cur) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - cur) < 0.0005f) cur = target;
    }

    public void snap(float v) { cur = target = v; }
}
