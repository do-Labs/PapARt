/*
 * Part of the PapARt project - https://project.inria.fr/papart/
 *
 * Copyright (C) 2014-2016 Inria
 * Copyright (C) 2011-2013 Bordeaux University
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, version 2.1.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; If not, see
 * <http://www.gnu.org/licenses/>.
 */
package fr.inria.papart.multitouch;

import fr.inria.papart.procam.display.BaseDisplay;
import fr.inria.papart.procam.Screen;
import TUIO.*;
import processing.core.PApplet;
import java.util.*;
import processing.core.PVector;

/**
 *
 * @author Jeremy Laviole
 */
public class TUIOTouchInput extends TouchInput {

    private final TuioProcessing tuioClient;
    private final PApplet parent;
    private final HashMap<Integer, TouchPoint> tuioObjects = new HashMap<>();
    private final HashMap<Integer, TouchPoint> tuioCursors = new HashMap<>();

    public TUIOTouchInput(PApplet parent, int port) {
        tuioClient = new TuioProcessing(parent, this, port);
        this.parent = parent;
    }

    @Override
    public void update() {
    }

    @Override
    public TouchList projectTouchToScreen(Screen screen, BaseDisplay display) {
        TouchList touchList = new TouchList();

        Vector<TuioCursor> tuioCursorList = tuioClient.getTuioCursors();
        for (TuioCursor tuioCursor : tuioCursorList) {
            try {
                Touch touch = getCursor(screen, display, tuioCursor);
                touchList.add(touch);
            } catch (Exception e) {
                System.out.println("No Intersection" + e);
            }
        }

        Vector<TuioObject> tuioObjectList = tuioClient.getTuioObjects();
        for (TuioObject tuioObject : tuioObjectList) {
            try {
                Touch touch = getObject(screen, display, tuioObject);
                touchList.add(touch);
            } catch (Exception e) {
                System.out.println("No Intersection" + e);
            }
        }

        return touchList;
    }

    private Touch getCursor(Screen screen, BaseDisplay display, TuioCursor tcur) throws Exception {
        TouchPoint touchPoint = createTouchPointFrom(tcur);

        Touch touch = touchPoint.getTouch();
        TuioPoint tuioPoint = tcur.getPosition();
        PVector v = display.project(screen, tuioPoint.getX(), tuioPoint.getY());
        touch.setPosition(v);
        return touch;
    }

    private Touch getObject(Screen screen, BaseDisplay display, TuioObject tobj) throws Exception {

        TouchPoint tp = tuioObjects.get(tobj.getSymbolID());
        Touch touch = tp.getTouch();

        TuioPoint tuioPoint = tobj.getPosition();
        PVector v = display.project(screen, tuioPoint.getX(), tuioPoint.getY());
        touch.setPosition(v);

//        // TODO: implement this ?
//        touch.size = new PVector(10, 10);
        return touch;
    }

// these callback methods are called whenever a TUIO event occurs
// called when an object is added to the scene
    public void addTuioObject(TuioObject tobj) {
        tuioObjects.put(tobj.getSymbolID(), createTouchPointFrom(tobj));
    }
    
    // called when a cursor is added to the scene
    public void addTuioCursor(TuioCursor tcur) {
        tuioCursors.put(tcur.getCursorID(), createTouchPointFrom(tcur));
    }




    private TouchPoint createTouchPointFrom(TuioObject tObj) {
        TouchPoint tp = new TouchPoint();
        tp.setCreationTime(parent.millis());
        tp.id = tObj.getSymbolID();
        return tp;
    }

    private TouchPoint createTouchPointFrom(TuioCursor tcur) {
        TouchPoint tp = new TouchPoint();
        tp.setCreationTime(parent.millis());
        tp.id = tcur.getCursorID();
        return tp;
    }
    

    public void updateTuioObject(TuioObject tobj) {
        //  System.out.println("update object " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ") " + tobj.getX() + " " + tobj.getY() + " " + tobj.getAngle()
        //          + " " + tobj.getMotionSpeed() + " " + tobj.getRotationSpeed() + " " + tobj.getMotionAccel() + " " + tobj.getRotationAccel());
    }


    public void updateTuioCursor(TuioCursor tcur) {
        //   System.out.println("update cursor " + tcur.getCursorID() + " (" + tcur.getSessionID() + ") " + tcur.getX() + " " + tcur.getY()
        //        + " " + tcur.getMotionSpeed() + " " + tcur.getMotionAccel());
    }
    
    public void removeTuioObject(TuioObject tobj) {
        int id = tobj.getSymbolID();
        tuioObjects.remove(id);
//        System.out.println("remove object " + tobj.getSymbolID() + " (" + tobj.getSessionID() + ")");
    }

// called when a cursor is removed from the scene
    public void removeTuioCursor(TuioCursor tcur) {
        tuioCursors.remove(tcur.getCursorID());
        //   System.out.println("remove cursor " + tcur.getCursorID() + " (" + tcur.getSessionID() + ")");
    }

// called after each message bundle
// representing the end of an image frame
    public void refresh(TuioTime bundleTime) {
    }
}
