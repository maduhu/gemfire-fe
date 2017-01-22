package io.pivotal.bds.gemfire.geojson.comp;

import com.vividsolutions.jts.geom.Geometry;

public class Disjoint extends GeoComparator {

    @Override
    public boolean compare(Geometry g1, Geometry g2) {
        return g1.disjoint(g2);
    }
}