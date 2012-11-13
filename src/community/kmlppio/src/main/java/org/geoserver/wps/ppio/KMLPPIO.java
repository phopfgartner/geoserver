/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Geometry;

public class KMLPPIO extends XMLPPIO {
    private static final Logger LOGGER = Logging.getLogger(KMLPPIO.class);

    Configuration xml;

    SimpleFeatureType type;

    /*
     * public KMLPPIO(Class type, QName element) { super(type, type, "application/vnd.google-earth.kml+xml", element); xml = new KMLConfiguration(); }
     */

    /*
     * protected KMLPPIO() { super(FeatureCollection.class, FeatureCollection.class, "application/vnd.google-earth.kml+xml"); }
     */

    public KMLPPIO() {
        super(FeatureCollection.class, FeatureCollection.class,
                "application/vnd.google-earth.kml+xml", KML.Document);

        // new org.geotools.wfs.v1_1.WFSConfiguration(), "text/xml; subtype=wfs-collection/1.1",
        // org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION);
        xml = new KMLConfiguration();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        // set the name
        b.setName("puregeometries");

        // add a geometry property
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("location", Geometry.class);

        // build the type
        type = b.buildFeatureType();

    }

    /**
     * Only the geometry is read from the data.
     * This is mainly due to the fact, that the feature as returned by decode()
     * can not be handled by the GML writer, since objects of class "Coordinate", e.g. Lookat
     * are returned
     */
    @Override
    public Object decode(InputStream input) throws Exception {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        
        StreamingParser parser = new StreamingParser(new KMLConfiguration(), input,
                KML.Placemark);
        SimpleFeature f = null;
        SimpleFeatureCollection features = new ListFeatureCollection(type);

        while ((f = (SimpleFeature) parser.parse()) != null) {
            Object dg = ((SimpleFeature) f).getDefaultGeometry();
            if (dg != null) {
                builder.add(dg);
                SimpleFeature fnew = builder.buildFeature(f.getID());
                features.add(fnew);
            }
        }
        return features;
        
        /*
         * Parser p = new Parser(xml); Object retval = p.parse(input); LOGGER.info("KMLPPIO::decode: returned object is of type " +
         * retval.getClass().getName()); List<Object> values = ((SimpleFeatureImpl) retval).getAttributes();
        return retval;
         */
    }

    @Override
    public void encode(Object obj, ContentHandler handler) throws Exception {
        LOGGER.info("KMLPPIO::encode: obj is of class " + obj.getClass().getName());
        LOGGER.info("KMLPPIO::encode: handler is of class " + handler.getClass().getName());
        Encoder e = new Encoder(xml);
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        CoordinateReferenceSystem targetCRS;
        try {
            targetCRS = factory.createCoordinateReferenceSystem("EPSG:4326");
            obj = new ReprojectingFeatureCollection((FeatureCollection) obj, targetCRS);
            Encoder encoder = new Encoder(new KMLConfiguration());
            encoder.setIndenting(true);

            LOGGER.info("KMLPPIO::encode: value is of class " + obj.getClass().getName());
            encoder.encode((FeatureCollection) obj, KML.kml, handler);
        } catch (NoSuchAuthorityCodeException e1) {
            LOGGER.warning(e1.toString());
        } catch (FactoryException e1) {
            LOGGER.warning(e1.toString());
        }
        // e.encode(obj, element, handler);
    }

    @Override
    public String getFileExtension() {
        return "kml";
    }

    // /**
    // * Place holder for process params which declare Geometry.class as the type.
    // */
    // public static class Geometry extends KMLPPIO {
    // public Geometry() {
    // super(com.vividsolutions.jts.geom.Geometry.class, KML.Geometry);
    // }
    //
    // @Override
    // public void encode(Object object, ContentHandler output) throws Exception {
    // if (object instanceof GeometryCollection) {
    // /*
    // * if ( object instanceof MultiPoint ) { new KMLPPIO(MultiPoint.class,KML.MultiGeometry).encode(object, output); } else if ( object
    // * instanceof MultiLineString ) { new KMLPPIO(MultiLineString.class,GML.MultiLineString).encode(object, output); } else if ( object
    // * instanceof MultiPolygon ) { new KMLPPIO(MultiPolygon.class,GML.MultiPolygon).encode(object, output); } else { new
    // * KMLPPIO(GeometryCollection.class, GML._Geometry).encode(object, output); }
    // */
    // } else {
    // if (object instanceof Point) {
    // new KMLPPIO(Point.class, KML.Point).encode(object, output);
    // } else if (object instanceof LineString) {
    // new KMLPPIO(LineString.class, KML.LineString).encode(object, output);
    // } else if (object instanceof Polygon) {
    // new KMLPPIO(Polygon.class, KML.Polygon).encode(object, output);
    // }
    // }
    // }
    //
    // }
    //
    // /**
    // * PPIO with alternate mime type suitable for usage in Execute KVP
    // */
    // public static class GeometryAlternate extends Geometry {
    //
    // public GeometryAlternate() {
    // super();
    // mimeType = "application/vnd.google-earth.kml+xml";
    // }
    // }
}
