package com.aws.athena.udf.h3;

import com.uber.h3core.util.LatLng;
import com.uber.h3core.H3Core;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
/**
 * Unit test for simple App.
 */
public class H3AthenaHandlerTest 
{
    final private static String LNG = "lng";
    final private static String LAT = "lat";   
    final private H3AthenaHandler handler;
    final private H3Core h3Core;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public H3AthenaHandlerTest() throws IOException
    {
        handler = new H3AthenaHandler();
        h3Core = H3Core.newInstance();
    }

    @Test
    public void testlat_lng_to_cell() 
    {
        assertNull(handler.lat_lng_to_cell(null, 10.5, 1));
        assertNull(handler.lat_lng_to_cell(-10.4, null, 2));
        assertNull(handler.lat_lng_to_cell(10.4, 13.2, null));

        final Random r = new Random();
        for (int i=0; i < 1000; ++i) {
            final double latitude = Math.random() * 180.0 - 90.0;
            final double longitude = Math.random() * 360.0 - 180.0;

            final double latitudeOther =  Math.max(-90.0, latitude - Math.random());
            final double longitudeOther = Math.max(-180.0, longitude- Math.random());

            final int res = r.nextInt(15);

            assertTrue( handler.lat_lng_to_cell(latitude, longitude,res) == h3Core.latLngToCell(latitude, longitude, res));

            if (handler.lat_lng_to_cell(latitude, longitude,res).longValue() == handler.lat_lng_to_cell(latitudeOther, longitudeOther,res)) {
                assertTrue(handler.lat_lng_to_cell(latitude, longitude,res).longValue()  == h3Core.latLngToCell(latitudeOther, longitudeOther,res));
            }
            else {
                assertFalse(handler.lat_lng_to_cell(latitude, longitude,res)  == h3Core.latLngToCell(latitudeOther, longitudeOther,res));
            }
        }
    }

    @Test
    public void testlat_lng_to_cell_address() 
    {
        assertNull(handler.lat_lng_to_cell_address(null, 10.5, 1));
        assertNull(handler.lat_lng_to_cell_address(-10.4, null, 2));
        assertNull(handler.lat_lng_to_cell_address(10.4, 13.2, null));

        final Random r = new Random();
        for (int i=0; i < 1000; ++i) {
            final double latitude = Math.random() * 180.0 - 90.0;
            final double longitude = Math.random() * 360.0 - 180.0;

            final double latitudeOther =  Math.max(-90.0, latitude - Math.random());
            final double longitudeOther = Math.max(-180.0, longitude- Math.random());

            final int res = r.nextInt(15);

            assertEquals( handler.lat_lng_to_cell_address(latitude, longitude,res) , h3Core.latLngToCellAddress(latitude, longitude, res));

            if (handler.lat_lng_to_cell_address(latitude, longitude,res).equals(handler.lat_lng_to_cell_address(latitudeOther, longitudeOther,res))) {
                assertEquals(handler.lat_lng_to_cell_address(latitude, longitude,res), h3Core.latLngToCellAddress(latitudeOther, longitudeOther,res));
            }
            else {
                assertFalse(handler.lat_lng_to_cell_address(latitude, longitude,res).equals(h3Core.latLngToCellAddress(latitudeOther, longitudeOther,res)));
            }
        }
    }

    @Test
    public void testget_icosahedron_faces() {
        final double latitude = 43.0;
        final double longitude = -42;

        assertNull(handler.get_icosahedron_faces((Long)null));
        assertNull(handler.get_icosahedron_faces((String)null));
        for (int i = 0; i < 16; ++i) {
            final Long h3 = handler.lat_lng_to_cell(latitude, longitude, i);
            final Long h3Address = handler.lat_lng_to_cell(latitude, longitude, i);

            assertEquals(h3Core.getIcosahedronFaces(h3), handler.get_icosahedron_faces(h3));
            assertEquals(h3Core.getIcosahedronFaces(h3Address), handler.get_icosahedron_faces(h3Address));
        }
    }

