package ez.pogdog.yescom.tracking.trackers.adaptive;

import ez.pogdog.yescom.query.IsLoadedQuery;

import java.util.Arrays;
import java.util.List;

public class GezaPhase extends BasicPhase {
    public GezaPhase() {
        super(1500, 6.0f, 6.0f, Arrays.asList(
                new BasicPhase.Offset(1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(-1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(0.0f, 1.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(0.0f, -1.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(3.0f, 0.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(-3.0f, 0.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(0.0f, 3.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(0.0f, -3.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(3.0f, 3.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(-3.0f, -3.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(3.0f, -3.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(-3.0f, 3.0f, IsLoadedQuery.Result.UNLOADED)));
    }
}
