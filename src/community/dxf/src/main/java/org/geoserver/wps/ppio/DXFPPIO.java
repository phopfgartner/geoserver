/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import java.util.LinkedList;
import org.opengis.feature.Feature;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geoserver.wfs.response.dxf.DXFWriter;
import org.geoserver.wfs.response.dxf.DXFWriterFinder;

/**
 * Inputs and outputs feature collections in GeoJSON format using gt-geojson
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class DXFPPIO extends CDataPPIO {

    protected DXFPPIO() {
        super(FeatureCollection.class, FeatureCollection.class, "application/dxf");
    }

    @Override
    public void encode(Object value, OutputStream os) throws IOException {
    	// the following code resembles org.geoserver.wfs.response.DXFOutputFormat
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os));
        DXFWriter dxfWriter = DXFWriterFinder.getWriter("14", w);
        String[] names = {"wps_result"};
        dxfWriter.setOption("layers", names);
        int[] colors = {1};
        dxfWriter.setOption("colors", colors);
        
        List lft = new LinkedList();
        lft.add(value);
        dxfWriter.write(lft,"14");
        w.flush();            
    }

    @Override
    public Object decode(InputStream input) throws Exception {
    	throw new Exception("not implemented");
    }

    @Override
    public Object decode(String input) throws Exception {
    	throw new Exception("not implemented");
    }
    
    @Override
    public String getFileExtension() {
        return "dxf";
    }

}
