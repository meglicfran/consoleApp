package com.example.commandLineApp;

import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.algorithm.distance.PointPairDistance;
import org.locationtech.jts.io.geojson.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import java.util.ArrayList;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public class Validator {

    public static boolean validate(String geomDataJson, String ruteJson) throws ParseException, FactoryException, TransformException {
        GeometryFactory geometryFactory = new GeometryFactory();
        GeoJsonReader reader = new GeoJsonReader(geometryFactory);

        Geometry geomData = reader.read(geomDataJson);
        Geometry kopno = geomData.getGeometryN(0);
        Geometry more = geomData.getGeometryN(1);

        Geometry ruteData = reader.read(ruteJson);
        LineString rute = (LineString) ruteData.getGeometryN(0);

        Geometry kopno1 = null;
        Geometry kopno2 = null;
        ArrayList<Coordinate> coordinateArrayList = new ArrayList<>();
        Coordinate[] ruteCoordinates = rute.getCoordinates();
        for(int i = 0; i < ruteCoordinates.length; i++){
            Point currPoint = geometryFactory.createPoint(ruteCoordinates[i]);
            if(i==0){
                boolean kopnoIliOtok = false;
                for(int j = 0; j < geomData.getNumGeometries(); j++){
                    if(j==1) continue;
                    Geometry currGeometry = geomData.getGeometryN(j);
                    if(currGeometry.contains(currPoint)) {
                        kopnoIliOtok = true;
                        kopno1=currGeometry;
                        break;
                    }
                }
                if(!kopnoIliOtok){
                    System.out.println("Pocetna i zavrsna tocka putovanja mora biti na kopnu ili otoku");
                    return false;
                }
                continue;
            }
            if(i==ruteCoordinates.length-1){
                boolean kopnoIliOtok = false;
                for(int j = 0; j < geomData.getNumGeometries(); j++){
                    if(j==1) continue;
                    Geometry currGeometry = geomData.getGeometryN(j);
                    if(currGeometry.contains(currPoint)) {
                        kopnoIliOtok = true;
                        kopno2=currGeometry;
                        break;
                    }
                }
                if(!kopnoIliOtok){
                    System.out.println("Pocetna i zavrsna tocka putovanja mora biti na kopnu ili otoku");
                    return false;
                }
                continue;
            }
            if(!more.contains(currPoint) && !kopno.contains(currPoint)){
                System.out.println("Sve tocke rute trebaju biti unutar granica mora, kopna i otoka");
                return false;
            }
            coordinateArrayList.add(currPoint.getCoordinate());
        }

        Coordinate[] modifiedCoords = new Coordinate[coordinateArrayList.size()];
        modifiedCoords = coordinateArrayList.toArray(modifiedCoords);
        LineString modifiedRute = geometryFactory.createLineString(modifiedCoords);

        for(int i = 0; i < geomData.getNumGeometries(); i++){
            if(i==1) continue;

            Geometry currGeometry = geomData.getGeometryN(i);
            Geometry intersection = modifiedRute.intersection(currGeometry);

            if(!intersection.isEmpty()){
                System.out.println("Niti jedna linija izmedu dvije tocke (osim pocetne i zavrsne) putanje ne smije prelaziti preko kopna ili otoka");
                return false;
            }
        }

        Point granicaKopna1 = granicaKopna(ruteCoordinates[0], ruteCoordinates[1], kopno1);
        Point granicaKopna2 = granicaKopna(ruteCoordinates[ruteCoordinates.length-1], ruteCoordinates[ruteCoordinates.length-2], kopno2);
        double zracna = zracnaUdaljenost(granicaKopna1,granicaKopna2);
        ruteCoordinates[0] = granicaKopna1.getCoordinate();
        ruteCoordinates[ruteCoordinates.length-1] = granicaKopna2.getCoordinate();
        double udaljenostRute = ukupnaDuzina(ruteCoordinates);
        double najdaljaUdaljenost = najdaljaTocka(rute, geomData);

        System.out.println("Ukupna duzina planirane rute samo na moru: "+udaljenostRute+" m");
        System.out.println("Zracna udaljenost izmedu pocetne i zavrsne tocke na granici mora s otokom ili kopnom: "+ zracna + " m");
        System.out.println("Udaljenost tocke rute koja je najudaljenija od kopna: "+ najdaljaUdaljenost + " m");
        return false;
    }

    private static double najdaljaTocka(LineString rute, Geometry geomData) throws FactoryException, TransformException {
        GeoJsonWriter writer = new GeoJsonWriter();
        GeometryFactory geometryFactory = new GeometryFactory();
        DistanceToPoint dtp = new DistanceToPoint();


        Coordinate[] coordinates = rute.getCoordinates();
        double maxDist = 0.0;
        for(Coordinate coordinate : coordinates) {
            Point currPoint = geometryFactory.createPoint(coordinate);
            double minDist = Double.MAX_VALUE;
            for(int i=0; i<geomData.getNumGeometries(); i++){
                if(i==1) continue;
                Geometry currGeom = geomData.getGeometryN(i);
                PointPairDistance pointDistance = new PointPairDistance();
                dtp.computeDistance(currGeom, currPoint.getCoordinate(), pointDistance);
                Point point1 = geometryFactory.createPoint(pointDistance.getCoordinate(0));
                Point point2 = geometryFactory.createPoint(pointDistance.getCoordinate(1));
                double currDist = zracnaUdaljenost(point1, point2);
                if(currDist<minDist) minDist=currDist;
            }
            if(minDist>maxDist) maxDist=minDist;
        }
        return maxDist;
    }

    private static double ukupnaDuzina(Coordinate[] ruteCoordinates) throws FactoryException, TransformException {
        GeometryFactory geometryFactory = new GeometryFactory();

        double distance = 0.0;

        Point first = geometryFactory.createPoint(ruteCoordinates[0]);
        for(int i=1; i<ruteCoordinates.length; i++){
            Point next = geometryFactory.createPoint(ruteCoordinates[i]);
            distance+=zracnaUdaljenost(first,next);
            first = geometryFactory.createPoint(ruteCoordinates[i]);
        }

        return distance;
    }

    private static double zracnaUdaljenost(Point granicaKopna1, Point granicaKopna2) throws FactoryException, TransformException {
        double dist4 = haversineDistance(granicaKopna1.getCoordinate().y, granicaKopna1.getCoordinate().x, granicaKopna2.getCoordinate().y, granicaKopna2.getCoordinate().x);
        return dist4*1000;
    }

    private static Point granicaKopna(Coordinate coord1, Coordinate coord2,Geometry kopno){
        GeometryFactory geometryFactory = new GeometryFactory();

        Coordinate[] ruteSegmentCoord = new Coordinate[] {coord1, coord2};
        LineString ruteSegment = geometryFactory.createLineString(ruteSegmentCoord);

        LineString intersection = (LineString) kopno.intersection(ruteSegment);

        Point start = intersection.getStartPoint();
        Point end = intersection.getEndPoint();
        Point rute1 = geometryFactory.createPoint(coord1);

        if(rute1.equalsExact(start)){
            return end;
        }else{
            return start;
        }
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double EARTH_RADIUS_KM = 6371.0;
        // Convert latitude and longitude from degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Calculate the differences
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Apply the Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate the distance
        return EARTH_RADIUS_KM * c;
    }
}
