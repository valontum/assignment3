/*
 * Copyright 2017 Technical University Munic
 * Released under GPLv3. See LICENSE.txt for details.
 */
package movement;

import input.WKTReader;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.Settings;
import core.SimClock;

/**
 * This class models the movement of facility managers. Based on the MapBasedMovement it
 * gives out Paths that use the roads of a SimMap. In addition, it uses a start point
 * for each node.
 *
 * @author Johannes Seiler, Nadja Schricker
 *
 */

public class FMMovement  extends MapBasedMovement implements
    SwitchableMovement {

    public static final String NR_OF_STARTS_SETTING = "nrOfStarts";

    public static final String START_LOCATIONS_FILE_SETTING =
            "startLocationsFile";

    private int nrOfStarts;

    private boolean ready;;

    private List<Coord> allStarts;

    private Coord startPoint;
    private Coord lastWaypoint;

    /**
     * FMMovement constructor
     * @param settings
     */
    public FMMovement(Settings settings) {
        super(settings);

        nrOfStarts = settings.getInt(NR_OF_STARTS_SETTING);

        String startLocationsFile = null;
        try {
            startLocationsFile = settings.getSetting(
                    START_LOCATIONS_FILE_SETTING);
        } catch (Throwable t) {
            // Do nothing;
        }

        if (startLocationsFile == null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
            int startIndex = rng.nextInt(mapNodes.length - 1) /
                    (mapNodes.length/nrOfStarts);
            startPoint = mapNodes[startIndex].getLocation().clone();
        } else {
            try {
                allStarts = new LinkedList<Coord>();
                List<Coord> locationsRead = (new WKTReader()).
                        readPoints(new File(startLocationsFile));
                for (Coord coord : locationsRead) {
                    SimMap map = getMap();
                    Coord offset = map.getOffset();
                    // mirror points if map data is mirrored
                    if (map.isMirrored()) {
                        coord.setLocation(coord.getX(), -coord.getY());
                    }
                    coord.translate(offset.getX(), offset.getY());
                    allStarts.add(coord);
                }
                startPoint = allStarts.get(
                        rng.nextInt(allStarts.size())).clone();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copyconstructor
     * @param proto
     */
    public FMMovement(FMMovement proto) {
        super(proto);
        this.nrOfStarts = proto.nrOfStarts;
        this.startPoint = proto.startPoint;

        if (proto.allStarts == null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
            int startIndex = rng.nextInt(mapNodes.length - 1) /
                    (mapNodes.length/nrOfStarts);
            startPoint = mapNodes[startIndex].getLocation().clone();
        } else {
            this.allStarts = proto.allStarts;
            startPoint = allStarts.get(
                    rng.nextInt(allStarts.size())).clone();
        }
    }


    @Override
    public Coord getInitialLocation() {
        SimMap map = super.getMap();
        if (map == null) {
            return null;
        }
        this.lastMapNode = map.getNodeByCoord(startPoint);
        return this.startPoint.clone();
    }

    @Override
    public MapBasedMovement replicate() {
        return new FMMovement(this);
    }

    /**
     * @see SwitchableMovement
     */
    public Coord getLastLocation() {
        return lastWaypoint.clone();
    }

    /**
     * @see SwitchableMovement
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * @see SwitchableMovement
     */
    public void setLocation(Coord lastWaypoint) {
        this.lastWaypoint = lastWaypoint.clone();
        ready = false;
    }

}
