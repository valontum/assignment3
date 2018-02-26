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



public class StudMovement extends MapBasedMovement implements
        SwitchableMovement {

    private static final int WALKING_TO_DEST_MODE = 0;
    private static final int AT_LECTURE_MODE = 1;
    private static final int AT_LUNCH_MODE = 2;
    private static final int AT_HOME_MODE = 3;

    public static final String CAFETERIA_POINT_SETTING = "cafeteriaPoint";
    public static final String NR_OF_ROOMS_SETTING = "nrOfRooms";
    public static final String NR_OF_STARTS_SETTING = "nrOfStarts";
    public static final String LUNCH_TIME_SETTING = "lunchTime";

    public static final String ROOM_LOCATIONS_FILE_SETTING =
            "roomLocationsFile";
    public static final String START_LOCATIONS_FILE_SETTING =
            "startLocationsFile";


   
    private static int lectureLength = 7200;
    private static int lunchBreakLength = 3600;

    private int currentRoomNr;

    private int mode;

    private int nrOfStarts;
    private int nrOfRooms;
    private int startedLectureTime;
    private int lunchTime;
    private int startedLunchTime;
    private boolean hadLunch = false;
    private boolean ready;
    private DijkstraPathFinder pathFinder;

    private List<Coord> allStarts;
    private List<Coord> allRooms;

    private Coord startPoint;
    private Coord cafePoint;
    private Coord lastWaypoint;

   
    public StudMovement(Settings settings) {
        super(settings);

        int [] cafeteriaPoint = settings.getCsvInts(CAFETERIA_POINT_SETTING,2);
        Coord c = new Coord(cafeteriaPoint[0], cafeteriaPoint[1]);
        cafePoint = c;

        nrOfStarts = settings.getInt(NR_OF_STARTS_SETTING);
        nrOfRooms = settings.getInt(NR_OF_ROOMS_SETTING);
        lunchTime = settings.getInt(LUNCH_TIME_SETTING);

        currentRoomNr = 0;
        startedLectureTime = -1;
        startedLunchTime = -1;
        pathFinder = new DijkstraPathFinder(null);
        mode = WALKING_TO_DEST_MODE;

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

        String roomLocationsFile = null;
        try {
            roomLocationsFile = settings.getSetting(
                    ROOM_LOCATIONS_FILE_SETTING);
        } catch (Throwable t) {
            // Do nothing;
        }

        if (roomLocationsFile == null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
        } else {
            try {
                allRooms = new LinkedList<Coord>();
                List<Coord> locationsRead = (new WKTReader()).
                        readPoints(new File(roomLocationsFile));
                for (Coord coord : locationsRead) {
                    SimMap map = getMap();
                    Coord offset = map.getOffset();
                    // mirror points if map data is mirrored
                    if (map.isMirrored()) {
                        coord.setLocation(coord.getX(), -coord.getY());
                    }
                    coord.translate(offset.getX(), offset.getY());
                    allRooms.add(coord);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copyconstructor
     * @param proto
     */
    public StudMovement(StudMovement proto) {
        super(proto);
        this.nrOfStarts = proto.nrOfStarts;
        this.startPoint = proto.startPoint;
        this.cafePoint = proto.cafePoint;
        this.currentRoomNr = proto.currentRoomNr;
        this.lunchTime = proto.lunchTime;
        startedLectureTime = -1;
        startedLunchTime = -1;
        this.pathFinder = proto.pathFinder;
        this.mode = proto.mode;

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

        if (proto.allRooms == null) {
            MapNode[] mapNodes = (MapNode[])getMap().getNodes().
                    toArray(new MapNode[0]);
        } else {
            this.allRooms = proto.allRooms;
        }
    }

    @Override
    public Coord getInitialLocation() {
        this.lastWaypoint = this.startPoint;
        return this.startPoint.clone();
    }

    @Override
    public Path getPath() {
        if (mode == WALKING_TO_DEST_MODE) {
            // Try to find to the next destination
            SimMap map = super.getMap();
            if (map == null) {
                return null;
            }
            MapNode thisNode = map.getNodeByCoord(lastWaypoint);
            MapNode destinationNode;
            if (currentRoomNr < 3) {
                // Check if it is already time for lunch
                if (this.hadLunch == false && SimClock.getIntTime() >= this.lunchTime) {
                    destinationNode = map.getNodeByCoord(cafePoint);
                    mode = AT_LUNCH_MODE;
                } else {
                    // Get next random room from Room File
                    destinationNode = map.getNodeByCoord(allRooms.get(
                            rng.nextInt(allRooms.size())).clone());
                    currentRoomNr++;
                    mode = AT_LECTURE_MODE;
                }
            } else {
                // Go home - Total number of lectures reached
                destinationNode = map.getNodeByCoord(startPoint);
                mode = AT_HOME_MODE;
            }
            List<MapNode> nodes = pathFinder.getShortestPath(thisNode,
                    destinationNode);
            Path path = new Path(generateSpeed());
            for (MapNode node : nodes) {
                path.addWaypoint(node.getLocation());
            }
            lastWaypoint = destinationNode.getLocation();
            return path;
        } else if (mode == AT_LECTURE_MODE) {
            if (startedLectureTime == -1) {
                startedLectureTime = SimClock.getIntTime();
            }
            // Check if current lecture is over
            if (SimClock.getIntTime() - startedLectureTime >= lectureLength) {
                mode = WALKING_TO_DEST_MODE;
                startedLectureTime = -1;
            }
            Path path = new Path(1);
            path.addWaypoint(lastWaypoint.clone());
            ready = true;
            return path;
        } else if (mode == AT_LUNCH_MODE) {
            if (startedLunchTime == -1) {
                startedLunchTime = SimClock.getIntTime();
            }
            // Check if lunch is over
            if (SimClock.getIntTime() - startedLunchTime >= lunchBreakLength) {
                mode = WALKING_TO_DEST_MODE;
                hadLunch = true;
            }
            Path path = new Path(1);
            path.addWaypoint(lastWaypoint.clone());
            ready = true;
            return path;
        } else {
            Path path = new Path(1);
            path.addWaypoint(lastWaypoint.clone());
            ready = true;
            return path;
        }
    }

    @Override
    public MapBasedMovement replicate() {
        return new StudMovement(this);
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
        startedLectureTime = -1;
        ready = false;
        mode = WALKING_TO_DEST_MODE;
    }
}
