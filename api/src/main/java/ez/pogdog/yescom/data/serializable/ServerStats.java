package ez.pogdog.yescom.data.serializable;

import ez.pogdog.yescom.data.ISerializable;

import java.math.BigInteger;

public class ServerStats implements ISerializable {

    private final long time;

    private final float TPS;
    private final float PING;
    private final int TSLP;

    public ServerStats(long time, float TPS, float PING, int TSLP) {
        this.time = time;

        this.TPS = TPS;
        this.PING = PING;
        this.TSLP = TSLP;
    }

    public ServerStats(float TPS, float PING, int TSLP) {
        this(System.currentTimeMillis(), TPS, PING, TSLP);
    }

    @Override
    public String toString() {
        return String.format("ServerStats(TPS=%s, PING=%s, TSLP=%s, TIME=%s)",
                TPS, PING, TSLP, time);
    }

    public long getTime() {
        return time;
    }

    public float getTPS() {
        return TPS;
    }

    public float getPING() {
        return PING;
    }

    public float getTSLP() {
        return TSLP;
    }

    @Override
    public BigInteger getID() {
        return null;
    }
}