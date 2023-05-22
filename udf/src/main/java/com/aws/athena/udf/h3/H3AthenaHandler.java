package com.aws.athena.udf.h3;

import com.amazonaws.athena.connector.lambda.handlers.UserDefinedFunctionHandler;
import com.uber.h3core.AreaUnit;
import com.uber.h3core.H3Core;
import com.uber.h3core.LengthUnit;
import com.uber.h3core.util.LatLng;
import com.uber.h3core.util.CoordIJ;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import mil.nga.sf.LineString;
import mil.nga.sf.MultiPolygon;
import mil.nga.sf.Point;
import mil.nga.sf.Polygon;
import mil.nga.sf.wkt.GeometryReader;

/** Lambda that hosts H3 UDFs */
public class H3AthenaHandler extends UserDefinedFunctionHandler {

    private final H3Core h3Core;
    private static final String SOURCE_TYPE = "h3_athena_udf_handler";
    private static final String LAT = "lat";
    private static final String LNG = "lng";
    private static final String POLYGON = "POLYGON";
    private static final String MULTIPOLYGON = "MULTIPOLYGON";

    public H3AthenaHandler() throws IOException {
        super(SOURCE_TYPE);
        this.h3Core = H3Core.newInstance();
    }

    /** Indexes the location at the specified resolution, returning index of the cell as number containing
     *  the location.
     *   @param lat the latitude of the location
     *   @param lng the longitude of the location
     *   @param res the resolution 0 &lt;= res &lt;= 15
     *   @return The H3 index as a long. Null when one of the parameter is null.
     *   @throws IllegalArgumentException latitude, longitude, or resolution are out of range.
     */
    public Long lat_lng_to_cell(Double lat, Double lng, Integer res) {
        return (lat == null || lng == null || res == null) ? null : 
            h3Core.latLngToCell(lat, lng, res);
    }

    /** Indexes the location at the specified resolution, returning index of the cell as String containing
     *  the location.
     *   @param lat the latitude of the location
     *   @param lng the longitude of the location
     *   @param res the resolution 0 &lt;= res &lt;= 15
     *   @return The H3 index as a long. Null when one of the parameter is null.
     *   @throws IllegalArgumentException latitude, longitude, or resolution are out of range.
     */
    public String lat_lng_to_cell_address(Double lat, Double lng, Integer res) {
        return (lat == null || lng == null || res == null) ? null : 
            h3Core.latLngToCellAddress(lat, lng, res);
    }

    /** Finds the centroid of an index, and returns an array list of coordinates representing latitude and longitude 
     *  respectively.
     *  @param h3 the H3 index
     *  @return List of Double of size 2 representing latitude and longitude. Null when the index is null.
     *  @throws IllegalArgumentException when the index is out of range
     */
    public List<Double> cell_to_lat_lng(Long h3) {
        if (h3 == null) { return null; } 

        final LatLng coord = h3Core.cellToLatLng(h3);
        return new ArrayList<>(Arrays.asList(coord.lat, coord.lng));
    }

     /** Finds the centroid of an index, and returns an array list of coordinates representing latitude and longitude 
     *  respectively.
     *  @param h3 the H3 index
     *  @return List of Double of size 2 representing latitude and longitude. Null when the index is null.
     *  @throws IllegalArgumentException when the index is out of range
     */
    public List<Double> cell_to_lat_lng(String h3) {
        if (h3 == null) { return null; } 
        
        final LatLng coord = h3Core.cellToLatLng(h3);
        return new ArrayList<>(Arrays.asList(coord.lat, coord.lng));
    }

    /** Finds the centroid of an index, and returns a WKT of the centroid.
     *  @param h3 the H3 index
     *  @return the WKT of the centroid of an H3 index. Null when the index is null;
     *  @throws IllegalArgumentException when the index is out of range
     */
    public String cell_to_lat_lng_wkt(Long h3) {
        if (h3 == null) { return null; }
    
        final LatLng coord = h3Core.cellToLatLng(h3);
        return wktPoint(coord); 
    }

    /** Finds the centroid of an index, and returns a WKT of the centroid.
     *  @param h3 the H3 index
     *  @return the WKT of the centroid of an H3 index. Null when the index is null;
     *  @throws IllegalArgumentException when the index is out of range
     */
    public String cell_to_lat_lng_wkt(String h3) {
        if (h3 == null) { return null; }
    
        final LatLng coord = h3Core.cellToLatLng(h3);
        return wktPoint(coord);      
    }

    /** Finds the boundary of an H3 cell.
     * @param h3 the H3 cell
     * @param sep the separator between the latitude and longitude.
     * @return the list of points representing the points in the boundary. Each returned list consists of two members, the first one is latitude, and the 
     * second one is longitude. Null when the parameter is null.
     * @throws IllegalArgumentException  when address is out of range 
     */
    public List<String> cell_to_boundary(Long h3, String sep){
        if (h3 == null) { return null;}
    
        return h3Core.cellToBoundary(h3).stream()
            .map(n-> pointsListStr(n, sep))
            .collect(Collectors.toList());
    }

