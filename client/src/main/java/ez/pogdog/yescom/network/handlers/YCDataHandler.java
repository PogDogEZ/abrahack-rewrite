package ez.pogdog.yescom.network.handlers;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.data.serializable.ChunkState;
import ez.pogdog.yescom.data.serializable.RenderDistance;
import ez.pogdog.yescom.data.serializable.TrackedPlayer;
import ez.pogdog.yescom.network.packets.DataExchangePacket;
import ez.pogdog.yescom.data.serializable.ChatMessage;
import me.iska.jclient.network.Connection;
import me.iska.jclient.network.handler.IHandler;
import me.iska.jclient.network.packet.Packet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class YCDataHandler implements IHandler {

    private final YesCom yesCom = YesCom.getInstance();

    private final Connection connection;

    public YCDataHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handlePacket(Packet packet) {
        if (packet instanceof DataExchangePacket) {
            DataExchangePacket dataExchange = (DataExchangePacket)packet;

            switch (dataExchange.getRequestType()) {
                case DOWNLOAD: {
                    switch (dataExchange.getDataType()) {
                        case TICK_DATA: {
                            long startTime = yesCom.dataHandler.clampStatStartTime(dataExchange.getStartTime()) * yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;
                            long endTime = yesCom.dataHandler.clampStatEndTime(dataExchange.getEndTime()) * yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(), dataExchange.getRequestID(),
                                    yesCom.dataHandler.getTickData(startTime, endTime), startTime, endTime,
                                    yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL));
                            break;
                        }
                        case PING_DATA: {
                            long startTime = yesCom.dataHandler.clampStatStartTime(dataExchange.getStartTime()) * yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;
                            long endTime = yesCom.dataHandler.clampStatEndTime(dataExchange.getEndTime()) * yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(), dataExchange.getRequestID(),
                                    yesCom.dataHandler.getPingData(startTime, endTime), startTime, endTime,
                                    yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL));
                            break;
                        }
                        case TSLP_DATA: {
                            long startTime = yesCom.dataHandler.clampStatStartTime(dataExchange.getStartTime()) * yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;
                            long endTime = yesCom.dataHandler.clampStatEndTime(dataExchange.getEndTime()) * yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL;

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(), dataExchange.getRequestID(),
                                    yesCom.dataHandler.getTSLPData(startTime, endTime), startTime, endTime,
                                    yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL));
                            break;
                        }
                        case ONLINE_PLAYER: {
                            break;
                        }
                        case LOGS: {
                            List<String> logData = new ArrayList<>();
                            List<BigInteger> invalidDataIDs = new ArrayList<>();

                            for (BigInteger logMessageID : dataExchange.getDataIDs()) {
                                String logMessage = yesCom.dataHandler.getLogMessage(logMessageID);

                                if (logMessage == null) {
                                    invalidDataIDs.add(logMessageID);
                                } else {
                                    logData.add(logMessage);
                                }
                            }

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), logData, invalidDataIDs));
                            break;
                        }
                        case CHAT: {
                            List<ChatMessage> chatData = new ArrayList<>();
                            List<BigInteger> invalidDataIDs = new ArrayList<>();

                            for (BigInteger chatMessageID : dataExchange.getDataIDs()) {
                                ChatMessage chatMessage = yesCom.dataHandler.getChatMessage(chatMessageID);

                                if (chatMessage == null) {
                                    invalidDataIDs.add(chatMessageID);
                                } else {
                                    chatData.add(chatMessage);
                                }
                            }

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), chatData, invalidDataIDs));
                            break;
                        }
                        case CHUNK_STATE: {
                            List<ChunkState> chunkStates = new ArrayList<>();
                            List<BigInteger> invalidDataIDs = new ArrayList<>();

                            for (BigInteger chunkStateID : dataExchange.getDataIDs()) {
                                ChunkState chunkState = yesCom.dataHandler.getChunkState(chunkStateID);

                                if (chunkState == null) {
                                    invalidDataIDs.add(chunkStateID);
                                } else {
                                    chunkStates.add(chunkState);
                                }
                            }

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), chunkStates, invalidDataIDs));
                            break;
                        }
                        case RENDER_DISTANCE: {
                            List<RenderDistance> renderDistances = new ArrayList<>();
                            List<BigInteger> invalidDataIDs = new ArrayList<>();

                            for (BigInteger renderDistanceID : dataExchange.getDataIDs()) {
                                RenderDistance renderDistance = yesCom.dataHandler.getRenderDistance(renderDistanceID);

                                if (renderDistance == null) {
                                    invalidDataIDs.add(renderDistanceID);
                                } else {
                                    renderDistances.add(renderDistance);
                                }
                            }

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), renderDistances, invalidDataIDs));
                            break;
                        }
                        case TRACKED_PLAYER: {
                            List<TrackedPlayer> trackedPlayers = new ArrayList<>();
                            List<BigInteger> invalidDataIDs = new ArrayList<>();

                            for (BigInteger trackedPlayerID : dataExchange.getDataIDs()) {
                                TrackedPlayer trackedPlayer = yesCom.dataHandler.getTrackedPlayer(trackedPlayerID);

                                if (trackedPlayer == null) {
                                    invalidDataIDs.add(trackedPlayerID);
                                } else {
                                    trackedPlayers.add(trackedPlayer);
                                }
                            }

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), trackedPlayers, invalidDataIDs));
                            break;
                        }
                        case TRACKING_DATA: {
                            List<TrackedPlayer.TrackingData> trackingDatas = new ArrayList<>();
                            List<BigInteger> invalidDataIDs = new ArrayList<>();

                            for (BigInteger trackedPlayerID : dataExchange.getDataIDs()) {
                                TrackedPlayer trackedPlayer = yesCom.dataHandler.getTrackedPlayer(trackedPlayerID);

                                if (trackedPlayer == null) {
                                    invalidDataIDs.add(trackedPlayerID);
                                } else {
                                    trackingDatas.add(trackedPlayer.getTrackingData());
                                }
                            }

                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), trackingDatas, invalidDataIDs));
                            break;
                        }
                    }
                    break;
                }
                case GET_BOUNDS: {
                    switch (dataExchange.getDataType()) {
                        case TICK_DATA:
                        case PING_DATA:
                        case TSLP_DATA: {
                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), yesCom.dataHandler.getEarliestStatData(),
                                    yesCom.dataHandler.getLatestStatData(), yesCom.configHandler.NUMERICAL_DATA_UPDATE_INTERVAL));
                            break;
                        }
                        case ONLINE_PLAYER: {
                            break;
                        }
                        case LOGS: {
                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), yesCom.dataHandler.getLogID(), BigInteger.ZERO));
                            break;
                        }
                        case CHAT: {
                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), yesCom.dataHandler.getChatID(), BigInteger.ZERO));
                            break;
                        }
                        case CHUNK_STATE: {
                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), yesCom.dataHandler.getChunkStateID(), BigInteger.ZERO));
                            break;
                        }
                        case RENDER_DISTANCE: {
                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), yesCom.dataHandler.getRenderDistanceID(), BigInteger.ZERO));
                            break;
                        }
                        case TRACKED_PLAYER:
                        case TRACKING_DATA: {
                            // TODO: Store the minimum IDs, instead of just assuming 0 as in the future it won't be 0
                            connection.sendPacket(new DataExchangePacket(dataExchange.getDataType(),
                                    dataExchange.getRequestID(), yesCom.dataHandler.getTrackedPlayerID(), BigInteger.ZERO));
                            break;
                        }
                    }
                    break;
                }
                case UPLOAD:
                case SET_BOUNDS: {
                    break;
                }
            }
        }
    }

    @Override
    public void update() {
    }

    @Override
    public void exit(String reason) {
    }
}
