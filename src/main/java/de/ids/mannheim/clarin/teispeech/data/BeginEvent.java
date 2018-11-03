package de.ids.mannheim.clarin.teispeech.data;

/**
 * an {@link Event} starting a turn
 *
 * @author bfi
 */
public class BeginEvent extends Event {
    public BeginEvent() {
        nr = ++lastEvent;
    }

    @Override
    public String mkTime() {
        return "B_" + Integer.toString(nr);
    }

}