    /** Finds the boundary of an H3 index for a given coordinate system (lat=latitude, or lng=longitude)
     * @param h3 the H3 index
     * @param coordSys the coordinate system, lng or lat.
     * @return the list of points representing the points in the boundary. Each returned list consists of two members, the first one is latitude, and the 
     * second one is longitude. Null when the parameter is null.
     * @throws IllegalArgumentException  when address is out of range  or when coordSys is unknown.
     */
    public List<Double> cell_to_boundary_sys(Long h3, String coordSys) {
        if (h3 == null || coordSys == null) { return null; }
        else if (!LAT.equals(coordSys) && !LNG.equals(coordSys)) { throw new IllegalArgumentException("Unknown coord sys"); }
        
        return h3Core.cellToBoundary(h3).stream()
                .map(n-> coordSys.equals(LAT) ? n.lat : n.lng)
                .collect(Collectors.toList());
    }

    /** Finds the boundary of an H3 index. Returns the result in an array of WKT points.
     * @param h3 the H3 index
     * @return the list of points representing the points in the boundary. Each returned list consists of a WKT representation of the point.
     * Null when h3 is null.
     * @throws IllegalArgumentException  when address is out of range.
     */
    public List<String> cell_to_boundary_wkt(Long h3){
        return (h3 == null) ? null :
            h3Core.cellToBoundary(h3).stream()
                .map(H3AthenaHandler::wktPoint)
                .collect(Collectors.toList());
    }
    
    /** Finds the boundary of an H3 index in a string form.
     * @param h3 the H3 index
     * @return the list of points representing the points in the boundary. Each returned list consists of two members, the first one is latitude, and the 
     * second one is longitude . Null when the h3Address is null.
     * @throws IllegalArgumentException  when address is out of range.
     */
    public List<String> cell_to_boundary_wkt(String h3){
        return (h3 == null) ? null :
            h3Core.cellToBoundary(h3).stream()
                .map(H3AthenaHandler::wktPoint)
                .collect(Collectors.toList());
    
    }
    
    /** Finds the boundary of an H3 address for a given coordinate system (lng=longitude, lat=latitude).
     * @param h3Address the H3 address 
     * @param coordSys the coordinate system, lng or lat.
     * @return the list of points representing the points in the boundary. Each returned list consists of a WKT representation of the point.
     * Null when h3Address is null.
     * @throws IllegalArgumentException  when address is out of range. 
     */
    public List<Double> cell_to_boundary_sys(String h3Address, String coordSys){
        if (h3Address == null || coordSys == null) { return null; }
        else if (!LAT.equals(coordSys) && !LNG.equals(coordSys)) { throw new IllegalArgumentException("Unknown coord sys"); }
        
        return h3Core.cellToBoundary(h3Address).stream()
                .map(n-> coordSys.equals(LAT) ? n.lat : n.lng)
                .collect(Collectors.toList());
    }

    /** Finds the boundary of an H3 address.
     * @param h3 the H3 index
     * @param sep the separator between the latitude and longitude.
     * @return the list of points representing the points in the boundary. Each returned list consists of two members, the first one is latitude, and the 
     * second one is longitude. Null when the parameter is null.
     * @throws IllegalArgumentException  when address is out of range 
     */
    public List<String> cell_to_boundary(String h3Address, String sep){
        return (h3Address == null || sep == null) ? null : 
            h3Core.cellToBoundary(h3Address).stream()
                            .map(n-> pointsListStr(n, sep))
                            .collect(Collectors.toList());
    }

    /** Returns the resolution of an index.
     *  @param h3 the H3 index.
     *  @return the resolution. Null when h3 is null.
     *  @throws  IllegalArgumentException  when index is out of range.
     */
    public Integer get_resolution(Long h3){
        return h3 == null ? null : h3Core.getResolution(h3);
    }

    /** Returns the resolution of an index.
     *  @param h3Address the H3 index in string form.
     *  @return the resolution. Null when h3Address is null.
     */
    public Integer get_resolution(String h3Address){
        return h3Address == null ? null : h3Core.getResolution(h3Address);
    }

    /** Returns the base cell number of the index.
     * @param h3 the index. 
     * @return the base cell number of the index. Null when h3 is null.
     */
    public Integer get_base_cell_number(Long h3){
        return h3 == null ? null : h3Core.getBaseCellNumber(h3);
    }

    /** Returns the base cell number of the index in string form
     * @param h3Address the address. 
     * @return the base cell number of the index. Null when h3Address is null.
     * @throws IllegalArgumentException when index is out of range.
     */
    public Integer get_base_cell_number(String h3Address){
        return h3Address == null ? null : h3Core.getBaseCellNumber(h3Address);
    }

    /** Converts the string representation to H3Index (uint64_t) representation.
    *   @param h3Address the h3 address.
    *   @return the string representation. Null when h3Address is null.
    */
    public Long string_to_h3(String h3Address){
        return h3Address == null ? null : h3Core.stringToH3(h3Address);
    }

