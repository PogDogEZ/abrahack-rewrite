package ez.pogdog.yescom.tracking.trackers.adaptive;

import ez.pogdog.yescom.query.IsLoadedQuery;

import java.util.Arrays;
import java.util.List;

public class WalkingPhase extends BasicPhase {

    public WalkingPhase() {
        super(2000, 1.0f, 1.0f, Arrays.asList(
                new BasicPhase.Offset(1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(-1.0f, 0.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(0.0f, 1.0f, IsLoadedQuery.Result.LOADED),
                new BasicPhase.Offset(0.0f, -1.0f, IsLoadedQuery.Result.LOADED)
        ));
    }
}
