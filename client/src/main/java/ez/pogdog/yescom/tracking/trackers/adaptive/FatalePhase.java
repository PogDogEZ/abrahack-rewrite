package ez.pogdog.yescom.tracking.trackers.adaptive;

import ez.pogdog.yescom.query.IsLoadedQuery;

import java.util.Arrays;
import java.util.List;

public class FatalePhase extends BasicPhase {
    public FatalePhase() {
        super(1150, 20.0f, 20.0f, Arrays.asList(
                new BasicPhase.Offset(1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(-1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(0.0f, 1.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(0.0f, -1.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(4.0f, 0.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(-4.0f, 0.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(0.0f, 4.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(0.0f, -4.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(4.0f, 4.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(-4.0f, -4.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(4.0f, -4.0f, IsLoadedQuery.Result.UNLOADED),
                new BasicPhase.Offset(-4.0f, 4.0f, IsLoadedQuery.Result.UNLOADED)));
    }
}