    /** Converts the H3Index representation of the index to the string representation. str must be at least of length 17.
     *  @param the h3 the h3 index.
     *  @return the string representation if the index or Null when h3 is null.
     */
    public String h3_to_string(Long h3) {
        return h3 == null ? null : h3Core.h3ToString(h3);
    }
    
    /** Returns whether an h3 value is valid.
     *  @param h3 the h3 index.
     *  @return whether or not the index is in the range. false when h3 is null.
     */
    public Boolean is_valid_cell(Long h3) {
        return h3 != null && h3Core.isValidCell(h3);
    }
    
    /** Returns whether an h3 address is valid.
     *  @param h3Address the h3 address to check.
     *  @return whether the h3 address is a valid h3 address. false when h3Address is null.
     */
    public Boolean is_valid_cell(String h3Address){
        return  h3Address != null && h3Core.isValidCell(h3Address);
    }

    /** Returns whether an h3 index is ResClassIII. 
     *  @param h3 the h3 index to check.
     *  @return whether the h3 index is resClassIII. False when h3 is null.
     */
    public Boolean is_res_class_iii(Long h3){
        return h3 != null && h3Core.isResClassIII(h3);
    }
    
    /** Returns whether an h3 address is ResClassIII.
     * @param h3Address the h3 address to check.
     * @return whether the h3 address is resClassIII. False when h3 is null.
     */
    public Boolean is_res_class_iii(String h3Address) {
        return h3Address != null &&  h3Core.isResClassIII(h3Address);
    }

    /** Returns whether an H3 index is a pentagon or not.
     * @param h3 the h3 index.
     * @return whether or not the h3 item is pentagon.
     */
    public Boolean is_pentagon(Long h3){
        return h3 != null && h3Core.isPentagon(h3);
    }

    /** Returns whether an H3 address is a pentagon or not.
     * @param h3 the h3 address.
     * @return whether or not the h3 item is pentagon.
     */
    public Boolean is_pentagon(String h3Address){
        return h3Address != null && h3Core.isPentagon(h3Address);
    }

    /** Finds all icosahedron faces intersected by a given H3 index.
     *   @param h3 h3 index.
     *   @return all icosahedron faces. Null when h3 is null.
     */
    public List<Integer> get_icosahedron_faces(Long h3){
        return h3 == null ? null : new ArrayList<>(h3Core.getIcosahedronFaces(h3));
    }

    /** Find all icosahedron faces intersected by a given H3 address.
     *   @param h3Address the h3 address. 
     *   @return the list of icosahedron faces. Null when h3Address is null.
     */
    public List<Integer> get_icosahedron_faces(String h3Address){
        return h3Address == null ? null : new ArrayList<>(
                                            h3Core.getIcosahedronFaces(h3Address));
   
    }

    /**  Produces indices within k distance of the origin index.
     *   @param origin the origin H3 index.
     *   @param k the distance.
     *   @return the h3 indexes inside the ring.
     */
    public List<Long> grid_disk(Long origin, Integer k){
        return origin == null || k == null ? null : h3Core.gridDisk(origin, k);

    }


    /** k-rings produces indices within k distance of the origin H3 address.
     *   @param origin the origin H3 address.
     *   @param k the distance.
     *   @param the addresses inside the ring.
     */
    public List<String> grid_disk(String origin, Integer k){
        return origin == null || k == null ? null : h3Core.gridDisk(origin, k);
    }

    
    /** Produces the hollow hexagonal ring centered at origin with sides of length k.
     *  @param h3 the h3 Index.
     *  @param k the length of the ring.
     *  @return the h3 indexes inside the ring.
     */
    public List<Long> grid_ring_unsafe(Long h3, Integer k) {
        return h3 == null || k == null ? null :h3Core.gridRingUnsafe(h3, k);
    }

    /** Produces the hollow hexagonal ring centered at origin with sides of length k.
     *  @param h3Address the h3 Address.
     *  @param k the length of the ring.
     *  @return the h3 addresses inside the ring.
     */
    public List<String> grid_ring_unsafe(String h3Address, Integer k)  {
        return h3Address == null || k == null ? null :  h3Core.gridRingUnsafe(h3Address, k);
    }

    /** Given two H3 indexes, return the line of indexes between them (inclusive).
     *   @param start the h3 index of start of the line.
     *   @param end the h3 index of end of the line.
     *   @return the h3 indexes. 
     */  
    public List<Long> grid_path_cells(Long start, Long end) {
        return (start == null || end == null) ? null : h3Core.gridPathCells(start, end);
    }

    /** Given two H3 indexes, return the line of indexes between them (inclusive).
     *   @param start the h3 address of start of the line.
     *   @param end the h3 address of end of the line.
     *   @return the h3 addresses 
     */
    public List<String> grid_path_cells(String start, String end)  {
        return (start == null || end == null) ? null : h3Core.gridPathCells(start, end);
    }
    
