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
 * This class models movement of employees in an university. If the node happens
 * to be at some other location than the office, it first walks the shortest path
 * to the office (e.g. library, cafeteria, chair-office) and stays there until lunch time.
 * After lunch it will immediatly return to its office and if the work day length is over.
 *
 * @author  Johannes Seiler, Nadja Schricker
 *
 */
public class StaffMovement extends MapBasedMovement implements
        SwitchableMovement {

    private static final int WALKING_TO_DEST_MODE = 0;
    private static final int AT_OFFICE_MODE = 1;
    private static final int AT_LUNCH_MODE = 2;
    private static final int AT_HOME_MODE = 3;

    public static final String CAFETERIA_POINT_SETTING = "cafeteriaPoint";
    public static final String WORK_DAY_LENGTH_SETTING = "workDayLength";
    public static final String NR_OF_OFFICES_SETTING = "nrOfOffices";
    public static final String NR_OF_STARTS_SETTING = "nrOfStarts";
    public static final String LUNCH_TIME_SETTING = "lunchTime";

    public static final String OFFICE_LOCATIONS_FILE_SETTING =
            "officeLocationsFile";
    public static final String START_LOCATIONS_FILE_SETTING =
            "startLocationsFile";

    /**
     *  43k ~= 12h -> 3600 ~= 1h
     */
    private static int lunchBreakLength = 3600;

    private static int nrOfOffices = 50;

    private int mode;
    private int nextmode;

    private int nrOfStarts;
    private int workDayLength;
    private int startedWorkingTime;
    private int lunchTime;
    private int startedLunchTime;
    private boolean hadLunch = false;
    private boolean ready;
    private DijkstraPathFinder pathFinder;

    private List<Coord> allStarts;
    private List<Coord> allOffices;

    private Coord lastWaypoint;
    private Coord nextLocation;
    private Coord officeLocation;
    private Coord startPoint;
    private Coord cafePoint;

    /**
     * StaffMovement constructor
     * @param settings
     */
    public StaffMovement(Settings settings) {
        super(settings);

        int [] cafeteriaPoint = settings.getCsvInts(CAFETERIA_POINT_SETTING,2);
        Coord c = new Coord(cafeteriaPoint[0], cafeteriaPoint[1]);
        cafePoint = c;

        workDayLength = settings.getInt(WORK_DAY_LENGTH_SETTING);
        nrOfOffices = settings.getInt(NR_OF_OFFICES_SETTING);
        nrOfStarts = settings.getInt(NR_OF_STARTS_SETTING);
        lunchTime = settings.getInt(LUNCH_TIME_SETTING);

        startedWorkingTime = -1;
        startedLunchTime = -1;
        pathFinder = new DijkstraPathFinder(null);
        mode = WALKING_TO_DEST_MODE;
        nextmode = AT_OFFICE_MODE;

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

        String officeLocationsFile = null;
        try {
            officeLocationsFile = settings.getSetting(
                    OFFICE_LOCATIONS_FILE_SETTING);
        } catch (Throwable t) {
            // Do nothing;
        }

        if (officeLocationsFile == null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
            int officeIndex = rng.nextInt(mapNodes.length - 1) /
                    (mapNodes.length/nrOfOffices);
            officeLocation = mapNodes[officeIndex].getLocation().clone();
        } else {
            try {
                allOffices = new LinkedList<Coord>();
                List<Coord> locationsRead = (new WKTReader()).
                        readPoints(new File(officeLocationsFile));
                for (Coord coord : locationsRead) {
                    SimMap map = getMap();
                    Coord offset = map.getOffset();
                    // mirror points if map data is mirrored
                    if (map.isMirrored()) {
                        coord.setLocation(coord.getX(), -coord.getY());
                    }
                    coord.translate(offset.getX(), offset.getY());
                    allOffices.add(coord);
                }
                officeLocation = allOffices.get(
                        rng.nextInt(allOffices.size())).clone();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        nextLocation = officeLocation;
    }

    /**
     * Copyconstructor
     * @param proto
     */
    public StaffMovement(StaffMovement proto) {
        super(proto);
        this.nrOfStarts = proto.nrOfStarts;
        this.startPoint = proto.startPoint;
        this.cafePoint = proto.cafePoint;
        this.lunchTime = proto.lunchTime;
        this.workDayLength = proto.workDayLength;
        startedWorkingTime = -1;
        startedLunchTime = -1;
        this.pathFinder = proto.pathFinder;
        this.mode = proto.mode;
        this.nextmode = proto.nextmode;

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

        if (proto.allOffices == null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
            int officeIndex = rng.nextInt(mapNodes.length - 1) /
                    (mapNodes.length/nrOfOffices);
            officeLocation = mapNodes[officeIndex].getLocation().clone();
        } else {
            this.allOffices = proto.allOffices;
            officeLocation = allOffices.get(
                    rng.nextInt(allOffices.size())).clone();
        }

        this.nextLocation = proto.officeLocation;
    }

    @Override
    public Coord getInitialLocation() {
        this.lastWaypoint = this.startPoint;
        return this.startPoint.clone();
    }

    @Override
    public Path getPath() {
        Path path = new Path(1);
        switch (mode) {
            case WALKING_TO_DEST_MODE:
                // Try to find to the next location
                SimMap map = super.getMap();
                if ( map == null ) {
                    return null;
                }
                MapNode thisNode = map.getNodeByCoord(lastWaypoint);
                MapNode destinationNode = map.getNodeByCoord(nextLocation);
                List<MapNode> nodes = pathFinder.getShortestPath(thisNode,
                        destinationNode);
                path = new Path(generateSpeed());
                for ( MapNode node : nodes ) {
                    path.addWaypoint(node.getLocation());
                }
                lastWaypoint = nextLocation.clone();
                mode = nextmode;
                break;

            case AT_OFFICE_MODE:
                if ( startedWorkingTime == -1 ) {
                    startedWorkingTime = SimClock.getIntTime();
                }
                // Check if it is time for lunch
                if ( hadLunch == false && SimClock.getIntTime() >= this.lunchTime ) {
                    nextLocation = cafePoint;
                    mode = WALKING_TO_DEST_MODE;
                    nextmode = AT_LUNCH_MODE;
                }
                // Check if work day is over
                if ( SimClock.getIntTime() - startedWorkingTime >= workDayLength ) {
                    mode = WALKING_TO_DEST_MODE;
                    nextmode = AT_HOME_MODE;
                    nextLocation = startPoint;
                }
                path = new Path(1);
                path.addWaypoint(lastWaypoint.clone());
                break;

            case AT_LUNCH_MODE:
                if ( startedLunchTime == -1) {
                    startedLunchTime = SimClock.getIntTime();
                }
                // Check if lunch is over
                if ( SimClock.getIntTime() - startedLunchTime >= lunchBreakLength ) {
                    nextLocation = officeLocation;
                    mode = WALKING_TO_DEST_MODE;
                    nextmode = AT_OFFICE_MODE;
                    hadLunch = true;
                }
                path = new Path(1);
                path.addWaypoint(lastWaypoint.clone());
                break;

            case AT_HOME_MODE:
                path = new Path(1);
                path.addWaypoint(lastWaypoint.clone());
                break;

            default:
                break;
        }
        return path;
    }

    @Override
    public MapBasedMovement replicate() {
        return new StaffMovement(this);
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
        startedWorkingTime = -1;
        ready = false;
        mode = nextmode;
    }

}