    @Test
    public void testgrid_disk() throws Exception {
        final double latitude = 43.0;
        final double longitude = -42;

        assertNull(handler.grid_disk((Long)null, 1));
        assertNull(handler.grid_disk((Long)null, null));

        assertNull(handler.grid_disk((String)null, 2));
        assertNull(handler.grid_disk((String)null, null));

        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 3);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, 3);

        assertNull(handler.grid_disk(h3, null));
        assertNull(handler.grid_disk(h3Address, null));

        final int kmax = 5;

        for (int i = 1; i < kmax; ++i) {
            final List<Long> indexes = handler.grid_disk(h3, i);
            for (final long index : indexes) {
                assertTrue(handler.grid_distance(index, h3) <= kmax);
            }
        }

        for (int i = 1; i < kmax; ++i) {
            final List<String> addresses = handler.grid_disk(h3Address, i);
            for (final String address : addresses) {
                assertTrue(handler.grid_distance(address, h3Address) <= kmax);
            }
        }
    }

    @Test
    public void testgrid_ring_unsafe() throws Exception {
        final double latitude = 43.0;
        final double longitude = -42;

        assertNull(handler.grid_ring_unsafe((Long)null, 1));
        assertNull(handler.grid_ring_unsafe((Long)null, null));

        assertNull(handler.grid_ring_unsafe((String)null, 2));
        assertNull(handler.grid_ring_unsafe((String)null, null));

        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 3);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, 3);

        assertNull(handler.grid_ring_unsafe(h3, null));
        assertNull(handler.grid_ring_unsafe(h3Address, null));

        final int kmax = 5;

        for (int i = 1; i < kmax; ++i) {
            final List<Long> indexes = handler.grid_ring_unsafe(h3, i);
            for (final long index : indexes) {
                assertTrue(handler.grid_distance(index, h3) <= kmax);
            }
        }

        for (int i = 1; i < kmax; ++i) {
            final List<String> addresses = handler.grid_ring_unsafe(h3Address, i);
            for (final String address : addresses) {
                assertTrue(handler.grid_distance(address, h3Address) <= kmax);
            }
        }
    }

    @Test
    public void test_pentagons() {
        final double latitude = 43.0;
        final double longitude = -42;

        for (int i = 0; i < 16; ++i) {
            final Long h3 = handler.lat_lng_to_cell(latitude, longitude, i);
            final Long h3Address = handler.lat_lng_to_cell(latitude, longitude, i);
            assertFalse(handler.is_pentagon((Long)null));
            assertFalse(handler.is_pentagon((String)null));
      
            final List<Long> indexes = handler.get_pentagons(i);
            final List<String> addresses = handler.get_pentagon_addresses(i);

            assertEquals(h3Core.getPentagons(i), indexes);
            assertEquals(h3Core.getPentagonAddresses(i), addresses);


            assertFalse(handler.is_pentagon(h3));
            assertFalse(handler.is_pentagon(h3Address));
            assertEquals(h3Core.isPentagon(h3), handler.is_pentagon(h3));
            assertEquals(h3Core.isPentagon(h3Address), handler.is_pentagon(h3Address));
            for (final Long index : indexes) {
                assertTrue(handler.is_pentagon(index));
                assertEquals(h3Core.isPentagon(index), handler.is_pentagon(index));
            }
        }
    }

    @Test
    public void testh3_to_geo() {
        assertNull(handler.cell_to_lat_lng((Long)null));

        final Random r = new Random();
        for (int i=0; i < 1000; ++i) {
            final double latitude = Math.random() * 180.0 - 90.0;
            final double longitude = Math.random() * 360.0 - 180.0;

            final int res = r.nextInt(15);

            // The centroid geo returns by a centroid is the centroid itself.
            final Long h3 = handler.lat_lng_to_cell(latitude, longitude, res);
            final List<Double> geo = handler.cell_to_lat_lng(h3);
            final Long centroid = handler.lat_lng_to_cell(geo.get(0), geo.get(1), res);
 
            assertEquals(handler.cell_to_lat_lng(centroid), geo);

            final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, res);
            final List<Double> geoFromAddress = handler.cell_to_lat_lng(h3Address);
            final Long centroidAddr = handler.lat_lng_to_cell(geo.get(0), geo.get(1), res);

            assertEquals(handler.cell_to_lat_lng(centroidAddr), geoFromAddress);
        }
    }

    @Test
    public void testcell_to_lat_lng_wkt() {
        assertNull(handler.cell_to_lat_lng_wkt((Long)null));

        final double latitude = 50.0;
        final double longitude = -43;
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 4);

        assertEquals(handler.cell_to_lat_lng_wkt(h3), "POINT (-42.941921 50.166306)");
    }

    /** Tests get_resolution functions. */
    @Test
    public void testget_resolution() {
        final double latitude = 50.0;
        final double longitude = -43;

        assertNull(handler.get_resolution((Long)null));
        assertNull(handler.get_resolution((String)null));
        
        for (int i = 0; i < 16; ++i) {
            final Long h3 = handler.lat_lng_to_cell(latitude, longitude, i);
            final Long h3Address = handler.lat_lng_to_cell(latitude, longitude, i);
            assertEquals(i, handler.get_resolution(h3));
            assertEquals(i, handler.get_resolution(h3Address));
        }
    }

    @Test
    public void test_get_base_cell_number() {
        final double latitude = 40.0;
        final double longitude = -42;
        
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 5);
        final Long h3Address = handler.lat_lng_to_cell(latitude, longitude, 5);

        assertNull(handler.get_base_cell_number((Long)null));
        assertNull(handler.get_base_cell_number((String)null));

        assertEquals(handler.get_base_cell_number(h3), handler.get_base_cell_number(h3Address));
        assertEquals(h3Core.getBaseCellNumber(h3), handler.get_base_cell_number(h3Address));
    }

    @Test
    public void teststring_to_h3_two_ways() {
        final double latitude = Math.random() * 180.0 - 90.0;
        final double longitude = Math.random() * 360.0 - 180.0;
        
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 5);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, 5);

        assertEquals(h3, handler.string_to_h3(h3Address));
        assertEquals(h3Address, handler.h3_to_string(h3));

        assertNull(handler.string_to_h3(null));
        assertNull(handler.h3_to_string(null));
    }


    /** Tests cell_to_boundary_wkt functions  */
    @Test
    public void testcell_to_boundary_wkt() {
        assertNull(handler.cell_to_boundary_wkt((Long)null));
        assertNull(handler.cell_to_boundary_wkt((String)null));

        final double latitude = 50.0;
        final double longitude = -43;
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 4);
        final Long h3Address = handler.lat_lng_to_cell(latitude, longitude, 4);

        final String[] boundaries = { "POINT (-43.038419 50.388228)", 
          "POINT (-43.296298 50.211244)", 
          "POINT (-43.199289 49.989297)", 
          "POINT (-42.846071 49.943733)", 
          "POINT (-42.587347 50.120176)", 
          "POINT (-42.682672 50.342723)"
        };

        assertEquals( Arrays.asList(boundaries), 
                      handler.cell_to_boundary_wkt(h3));
        assertEquals( Arrays.asList(boundaries), 
                      handler.cell_to_boundary_wkt(h3Address));
    } 

    @Test
    public void testgrid_path_cells() throws Exception {
        final double latitude = 52.0;
        final double longitude = -4.3;

        for (int res = 2; res < 16; ++res) {
            final Long h3 = handler.lat_lng_to_cell(latitude, longitude, res);
            for (final Long index : handler.grid_disk(h3, 5)) {
                final List<Long> line = handler.grid_path_cells(h3, index);
                assertEquals(line.get(0), h3);
                assertEquals(line.get(line.size() -1), index);
                
                for (int i = 0; i < line.size(); ++i) {
                    assertEquals(i, handler.grid_distance(h3, line.get(i)));
                }
            }

            final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, res);
            for (final String index : handler.grid_disk(h3Address, 5)) {
                final List<String> lineAddr = handler.grid_path_cells(h3Address, index);
                assertEquals(lineAddr.get(0), h3Address);
                assertEquals(lineAddr.get(lineAddr.size() -1), index);
                
                for (int i = 0; i < lineAddr.size(); ++i) {
                    assertEquals(i, handler.grid_distance(h3Address, lineAddr.get(i)));
                }
            }
        }
        assertNull(handler.grid_path_cells(handler.lat_lng_to_cell(latitude, longitude, 5), null));
        assertNull(handler.grid_path_cells((Long)null, handler.lat_lng_to_cell(latitude, longitude, 3)));
        assertNull(handler.grid_path_cells((String)null, handler.lat_lng_to_cell_address(latitude, longitude, 3)));
    }

    @Test
    public void testh3_parent() {
        final double latitude = 52.0;
        final double longitude = -4.3;

        final int res = 14;
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, res);
        final List<Long> parents = handler.cell_to_parents(h3);

        assertEquals(res, parents.size());

        for (int i = 0; i < res; ++i) {
            assertEquals(res - i - 1, handler.get_resolution(parents.get(i)));
        }

        final Long directParent = handler.cell_direct_parent(h3);
        assertEquals(handler.get_resolution(h3) -1, 
                     handler.get_resolution(directParent));
    }

    @Test
    public void testcell_to_center_child() {
        final double latitude = 52.0;
        final double longitude = -4.3;

        final int res = 5;
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, res);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, res);

        assertNull(handler.cell_to_center_child((Long)null, res + 1));
        assertNull(handler.cell_to_center_child((String)null, res + 1));
        assertNull(handler.cell_to_center_child(h3, null));
        assertNull(handler.cell_to_center_child(h3Address, null));

        assertEquals(h3Core.cellToCenterChild(h3, res + 1),
                    handler.cell_to_center_child(h3, res + 1));
        assertEquals(h3Core.cellToCenterChild(h3Address, res + 1),
                    handler.cell_to_center_child(h3Address, res + 1));
    }

    @Test
    public void testcompact_uncompact() {
        final double latitude = 52.0;
        final double longitude = -4.3;

        assertNull(handler.compact_cells(null));
        assertNull(handler.compact_cell_addresses(null));
        assertNull(handler.uncompact_cells(null, 4));
        assertNull(handler.uncompact_cell_addresses(null, 4));

        final int neighborhood = 4;

        for (int res = 5; res <= 10; ++res) {

            final Long h3 = handler.lat_lng_to_cell(latitude, longitude, res);
            final List<Long> h3List  = handler.grid_disk(h3, neighborhood);

            final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, res);
            final List<String> h3Addresses  = handler.grid_disk(h3Address, neighborhood);

            final List<Long> compacted = h3Core.compactCells(h3List);
            final List<String> compactedAddress = h3Core.compactCellAddresses(h3Addresses);

            assertEquals(compacted, handler.compact_cells(h3List));
            assertEquals(compactedAddress, h3Core.compactCellAddresses(h3Addresses));

            assertEquals(h3Core.uncompactCells(compacted, res + 3), 
                            handler.uncompact_cells(compacted, res + 3 ));
            assertEquals(h3Core.uncompactCellAddresses(compactedAddress, res + 3), 
                            handler.uncompact_cell_addresses(compactedAddress, res + 3));
        }
    }

    @Test
    public void testh3_descendants() {
        final double latitude = 52.0;
        final double longitude = -4.3;

        final int res = 5;
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, res);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, res);
        
        for (int childRes = res + 1; childRes < res + 5; ++childRes) {
            for (final Long c : handler.cell_to_children(h3, childRes)) {
                assertEquals(handler.get_resolution(c), childRes);
                assertEquals(h3, handler.cell_to_parent(c, res));
            }
        }

        for (int childRes = res + 1; childRes < res + 5; ++childRes) {
            for (final String c : handler.cell_to_children(h3Address, childRes)) {
                assertEquals(handler.get_resolution(c), childRes);
                assertEquals(h3Address, handler.cell_to_parent(c, res));
            }
        }

        for (final Long desc : handler.cell_to_descendants(h3, 5)) {
            assertTrue(handler.get_resolution(desc) > res &&
                      handler.get_resolution(desc) <= res + 5);
        }

        for (final String desc : handler.cell_to_descendants(h3Address, 5)) {
            assertTrue(handler.get_resolution(desc) > res &&
                      handler.get_resolution(desc) <= res + 5);
        }
    }


    /** Tests cell_to_boundary functions as well as cell_to_boundary_sys. */
    @Test
    public void testcell_to_boundary() {
        assertNull(handler.cell_to_boundary((Long)null, ","));
        assertNull(handler.cell_to_boundary((String)null, ","));
        assertNull(handler.cell_to_boundary_sys((Long)null, LNG));
        assertNull(handler.cell_to_boundary_sys((String)null, LNG));
        
        final double latitude = 50.0;
        final double longitude = -43;
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 4);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, 4);

        assertNull(handler.cell_to_boundary_sys(h3, null));
        assertNull(handler.cell_to_boundary_sys(h3Address, null));

        final List<Double> resultLat = handler.cell_to_boundary_sys(h3, LAT);
        final List<Double> resultLng = handler.cell_to_boundary_sys(h3, LNG);
        final List<Double> resultLatAddr = handler.cell_to_boundary_sys(h3Address, LAT);
        final List<Double> resultLngAddr = handler.cell_to_boundary_sys(h3Address, LNG);

        Assertions.assertThrows(IllegalArgumentException.class,
                        () -> handler.cell_to_boundary_sys(h3, "unk"));

        for (int i = 0; i < 6; ++i) {
            
            final String[] splittedResult = handler.cell_to_boundary(h3, ",")
                                             .get(i)
                                             .split(",");
            assertEquals(h3Core.cellToBoundary(h3).get(i).lat , 
                            Double.parseDouble(splittedResult[0]), 
                            1e-4);

            assertEquals(h3Core.cellToBoundary(h3).get(i).lng , 
                            Double.parseDouble(splittedResult[1]),
                            1e-4);

            assertEquals(h3Core.cellToBoundary(h3Address).get(i).lat , 
                            Double.parseDouble(splittedResult[0]), 
                            1e-4);
            assertEquals(h3Core.cellToBoundary(h3Address).get(i).lng , 
                            Double.parseDouble(splittedResult[1]),
                            1e-4);

            assertEquals(h3Core.cellToBoundary(h3).get(i).lat, 
                            resultLat.get(i),
                            1e-4);
            assertEquals(h3Core.cellToBoundary(h3).get(i).lng,
                            resultLng.get(i),
                            1e-4);

            assertEquals(resultLatAddr.get(i), resultLat.get(i), 1e-4);
            assertEquals(resultLngAddr.get(i), resultLng.get(i), 1e-4);
        }       
    }

    @Test
    public void testis_valid_cell() {
        final double latitude = Math.random() * 180.0 - 90.0;
        final double longitude = Math.random() * 360.0 - 180.0;
        
        final Long h3 = handler.lat_lng_to_cell(latitude, longitude, 4);
        final String h3Address = handler.lat_lng_to_cell_address(latitude, longitude, 4);

        assertTrue(handler.is_valid_cell(h3));
        assertTrue(handler.is_valid_cell(h3Address));
        assertFalse(handler.is_valid_cell(4L));
        assertFalse(handler.is_valid_cell("4"));
        assertFalse(handler.is_valid_cell((Long)null));
        assertFalse(handler.is_valid_cell((String)null));
    }

    @Test
    public void testis_res_class_iii() {
        final long h3False = 622506764662964223L;
        final long h3True = 617420388352917503L;

        assertFalse(handler.is_res_class_iii(h3False));
        assertTrue(handler.is_res_class_iii(h3True));

        assertFalse(handler.is_res_class_iii(handler.h3_to_string(h3False)));
        assertTrue(handler.is_res_class_iii(handler.h3_to_string(h3True)));

        assertFalse(handler.is_res_class_iii((Long)null));
        assertFalse(handler.is_res_class_iii((String)null));
    }

    @Test
    public void testpolygon_to_cells() throws Exception {
        final String polygonWKT = 
            "POLYGON ((1.444209 43.604652, -1.553621 47.218371, 3.05726 50.62925, 2.349014 48.864716, 7.27178 43.6961, 1.444209 43.604652))";
        final List<LatLng> latLngPoints = List.of(new LatLng(43.604652,1.444209),
                                                      new LatLng(47.218371, -1.553621),
                                                      new LatLng(50.62925, 3.05726),
                                                      new LatLng(48.864716, 2.349014),
                                                      new LatLng(43.6961, 7.27178),
                                                      new LatLng(43.604652, 1.444209));
        final String polygonWKTAlt = 
            "POLYGON ((1.444209 43.604652, -1.553621 47.218371, 3.05726 50.62925, 2.349014 48.864716, 7.27178 43.6961, 1.444209 43.604652))";

        final List<List<LatLng>>  empty = new LinkedList<>();

        for (int i = 0; i <= 5 ;++i) {
            assertEquals(h3Core.polygonToCells(latLngPoints, empty, i), handler.polygon_to_cells(polygonWKT, i));
            assertEquals(handler.polygon_to_cells(polygonWKT, i), handler.polygon_to_cells(polygonWKTAlt, i));
        }
    }

    // @Test
    public void testpolygon_to_cell_addresses() throws Exception{
        final String polygonWKT = 
            "POLYGON ((1.444209 43.604652, -1.553621 47.218371, 3.05726 50.62925, 2.349014 48.864716, 7.27178 43.6961, 1.444209 43.604652))";
        final List<LatLng> latLngPoints = List.of(new LatLng(43.604652,1.444209),
                                                      new LatLng(47.218371, -1.553621),
                                                      new LatLng(50.62925, 3.05726),
                                                      new LatLng(48.864716, 2.349014),
                                                      new LatLng(43.6961, 7.27178),
                                                      new LatLng(43.604652, 1.444209));

        final String polygonWKTAlt = 
            "POLYGON ((1.444209 43.604652, -1.553621 47.218371, 3.05726 50.62925, 2.349014 48.864716, 7.27178 43.6961, 1.444209 43.604652))";

        final List<List<LatLng>> empty = new LinkedList<>();

        for (int i = 0; i <= 5 ;++i) {
            assertEquals(h3Core.polygonToCellAddresses(latLngPoints, empty, i), handler.polygon_to_cell_addresses(polygonWKT, i));
            assertEquals(handler.polygon_to_cell_addresses(polygonWKT, i), handler.polygon_to_cell_addresses(polygonWKTAlt, i));
        }

        final String multiPolygonWKT = "MULTIPOLYGON (((-112.13417747722622 40.48305525857179, -112.07601701040902 40.40458400920371, -112.14044403871432 40.327152363986706, -112.08236892255083 40.24855573342874, -111.95994978862437 40.24734787912595, -111.9020429024643 40.16858374280796, -111.77968666606233 40.16717680240547, -111.71504793475805 40.244544265218366, -111.59256744172903 40.2429483824467, -111.52771940855304 40.32016865585556, -111.5854551484613 40.39901752493811, -111.5205009128757 40.47612241452489, -111.57832080748169 40.55484644293405, -111.70118207844375 40.556423521944474, -111.75917340878016 40.63497990483772, -111.88210082412408 40.63635632640663, -111.9468454993574 40.55918644383781, -112.06964540143417 40.56037241099826), (-111.88876877847025 40.48067243111808, -111.76603234430006 40.47928592430941, -111.70812573681557 40.40060396307876, -111.77287006403081 40.32335096944806, -111.89541610612225 40.32474764935123, -111.95340778646954 40.403387143348745)))";
        final List<LatLng> multiPolyLatLngPoints = List.of( 
            new LatLng(40.48305525857179, -112.13417747722622),
            new LatLng(40.40458400920371, -112.07601701040902),
            new LatLng(40.327152363986706, -112.14044403871432),
            new LatLng(40.24855573342874, -112.08236892255083),
            new LatLng(40.24734787912595, -111.95994978862437),
            new LatLng(40.16858374280796, -111.9020429024643),
            new LatLng(40.16717680240547, -111.77968666606233),
            new LatLng(40.244544265218366, -111.71504793475805),
            new LatLng(40.2429483824467, -111.59256744172903),
            new LatLng(40.32016865585556, -111.52771940855304),
            new LatLng(40.39901752493811, -111.5854551484613),
            new LatLng(40.47612241452489, -111.5205009128757),
            new LatLng(40.55484644293405, -111.57832080748169),
            new LatLng(40.556423521944474, -111.70118207844375),
            new LatLng(40.63497990483772, -111.75917340878016),
            new LatLng(40.63635632640663, -111.88210082412408),
            new LatLng(40.55918644383781, -111.9468454993574),
            new LatLng(40.56037241099826, -112.06964540143417)
        );

        final List<List<LatLng>> holeLists = new LinkedList<>();
        holeLists.add(List.of(
            new LatLng(40.48067243111808, -111.88876877847025),
            new LatLng(40.47928592430941, -111.76603234430006),
            new LatLng(40.40060396307876, -111.70812573681557),
            new LatLng(40.32335096944806, -111.77287006403081),
            new LatLng(40.32474764935123, -111.89541610612225),
            new LatLng(40.403387143348745, -111.95340778646954)
        ));

        for (int i = 0; i <= 9 ;++i) {
            assertEquals(h3Core.polygonToCellAddresses(multiPolyLatLngPoints, holeLists, i), handler.polygon_to_cell_addresses(multiPolygonWKT, i));
        }
        
    }

    public void testcells_to_multipolygon() {
        final List<Long> h3Indexes = List.of( 613498908116516863L,
                                               613499565410091007L,
                                               613498908185722879L,
                                               613499565420576767L,
                                               613498908183625727L,
                                               613499565418479615L,
                                               613498908145876991L,
                                               613498908223471615L,
                                               613498907535605759L,
                                               613498908120711167L,
                                               613498908124905471L,
                                               613498908217180159L,
                                               613498908112322559L,
                                               613498908156362751L,
                                               613498908215083007L,
                                               613498908221374463L,
                                               613498908118614015L,
                                               613498908212985855L,
                                               613498908154265599L,
                                               613499565414285311L,
                                               613499565412188159L,
                                               613498908158459903L,
                                               613499565416382463L,
                                               613498908225568767L,
                                               613498908219277311L,
                                               613499565422673919L );
        final List<List<List<LatLng>>> polygon1 = h3Core.cellsToMultiPolygon(h3Indexes, true);
        final String polygon2 = handler.cells_to_multi_polygon(h3Indexes, true);
        final String polygon2Split[] = polygon2.substring("MULTIPOLYGON (((".length(), polygon2.length() - 3).split("\\)\\), \\(\\(");

        assertEquals(polygon2Split.length, polygon1.size());

        for (int i = 0; i < polygon2Split.length; ++i) {
            final String[] splitted = polygon2Split[i].split(",");
            assertEquals(splitted.length, polygon1.get(i).get(0).size());

            for (int j = 0; j < splitted.length ;  ++j) {
                final String[] latlng = splitted[j].trim().split(" ");
                assertEquals(Double.parseDouble(latlng[0]), polygon1.get(i).get(0).get(j).lat, 1e-3);
                assertEquals(Double.parseDouble(latlng[1]), polygon1.get(i).get(0).get(j).lng, 1e-3);

            }

        }
        assertEquals(polygon2Split.length, polygon1.size());
        assertEquals(2, polygon2Split.length);
        
    }
}
