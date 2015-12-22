package com.graphhopper.examples.sfr;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Peter Karich
 */
public class ShapefileWriter {

    public void write(String fileName, List<List<GHPoint>> results, CoordinateReferenceSystem crs) {
        Transaction transaction = new DefaultTransaction("Reproject");
        FeatureWriter<SimpleFeatureType, SimpleFeature> writer = null;

        try {
            final File file = new File(fileName);
            ShapefileDataStore outStore = new ShapefileDataStore(file.toURI().toURL());
            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName("Location");
            builder.setCRS(crs);
            builder.add("geom", LineString.class);
            builder.length(15).add("Name", String.class);
            builder.add("number", Integer.class);
            outStore.createSchema(builder.buildFeatureType());

            writer = outStore.getFeatureWriterAppend(transaction);
            GeometryFactory fac = JTSFactoryFinder.getGeometryFactory();

            // create list of LineString's
            for (List<GHPoint> mr : results) {

                // create one LineString
                List<Coordinate> coordinates = new ArrayList<>();

                for (GHPoint p : mr) {
                    coordinates.add(new Coordinate(p.lon, p.lat));
                }

                if (coordinates.size() < 2) {
                    throw new IllegalStateException("empty coordinates? " + coordinates);
                }

                Coordinate[] coords = new Coordinate[coordinates.size()];
                for (int i = 0; i < coordinates.size(); i++) {
                    coords[i] = coordinates.get(i);
                }
                LineString line = fac.createLineString(coords);
                SimpleFeature feature = writer.next();
                feature.setDefaultGeometry(line);
                writer.write();
            }
            transaction.commit();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            close(writer);
            close(transaction);
        }
    }

    public static void close(Closeable cl) {
        try {
            if (cl != null) {
                cl.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Couldn't close resource", ex);
        }
    }
}
