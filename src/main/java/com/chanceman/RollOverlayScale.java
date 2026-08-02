package com.chanceman;

import lombok.Getter;

public enum RollOverlayScale
{
    SCALE_100("100% (Default)", 1.00f),
    SCALE_125("125%", 1.25f),
    SCALE_150("150%", 1.50f),
    SCALE_175("175%", 1.75f),
    SCALE_200("200%", 2.00f),
    SCALE_250("250%", 2.50f),
    SCALE_300("300%", 3.00f),
    SCALE_400("400%", 4.00f);

    private final String displayName;
    @Getter
    private final float scale;

    RollOverlayScale(String displayName, float scale)
    {
        this.displayName = displayName;
        this.scale = scale;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}