     /** Returns the distance in grid cells between the two addresses.
     *   Returns a negative number if finding the distance failed. Finding the distance can fail because the two indexes are not 
     *  comparable (different resolutions), too far apart, or are separated by pentagonal distortion.
     *  @param a first cell.
     *  @param b second cell.
     *  @return the distance.
     */
    public Long grid_distance(Long a, Long b){
        if (a == null || b == null) { return  null; }
        else if (h3Core.getResolution(a) != h3Core.getResolution(b)) {
            throw new IllegalArgumentException("Cannot compute distance of two indexes from different resolutions");
        }
        return h3Core.gridDistance(a, b);
    }

    /** Returns the distance in grid cells between the two addresses.
     *   Returns a negative number if finding the distance failed. Finding the distance can fail because the two indexes are not 
     *  comparable (different resolutions), too far apart, or are separated by pentagonal distortion.
     *  @param a first cell.
     *  @param b second cell.
     *  @return the distance.
     */
    public Long grid_distance(String a, String b) {
        if (a == null || b == null) { return  null; }
        else if (h3Core.getResolution(a) != h3Core.getResolution(b)) {
            throw new IllegalArgumentException("Cannot compute distance of two indexes from different resolutions");
        }
        return h3Core.gridDistance(a, b);
    }

    /** Returns the direct parent (parent resolution = resolution -1) index containing h.
      * @param h the h3 index.
      * @param parentRes parent resolution.
      * @return parent index containing h or null when h3 is null.
      */
    public Long cell_to_parent(Long h) {
        return h == null ? null : h3Core.cellToParent(h, h3Core.getResolution(h) - 1);
    }

    /** Returns the parent (coarser) index containing h.
      * @param h3 the h3 index.
      * @param parentRes parent resolution.
      * @return parent index containing h or null when h3 is null.
      */
    public Long cell_to_parent(Long h3, Integer parentRes) {
        return h3 == null || parentRes == null ? null : h3Core.cellToParent(h3, parentRes);
    }

    /** Returns all the parents up to resolution 0. 
      * @param h3 the h3 index.
      * @return parent index containing h or null when h3 is null.
      */
    public List<Long> cell_to_parents(Long h3) {
        if (h3 == null) { return null; }
        
        final List<Long> result = new LinkedList<>();
        for (int res = get_resolution(h3) - 1; res >= 0 ; --res) {
            result.add(cell_to_parent(h3, res));
        }
        return result;
    }

    /** Returns the parent (coarser) index containing h3Address. 
     *  @param h3Address the h3 address of an h3 cell.
     *  @param parentRes the parent resolution.
     *  @return parent address containing h3Address or null when h3Address is null.
     * 
     */
    public String cell_to_parent(String h3Address, Integer parentRes) {
        return (h3Address == null || parentRes == null) ? null :
            h3Core.cellToParentAddress(h3Address, parentRes);
    }

    /** Returns all the parents up to resolution 0. 
      * @param h3Address the h3 address.
      * @return parent index containing h or null when h3 is null.
      */
    public List<String> cell_to_parents(String h3Address) {
        if (h3Address == null) { return null; }
        
        final List<String> result = new LinkedList<>();
        for (int res = h3Core.getResolution(h3Address) - 1; res >= 0 ; --res) {
            result.add(cell_to_parent(h3Address, res));
        }
        return result;
    }

    /** Returns the direct parent (parent resolution = resolution -1) index containing h.
      * @param h the h3 index.
      * @param parentRes parent resolution.
      * @return parent adress containing h or null when h3 is null.
      */
    public Long cell_direct_parent(Long h) {
        return h == null ? null : cell_to_parent(h, get_resolution(h) - 1);
    }

    /** Returns the direct parent (parent resolution = resolution -1) index containing h.
      * @param h the h3 index.
      * @param parentRes parent resolution.
      * @return parent adress containing h or null when h3 is null.
      */
    public String cell_direct_parent(String h) {
        return h == null ? null : cell_to_parent(h, get_resolution(h) - 1);
    }

    /** Populates children with the indexes contained by h at resolution childRes. 
     *  @param h3 the h3 index
     *  @param childRes the children resolution
     *  @return the h3 indexes of the children
     */
    public List<Long> cell_to_children(Long h3, Integer childRes) {
        return h3 == null  || childRes == null ? null : h3Core.cellToChildren(h3, childRes);
    }

    /** Populates descendants with the indexes contained by h at resolution lower than
     *  resolution of h until resolution of h + depth. 
     *  @param h3 the h3 index
     *  @param depth the depth of descendants in term of resolution.
     *  @return the h3 indexes of the children
     */
    public List<Long> cell_to_descendants(Long h3, Integer depth) {
        if (h3 == null || depth == null || depth <= 0) { return null; }
        
        final int resolution = get_resolution(h3);
        final List<Long> result = new LinkedList<>();
        for (int i = 1; i <= depth; ++i) {
            result.addAll(cell_to_children(h3, resolution + i));
        }
        return result;
    }

    /** Populates descendants with the indexes contained by h at resolution lower than
     *  resolution of h until resolution of h + depth. 
     *  @param h3Address the h3 address
     *  @param depth the depth of descendants in term of resolution.
     *  @return the h3 indexes of the children
     */
    public List<String> cell_to_descendants(String h3Address, Integer depth) {
        if (h3Address == null || depth == null || depth <= 0) { return null; }
        
        final int resolution = get_resolution(h3Address);
        final List<String> result = new LinkedList<>();
        for (int i = 1; i <= depth; ++i) {
            result.addAll(cell_to_children(h3Address, resolution + i));
        }
        return result;
    }

