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

import java.util.ArrayList;
import toxi.geom.Vec3D;

/**
 *
 * @author Jeremy Laviole
 */
public class ConnectedComponent extends ArrayList<Integer> {

    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public Vec3D getMean(Vec3D[] array) {
        Vec3D mean = new Vec3D(0, 0, 0);
        for (int offset : this) {
            mean.addSelf(array[offset]);
        }
        mean.scaleSelf(1.0f / this.size());
        return mean;
    }

    public float getMinZ(Vec3D[] array) {
        float min = Float.MAX_VALUE;
        for (int offset : this) {
            float z = array[offset].z;
            if (z < min) {
                min = z;
            }
        }
        return min;
    }
    
    public float getHeight(Vec3D[] array) {
        float min = Float.MAX_VALUE;
        float max = 0;
        for (int offset : this) {
            float z = array[offset].z;
            if (z < min) {
                min = z;
            }
            if (z > max) {
                max = z;
            }
        }
        return max - min;
    }

}
