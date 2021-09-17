package ez.pogdog.yescom.tracking;

import ez.pogdog.yescom.YesCom;
import ez.pogdog.yescom.query.IQuery;
import ez.pogdog.yescom.query.IsLoadedQuery;
import ez.pogdog.yescom.util.ChunkPosition;
import ez.pogdog.yescom.util.Dimension;

public class TestCenter {

    private final YesCom yesCom = YesCom.getInstance();

    // Credits to hobrin, im just trying to make it work in yescom

    //TODO: This works (sorta), it is able to get the center of the render distance more accurately the closer your inital hit was
    // to the actual center of the render distance. This obviously needs some work and cleanup, figured id commit if anyone wanted to
    // help since I am gonna be busy

    private ChunkPosition center;
    private final Dimension dimension;
    private static final int rDis = 5;

    public TestCenter(ChunkPosition hit, Dimension dimension) {
        center = hit;
        this.dimension = dimension;

        center(hit);
    }

    public void center(ChunkPosition pos) {
        new Thread(() -> {
            ChunkPosition south = center.add(0,rDis);
            ChunkPosition north = center.add(0,-rDis);

            ChunkPosition southEast = south.add(rDis,0);
            ChunkPosition southWest = south.add(-rDis,0);
            ChunkPosition northEast = north.add(rDis,0);
            ChunkPosition northWest = north.add(-rDis,0);

            ChunkPosition[] off = new ChunkPosition[] {southEast, southWest, northEast, northWest};
            boolean[] loadedMap = new boolean[off.length];

            Counter c = new Counter(off.length);

            for (int i = 0; i < off.length; i++) {
                 ChunkPosition pos1 = off[i];

                final int j = i;

                yesCom.queryHandler.addQuery(new IsLoadedQuery(pos1.getPosition(),
                        dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                        (query, result) -> {
                            yesCom.logger.debug(result);
                            loadedMap[j] = result == IsLoadedQuery.Result.LOADED;
                        c.decrement();
                        synchronized (c) {
                            c.notify();
                        }
                    }));
                };

            try {
                synchronized (c) {
                    while (!c.isReady()) {
                        c.wait();
                    }
                }
            } catch (InterruptedException exc) {
                exc.printStackTrace();
                return;
            }
            /*
             * loaded all the values. Now counting amount of loaded and unloaded chunks.
             */
            int loadedC = 0;
            for (boolean loaded : loadedMap)
                if (loaded)
                    loadedC++;
            int unloaded = off.length - loadedC;

            if (unloaded == 0) {
                setCenter(center);
                yesCom.logger.debug(center.getPosition());
                return; // center still at the same position.
            }
            if (loadedC == 0) {
                //disappeared(); // player can't sit and camp in the middle.
                return;
            }
            if (loadedC == 3) { // out of 4
                // impossible with only 1 player not doing funny chunk loading business. Has to
                // be multiple.
                setCenter(center);
                yesCom.logger.debug(center.getPosition());
                return;
            }
            final int loadedCount = loadedC; // I really do hate this java 'bug'/feature.

            // either 1 or 2 chunks are loaded. Requires middle.
            yesCom.queryHandler.addQuery(new IsLoadedQuery(center.getPosition(),
                    dimension, IQuery.Priority.HIGH, yesCom.configHandler.TYPE,
                    (query, result) -> {
                    boolean loaded = result == IsLoadedQuery.Result.LOADED;

                    if (loadedCount == 1) {

                        ChunkPosition chunk = null;
                        for (int i = 0; i < off.length; i++) {
                            if (loadedMap[i]) {
                                chunk = off[i];
                                break;
                            }
                        }

                        if (loaded) {// both the center and the offset chunk are loaded.

                            setCenter(average(chunk, center));
                            yesCom.logger.debug("avg1 " + center.getPosition());
                        } else {
                            setCenter(chunk); // should actually be even further in that direction.
                            yesCom.logger.debug("noavg " + center.getPosition());
                        }
                        return;
                    } else if (loadedCount == 2) {
                        ChunkPosition first = null;
                        ChunkPosition second = null;

                        for (int i = 0; i < off.length; i++) {
                            if (loadedMap[i]) {
                                if (first == null) {
                                    first = off[i];
                                } else {
                                    second = off[i];
                                    break;
                                }
                            }
                        }
                        // TODO: look if they are opposite and impossible for 1 player.
                        if (loaded) {
                            setCenter(average(first, second, pos));
                            yesCom.logger.debug("avg2 3 elements" + center.getPosition());
                        } else {
                            setCenter(average(first, second));
                            yesCom.logger.debug("avg3 " + center.getPosition());
                        }
                        return;
                    }
                    // unreachable code.
                    throw new RuntimeException();
                })); // t
            }).start();
    }

    private void setCenter(ChunkPosition pos) {
        this.center = pos;
    }

    private ChunkPosition average(ChunkPosition o, ChunkPosition t) {
        int x = o.getX() + t.getX();
        int z = o.getZ() + t.getZ();

        return new ChunkPosition(x / 2,z / 2);
    }

    private ChunkPosition average(ChunkPosition o, ChunkPosition t, ChunkPosition t2) {
        int x = o.getX() + t.getX() + t2.getX();
        int z = o.getZ() + t.getZ() + t2.getZ();

        return new ChunkPosition(x / 3,z / 3);
    }

    class Counter {
        volatile int c;

        public Counter(int c) {
            this.c = c;
        }

        public synchronized void decrement() {
            c--;
        }

        public boolean isReady() {
            return c == 0;
        }
    }
}