    /** Populates children with the indexes contained by h at resolution childRes. 
     *  @param h3 the h3 index
     *  @param childRes the children resolution
     *  @return the h3 addresses of the children.
     */
    public List<String> cell_to_children(String h3Address, Integer childRes) {
        return h3Address == null || childRes == null ? null : h3Core.cellToChildren(h3Address, childRes);
    }

    /** Returns the center child (finer) index contained by h at resolution childRes.
     * @param h3 the h3 index
     * @param childRes the child resolution.
     * @return the h3 index of the center child.
    */
    public Long cell_to_center_child(Long h3, Integer childRes){
        return h3 == null || childRes == null ? null :  h3Core.cellToCenterChild(h3, childRes);
    }

    public List<Long> cell_to_center_descendants(Long h3, Integer depth) {
        if (h3 == null || depth == null) { return null; }
        
        final int resolution = get_resolution(h3);
        final List<Long> result = new LinkedList<>();
        for (int i = 1; i <= depth; ++i) {
            result.add(cell_to_center_child(h3, resolution + i));
        }
        return result;
    }

    public List<String> cell_to_center_descendants(String h3Address, Integer depth) {
        if (h3Address == null || depth == null) { return null; } 
        
        final int resolution = get_resolution(h3Address);
        final List<String> result = new LinkedList<>();
        for (int i = 1; i <= depth; ++i) {
            result.add(cell_to_center_child(h3Address, resolution + i));
        }
        return result;
    }
 
    /** Returns the center child (finer) index contained by h at resolution childRes.
     * @param h3 the h3 index
     * @param childRes the child resolution
     * @return the h3 Address of the center child.
    */
    public String cell_to_center_child(String h3Address, Integer childRes){
        return h3Address == null || childRes == null ? null : h3Core.cellToCenterChild(h3Address, childRes);
    }

    /** Compacts the set h3Set of indexes as best as possible, into the array compacted set. 
     *  This function compacts a set of cells of the same resolution into a set of cells across multiple 
     *  resolutions that represents the same area.
     * 
     *  @param h3 list of h3 indexes 
     *  @return list of h3 indexes after compaction.
     */
    public List<Long> compact_cells(List<Long> h3){
        return h3 == null ? null : h3Core.compactCells(h3);
    }
 
    /** Compacts the set h3Set of indexes as best as possible, into the array compacted set. 
     *  This function compacts a set of cells of the same resolution into a set of cells across multiple 
     *  resolutions that represents the same area.
     *
     *  @param h3Addresses the initial h3 addresses t
     *  @return the list of h3 addresses that compact the initial addresses.
     */
    public List<String> compact_cell_addresses(List<String> h3Addresses) {
        return h3Addresses == null ? null : h3Core.compactCellAddresses(h3Addresses);
    }

    /** This function uncompacts a compacted set of H3 cells to indices of the target resolution.
     *  @param h3 the list of indices, may be in different resolutions
     *  @param res the target resolution.
     *  @param list of indices in the target resolution
     *  @return the list of H3 indexes as a result of uncompaction
    */
    public List<Long> uncompact_cells(List<Long> h3, Integer res) {
        return h3 == null || res == null ? null : h3Core.uncompactCells(h3, res);
    }

    /** This function uncompacts a compacted set of H3 cells to indices of the target resolution.
     *  @param h3 the list of indices, may be in different resolutions
     *  @param res the target resolution.
     *  @param list of indices in the target resolution
     *  @return list of h3 address as result of uncompation
    */
    public List<String> uncompact_cell_addresses(List<String> h3Addresses, Integer res){
        return h3Addresses == null || res == null ? null : h3Core.uncompactCellAddresses(h3Addresses, res);
    }

    /** Receives a polygon WKT without holes, and resolution, and find all H3 objects whose center located inside the polygon
     *  @param polygon the polygon WKT
     *  @param res the resolution.
     *  @return H3 indexes
     *  @throws IOException when failing to read WKT
     */
    public List<Long> polygon_to_cells(String polygonWKT, Integer res) throws IOException {
        final List<Long> result;
        if (polygonWKT == null || res == null) { result = null; } 
        else {
            final String trimmed = polygonWKT.trim();
            if (trimmed.startsWith(POLYGON) && trimmed.endsWith("))")) {
                final Polygon polygon = GeometryReader.readGeometry(trimmed, Polygon.class);
                result = sf_polygon_to_cells(polygon, res);
            } else if (trimmed.startsWith(MULTIPOLYGON) && trimmed.endsWith(")))")) {
                final HashSet<Long> resultSet = new HashSet<Long>();
                final MultiPolygon mp = GeometryReader.readGeometry(trimmed, MultiPolygon.class);
                
                for (final Polygon p : mp.getPolygons()) {
                    resultSet.addAll(sf_polygon_to_cells(p, res));
                }
                
                result = new LinkedList<Long>(resultSet);
            } else {
                throw new IllegalArgumentException("invalid polygonWKT");
            }
        }
        return result;
    }

