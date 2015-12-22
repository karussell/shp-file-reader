package com.graphhopper.examples.sfr;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 *
 * @author Peter Karich
 */
public class ShapefileReader {

    public static void main(String[] args) throws Exception {
        System.out.println(new ShapefileReader().read(args[0], null));
    }

    private final CoordinateReferenceSystem targetCRS;

    /**
     * Uses DefaultGeographicCRS.WGS84 as target CRS. Keep in mind that although
     * it should be identical to CRS.decode("EPSG:4326") the axes are swapped.
     * See http://stackoverflow.com/a/12304356/194609
     */
    public ShapefileReader() {
        // CRS.lookupEpsgCode(WGS84, true) returns 4326 and 
        // CRS.lookupEpsgCode(CRS.decode("EPSG:4326"), true) returns the same BUT has swapped axes!?
        targetCRS = DefaultGeographicCRS.WGS84;
    }

    /**
     * Specify custom target CRS. Get one e.g. via CRS.decode("EPSG:4326")
     */
    public ShapefileReader(CoordinateReferenceSystem targetCRS) {
        this.targetCRS = targetCRS;
    }

    public List<List<GPXEntry>> read(String fileStr, CoordinateReferenceSystem sourceCRS) throws Exception {
        File file = new File(fileStr);

        Map connect = new HashMap();
        connect.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(connect);
        String[] typeNames = dataStore.getTypeNames();
        String typeName = typeNames[0];
        FeatureSource featureSource = dataStore.getFeatureSource(typeName);
        if (sourceCRS == null) {
            sourceCRS = featureSource.getSchema().getCoordinateReferenceSystem();
        }

        // System.out.println(CRS.lookupEpsgCode(crs, true));
        FeatureCollection collection = featureSource.getFeatures();
        // allow for some error due to different datums ('bursa wolf parameters required')
        boolean lenient = true;
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);

        List<List<GPXEntry>> lineList = new ArrayList<>();
        try (FeatureIterator iterator = collection.features()) {

            while (iterator.hasNext()) {
                long counter = 0;
                SimpleFeature feature = (SimpleFeature) iterator.next();

                MultiLineString mlString = (MultiLineString) feature.getDefaultGeometry();

                if (mlString == null || !mlString.isValid()) {
                    throw new IllegalStateException("Invalid geometry " + mlString);
                }

                if (mlString.getNumGeometries() != 1) {
                    throw new IllegalStateException("one geometry expected but was " + mlString.getNumGeometries() + ", " + mlString);
                }

                LineString line = (LineString) mlString.getGeometryN(0);
                // transform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
                // does not work precisely?
                line = (LineString) JTS.transform(line, transform);
                List<GPXEntry> oneLine = new ArrayList<>();
                for (int i = 0; i < line.getNumPoints(); i++) {
                    Coordinate c = line.getCoordinateN(i);
                    oneLine.add(new GPXEntry(c.y, c.x, counter));
                    counter += 1000;
                }

                // show raw GPS info System.out.println(oneLine);
                lineList.add(oneLine);
            }
        }
        dataStore.dispose();
        // System.out.println("counts " + counter);

        return lineList;
    }
}
