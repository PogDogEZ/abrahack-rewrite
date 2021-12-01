package ez.pogdog.yescom.events.data;

import ez.pogdog.yescom.events.ReporterEvent;
import ez.pogdog.yescom.network.handlers.YCReporter;
import ez.pogdog.yescom.network.packets.shared.DataExchangePacket;

import java.util.ArrayList;
import java.util.List;

public class DataBroadcastEvent extends ReporterEvent { // FIXME: Fix this to work with numeric data too

    private final List<Object> data = new ArrayList<>();

    private final DataExchangePacket.DataType dataType;

    public DataBroadcastEvent(YCReporter reporter, DataExchangePacket.DataType dataType, List<Object> data) {
        super(reporter);

        this.dataType = dataType;
        this.data.addAll(data);
    }

    public List<Object> getData() {
        return new ArrayList<>(data);
    }

    public DataExchangePacket.DataType getDataType() {
        return dataType;
    }
}