    /**
     * 
     * @param Polygon the Simple Feature Polygon
     * @param res the resolution
     * @return H3 indexes
     */
    private List<Long> sf_polygon_to_cells(Polygon pg, Integer res) {
        final List<LatLng> exteriorCoordPoints = new LinkedList<>();
        final List<List<LatLng>> holeLists = new LinkedList<>();

        for (final Point p : pg.getExteriorRing().getPoints()) {
            Double lng = p.getX();
            Double lat = p.getY();
            exteriorCoordPoints.add(new LatLng(lat, lng));
        }

        // For each interior ring, create a hole
        for (int r = 0; r < pg.numInteriorRings() ; r++) {
            final LineString ir = pg.getInteriorRing(r);
            final List<LatLng> interiorCoordPoints = new LinkedList<>();
            for (final Point p : ir.getPoints()) {
                Double lng = p.getX();
                Double lat = p.getY();
                interiorCoordPoints.add(new LatLng(lat, lng));
            }
            holeLists.add(interiorCoordPoints);
        }
        return h3Core.polygonToCells(exteriorCoordPoints, holeLists, res);
    }

    /** Receives a polygon WKT without holes, and resolution, and find all H3 objects whose center located inside the polygon
     *  @param polygon the polygon WKT
     *  @param res the resolution.
     *  @return H3 indexes
     *  @throws IOException when failing to read WKT
     */
    public List<String> polygon_to_cell_addresses(String polygonWKT, Integer res) throws IOException {
        final List<String> result;
        if (polygonWKT == null || res == null) { result = null; } 
        else {
            final String trimmed = polygonWKT.trim();
            if (trimmed.startsWith(POLYGON) && trimmed.endsWith("))")) {
                final Polygon polygon = GeometryReader.readGeometry(trimmed, Polygon.class);
                result = sf_polygon_to_cell_addresses(polygon, res);
            } else if (trimmed.startsWith(MULTIPOLYGON) && trimmed.endsWith(")))")) {
                final HashSet<String> resultSet = new HashSet<String>();
                final MultiPolygon mp = GeometryReader.readGeometry(trimmed, MultiPolygon.class);
                
                for (final Polygon p : mp.getPolygons()) {
                    resultSet.addAll(sf_polygon_to_cell_addresses(p, res));
                }
                
                result = new LinkedList<String>(resultSet);
            } else {
                throw new IllegalArgumentException("invalid polygonWKT");
            }
        }
        return result;
    }

    /**
     * 
     * @param Polygon the Simple Feature Polygon
     * @param res the resolution
     * @return H3 indexes
     */
    private List<String> sf_polygon_to_cell_addresses(Polygon pg, Integer res) {
        final List<LatLng> exteriorCoordPoints = new LinkedList<>();
        final List<List<LatLng>> holeLists = new LinkedList<>();

        for (final Point p : pg.getExteriorRing().getPoints()) {
            Double lng = p.getX();
            Double lat = p.getY();
            exteriorCoordPoints.add(new LatLng(lat, lng));
        }

        // For each interior ring, create a hole
        for (int r = 0; r < pg.numInteriorRings() ; r++) {
            final LineString ir = pg.getInteriorRing(r);
            final List<LatLng> interiorCoordPoints = new LinkedList<>();
            for (final Point p : ir.getPoints()) {
                Double lng = p.getX();
                Double lat = p.getY();
                interiorCoordPoints.add(new LatLng(lat, lng));
            }
            holeLists.add(interiorCoordPoints);
        }
        return h3Core.polygonToCellAddresses(exteriorCoordPoints, holeLists, res);
    }
    
     /** Gets a multipolygon WKT given an h3 set.  Either h3 or h3Address parameter can be defined, not both.
     *  @param h3 h3 set.
     *  @param h3Address set.  
     *  @param geoJson whether to return in the format of geoJson
     *  @return WKT Polygon
     */
    private String cellsToMultiPolygon(List<Long> h3, List<String> h3Addresses, Boolean geoJson) {
        final String result;
        if (h3 == null && h3Addresses== null) {
            result =  null;
        } else {
            final List<List<List<LatLng>>> multiPolygon = 
                (h3 == null) ?  h3Core.cellAddressesToMultiPolygon(h3Addresses, geoJson): 
                                h3Core.cellsToMultiPolygon(h3, geoJson);

            final StringBuilder multiPolygonWKT = new StringBuilder("MULTIPOLYGON (");
            boolean firstPolygon = true;    
            for (final List<List<LatLng>> polygon: multiPolygon) {
                if (firstPolygon) {
                    firstPolygon = false;
                }
                else {
                    multiPolygonWKT.append(", ");
                }
                multiPolygonWKT.append("(");

                boolean firstList = true;
                for (final List<LatLng> points: polygon) {
                    if (firstList) { firstList = false; }
                    else { multiPolygonWKT.append(", "); }

                    multiPolygonWKT.append("(");

                    boolean firstPoint = true;
                    for (final LatLng coord: points) {
                        if (firstPoint) {
                            firstPoint = false;
                        }
                        else {
                            multiPolygonWKT.append(", ");
                        }
                        multiPolygonWKT.append(coord.lng).append(" ").append(coord.lat);
                    }
                    multiPolygonWKT.append(")");
                }
                multiPolygonWKT.append(")");
            }
            multiPolygonWKT.append(")");
            result = multiPolygonWKT.toString();
        }
        return result;
    }

