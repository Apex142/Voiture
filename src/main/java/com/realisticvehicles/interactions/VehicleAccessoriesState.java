package com.realisticvehicles.interactions;

/**
 * Electrical and comfort accessories state (lights, turn signals, hazards, horn).
 */
public final class VehicleAccessoriesState {
    private boolean headlightsOn;
    private boolean highBeamsOn;
    private boolean leftIndicator;
    private boolean rightIndicator;
    private boolean hazardsOn;
    private boolean hornPressed;

    public boolean headlightsOn() {
        return headlightsOn;
    }

    public boolean highBeamsOn() {
        return highBeamsOn;
    }

    public boolean leftIndicator() {
        return leftIndicator || hazardsOn;
    }

    public boolean rightIndicator() {
        return rightIndicator || hazardsOn;
    }

    public boolean hazardsOn() {
        return hazardsOn;
    }

    public boolean hornPressed() {
        return hornPressed;
    }

    public void toggleHeadlights() {
        if (!headlightsOn) {
            headlightsOn = true;
            highBeamsOn = false;
        } else if (!highBeamsOn) {
            highBeamsOn = true;
        } else {
            headlightsOn = false;
            highBeamsOn = false;
        }
    }

    public void toggleLeftIndicator() {
        leftIndicator = !leftIndicator;
        if (leftIndicator) rightIndicator = false;
    }

    public void toggleRightIndicator() {
        rightIndicator = !rightIndicator;
        if (rightIndicator) leftIndicator = false;
    }

    public void toggleHazards() {
        hazardsOn = !hazardsOn;
    }

    public void setHornPressed(boolean pressed) {
        this.hornPressed = pressed;
    }
}
