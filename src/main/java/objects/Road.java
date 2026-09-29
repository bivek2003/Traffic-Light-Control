package objects;

import devices.Direction;
import java.util.Objects;

public enum Road {
    NS, EW;

    public static Road from(Direction direction) {
        Objects.requireNonNull(direction);
        return direction == Direction.N || direction == Direction.S ? NS : EW;
    }

    public Phase through() {
        return this == NS ? Phase.NS_THROUGH : Phase.EW_THROUGH;
    }

    public Phase left() {
        return this == NS ? Phase.NS_LEFT : Phase.EW_LEFT;
    }
}