    /** Gets a multipolygon WKT given an h3 set. 
     *  @param h3 h3 set.
     *  @param geoJson whether to return in geoJSon format
     *  @return WKT Polygon
     */
    public String cells_to_multi_polygon(List<Long> h3, Boolean geoJson) {
        return geoJson == null ? null : cellsToMultiPolygon(h3, null, geoJson);
    }

    /** Gets a multipolygon WKT given an h3 set. 
     *  @param h3 h3 set.
     *  @param geoJson whether to return in geoJSon format.
     *  @return WKT Polygon
     */
    public String cell_addresses_to_multi_polygon(List<String> h3Addresses, Boolean geoJson) {
        return geoJson == null ? null : cellsToMultiPolygon(null, h3Addresses, geoJson);
    }

    /** Returns whether or not the provided H3Indexes are neighbors.
     *  @param origin the first h3 index
     *  @param destination the second h3 index
     *  @return true when the two h3 indexes are neighbors.
     */
    public Boolean are_neighbor_cells(Long origin, Long destination){
        return (origin == null || destination == null) ? null : 
            h3Core.areNeighborCells(origin, destination);
    }

    /** Returns whether or not the provided H3 addresses are neighbors.
     *  @param origin the first h3 address
     *  @param destination the second h3 address
     *  @return true when the two h3 adresses are neighbors.
     */
    public Boolean are_neighbor_cells(String origin, String destination){
        return (origin == null || destination == null) ? null : 
            h3Core.areNeighborCells(origin, destination);
    }

    /** Returns a unidirectional edge H3 index based on the provided origin and destination.
     *  @param origin the origin index.
     *  @param destination the destination index
     *  @return the id of edge from origin to destination.
     */
    public Long cells_to_directed_edge(Long origin, Long destination) {
        return origin == null || destination == null ? null : 
            h3Core.cellsToDirectedEdge(origin, destination);
    }

    /** Returns a unidirectional edge H3 index based on the provided origin and destination.
     *  @param origin the origin h3 address
     *  @param destination the destination h3 address.
     *  @return the id of edge from origin to destination.
     */
    public String cells_to_directed_edge(String origin, String destination) {
        return origin == null || destination == null ? null :
            h3Core.cellsToDirectedEdge(origin, destination);
      }

    /** Determines if the provided H3Index is a valid unidirectional edge index.
     * @param edge the edge id
     * @return true when edge is not null and the edge is valid.
     */
    public Boolean is_valid_directed_edge(Long edge){
        return edge != null && h3Core.isValidDirectedEdge(edge);
    }

    /** Determines if the provided H3 edge address is a valid unidirectional edge index.
     * @param edgeAddress the edge address
     * @return true when edge address is valid. 
     *
     */
    public Boolean is_valid_directed_edge(String edgeAddress){
        return edgeAddress != null && h3Core.isValidDirectedEdge(edgeAddress);      
    }

    /** Returns the origin hexagon from the unidirectional edge H3Index.
    * @param edge the edge ID
    * @return the h3 index of the origin of the edge.
    */
    public Long get_directed_edge_origin(Long edge){
        return (edge == null)  ? null : h3Core.getDirectedEdgeOrigin(edge); 
    }

    /** Returns the origin hexagon from the unidirectional edge H3Index. 
     *  @param edgeAddress the edge address.
     *  @return the h3 address of  the origin of the edge
     */
    public String get_directed_edge_origin(String edgeAddress){
        return edgeAddress == null ? null : h3Core.getDirectedEdgeOrigin(edgeAddress);
    }

    /** Returns the destination hexagon from the unidirectional edge H3Index. */
    public Long get_directed_edge_destination(Long edge){
        return edge == null ? null : h3Core.getDirectedEdgeDestination(edge);
    }

    /** Returns the destination hexagon from the unidirectional edge address. */
    public String get_directed_edge_destination(String edgeAddress){
        return edgeAddress == null ? null : 
            h3Core.getDirectedEdgeDestination(edgeAddress);
    }

    /** Returns origin and destination hexagons from a unidrectional edge. 
     * @param edge the unidirectional edge
     * @return list of two elements , the first one is the origin, and the second one is the destination.
     */
    public List<Long> get_directed_edge_origin_destination(Long edge) {
        return (edge == null) ? null :
            List.of(get_directed_edge_origin(edge), get_directed_edge_destination(edge));
    }

    /** Returns origin and destination hexagons from a unidrectional edge. 
     * @param edge the unidirectional edge
     * @return array of two elements , the first one is the origin, and the second one is the destination.
     */
    public List<String> get_directed_edge_origin_destination(String edge) {
        return (edge == null) ? null :
            List.of(get_directed_edge_origin(edge), get_directed_edge_destination(edge));
    }

