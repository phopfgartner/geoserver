/* Copyright (c) 2012 R3 GIS - www.r3-gis.com. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.geotools.xml.StreamingParser;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
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

    public KMLPPIO() {
        super(FeatureCollection.class, FeatureCollection.class,
                "application/vnd.google-earth.kml+xml", KML.Document);

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
     * Create a HashMap describing the type signature of the feature
     * This is used to verify that only features of the same type are
     * processed.
     * Only features of types, which are known to be parsable are included
     * 
     * @param f
     * @return
     */
    private HashMap<Name, Class> getSignature(SimpleFeature f) {
        HashMap<Name, Class> ftype = new LinkedHashMap<Name, Class>();
        Collection<Property> properties = f.getProperties();
        for (Property p :properties) {
            Class c = (Class)p.getType().getBinding();
            if (c.isAssignableFrom(String.class) ||  
                    c.isAssignableFrom(Boolean.class) ||
                    c.isAssignableFrom(Integer.class) ||
                    c.isAssignableFrom(Float.class) ||
                    c.isAssignableFrom(Double.class) ||
                    c.isAssignableFrom(Geometry.class)
                    ) {
            ftype.put(p.getName(), c);
            }
        }
        return ftype;
    }
    
    /**
     * Convert the HashMap into a TeatureType
     * 
     * @param ftype
     * @return
     */
    private SimpleFeatureType getType(HashMap<Name, Class> ftype) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        //set the name
        b.setName( "puregeometries" );

        //add a geometry property
        b.setCRS( DefaultGeographicCRS.WGS84 );
        for (Map.Entry<Name, Class> entry : ftype.entrySet()) {
            b.add( entry.getKey().toString(), entry.getValue() );
        }                
        return b.buildFeatureType();
    }
    
    
    /**
     * Read KML file. This is not done with decode, since some features con not be converted by
     * other DataSources.
     */
    @Override
    public Object decode(InputStream input) throws Exception {

        StreamingParser parser = new StreamingParser(new KMLConfiguration(), input, KML.Placemark);
        SimpleFeature f = null;
        SimpleFeatureCollection features = null; // new ListFeatureCollection(type)
        HashMap<Name, Class> oldftype = null;
        SimpleFeatureType type = null;
        SimpleFeatureBuilder featureBuilder = null;

        while ((f = (SimpleFeature) parser.parse()) != null) {
            HashMap<Name, Class> ftype = getSignature(f);
            if (oldftype == null) {
                oldftype = ftype;
                type = getType(ftype);
                featureBuilder = new SimpleFeatureBuilder(type);
                features = new ListFeatureCollection(type);
            } else if (!oldftype.equals(ftype)){
                // maybe I'm overly defensive here. Can it happen that features with a different type land here?
                break;
            }
            for (Map.Entry<Name, Class> entry : ftype.entrySet()) {
                featureBuilder.add(f.getAttribute(entry.getKey()));
            }                
            SimpleFeature fnew = featureBuilder.buildFeature(f.getID());
            features.add(fnew);
        }
        return features;
    }

    @Override
    public void encode(Object obj, ContentHandler handler) throws Exception {
        LOGGER.info("KMLPPIO::encode: obj is of class " + obj.getClass().getName()+
                ", handler is of class " + handler.getClass().getName());
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        CoordinateReferenceSystem targetCRS;
        try {
            targetCRS = factory.createCoordinateReferenceSystem("EPSG:4326");
            obj = new ReprojectingFeatureCollection((FeatureCollection) obj, targetCRS);
            Encoder encoder = new Encoder(new KMLConfiguration());
            encoder.setIndenting(true);
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
}