    /** Provides all of the unidirectional edges from the current H3Index. 
     *  @param h3 the h3 index.
     *  @param all the edges from h3.
     *  @return list of all unidirectional edges.
     */
    public List<Long> origin_to_directed_edges(Long h3){
        return h3 == null ? null : h3Core.originToDirectedEdges(h3);
    }

    /** Provides all of the unidirectional edges from the current H3 address. 
     *  @param h3 the h3 address 
     *  @return all edges from the cell
     */
    public List<String> origin_to_directed_edges(String h3){
        return h3 == null ? null : h3Core.originToDirectedEdges(h3);
    }

    /** Get the vertices of a given edge as a list of WKT oints
     *  @param edge an edge
     *  @return all points in WKT Points format.
     */
    public List<String> directed_edge_to_boundary(Long edge){
        return edge == null ? null : 
            h3Core.directedEdgeToBoundary(edge).stream()
                .map(H3AthenaHandler::wktPoint)
                .collect(Collectors.toList());
    }

    /** Produces local IJ coordinates for an H3 index anchored by an origin.
     *  The output is stored in a list of two elements, the first one is i, the second is j.
     *  @param origin the origin
     *  @param h3 the cell.
     */
    public List<Integer> cell_to_local_ij(Long origin, Long h3) {
        if (origin == null || h3 == null) { return null; }

        final CoordIJ coord = h3Core.cellToLocalIj(origin, h3);
        return new ArrayList<>(Arrays.asList(coord.i, coord.j));
    }

    /** Produces local IJ coordinates for an H3 index anchored by an origin.
     *  The output is stored in a list of two elements, the first one is i, the second is j.
     *  @param origin the origin
     *  @param h3 the cell.
     */
    public List<Integer> cell_to_local_ij(String origin, String h3) {
        if (origin == null || h3 == null) { return null; }

        final CoordIJ coord = h3Core.cellToLocalIj(origin, h3);
        return new ArrayList<>(Arrays.asList(coord.i, coord.j));
    }

    /** Provides all of the unidirectional edges from the current H3Index. 
     *  It returns the WKT Points.
     *  @param edgeAddress the edge as a String
     *  @return the boundary in WKT Point format.
     *  @return the area
     */
    public List<String> directed_edge_to_boundary(String edgeAddress){
        return edgeAddress == null ? null : h3Core.directedEdgeToBoundary(edgeAddress).stream()
                                                    .map(H3AthenaHandler::wktPoint)
                                                    .collect(Collectors.toList());
    }

    /** Average hexagon area in unit of area at the given resolution. 
     *  @param res resolution 
     *  @param unit the unit of area, km2 or m2.
     *  @return the area. 
     */
    public Double cell_area(Integer res, String unit) {
        return res == null || unit == null ? null : 
            h3Core.cellArea(res, AreaUnit.valueOf(unit));
    }

    /** Average hexagon edge length  at a given resolution.
     *  @param res the resolution
     *  @param unit the unit, km or m, rads.
     *  @return the length of the edge. 
     *  
     */
    public Double get_hexagon_edge_length_avg(Integer res, String unit){
        return res == null || unit == null ? null : 
            h3Core.getHexagonEdgeLengthAvg(res, LengthUnit.valueOf(unit));
    }

    /** Returns the total count of hexagons in the world at a given resolution. 
     * @param res the resolution.
     * @return the number of hexagons at a given resolution.
     */
    public Long get_num_cells(Integer res){
        return res == null ? null : h3Core.getNumCells(res);
    }

    /** Returns all the resolution 0 h3 indexes.
     *  @param dummy a dummy parameter, ignored.
     *  @return the indexes.
     */
    public List<Long> get_res0_cells(Integer dummy){
        return new ArrayList<Long>(h3Core.getRes0Cells());
    }

    /** Returns all the resolution 0 h3 indexes.
     *  @param dummy a dummy parameter, ignored.
     *  @return the indexes.
     */
    public List<String> get_res0_cells(String dummy){
        return new ArrayList<String>(h3Core.getRes0CellAddresses());
    }

    /** Gets the pentagon indexes at a given resolution. 
     * @param res resolution.
     * @return the indexes of pentagons in H3 system. 
     */
    public List<Long> get_pentagons(Integer res){
        return res == null ? null : new ArrayList<Long>(h3Core.getPentagons(res));
    }

    /** Gets all pentagon addresses at a given resolution. 
     * @param res resolution.
     * @return the addresses of pentagons in H3 system. 
     */
    public List<String> get_pentagon_addresses(Integer res){
        return res == null ? null : new ArrayList<String>(h3Core.getPentagonAddresses(res));
    }

    private static String pointsListStr(LatLng geoCoord, String sep) {
        return String.format("%f%s%f", geoCoord.lat, sep, geoCoord.lng);
    }

    private static String wktPoint(LatLng coord) {
        return String.format("POINT (%f %f)", coord.lng, coord.lat);
    }
}
