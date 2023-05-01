package com.isep.dataengineservice.Services;

import com.isep.dataengineservice.Models.GeoPosition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service

public class GeoNodeService {
    private static final int earthRadius = 6371;
    public Set<GeoPosition> BfsSearchGeoNodes(GeoPosition geoNode, Set<GeoPosition> allGeoNodes) {
        //starts with geoNode and returns a list of 100 geoPositions in 70km radius.

        Queue<GeoPosition> queue = new LinkedList<>();
        geoNode.setDistanceFromStart(0);
        queue.add(geoNode);

        int maxDistance = 70000; // 70km maximum distance between initial point and the found geoNode

        while (!queue.isEmpty() && allGeoNodes.size() < 100) {
            GeoPosition currentNode = queue.poll();
            var threeDirections = getNeighboringNodesInAllDirections(currentNode);

            threeDirections.forEach(position -> {
                double distanceFromStart = calculateDistanceBetweenGeoNodes(geoNode.getLat(), geoNode.getLon(), position.getLat(), position.getLon());
                position.setDistanceFromStart(distanceFromStart);

                boolean isFarEnough = allGeoNodes.stream().noneMatch(existingPlace ->
                        calculateDistanceBetweenGeoNodes(existingPlace.getLat(), existingPlace.getLon(), position.getLat(), position.getLon()) <= 1000
                );

                if (isFarEnough && distanceFromStart <= maxDistance && !allGeoNodes.contains(position)) {
                    allGeoNodes.add(position);
                    queue.add(position);
                }
            });
        }
        allGeoNodes.forEach(e -> System.out.println("(" + e.getLat() + ", " + e.getLon() + "),"));
        return allGeoNodes;
    }

    public List<GeoPosition> getNeighboringNodesInAllDirections(GeoPosition currentGeoNode) {
        List<GeoPosition> neighbors = new ArrayList<>();
        GeoPosition east = getNextGeoNode(currentGeoNode.getLat(), currentGeoNode.getLon(), 0);
        GeoPosition north = getNextGeoNode(currentGeoNode.getLat(), currentGeoNode.getLon(), Math.PI / 2);
        GeoPosition west = getNextGeoNode(currentGeoNode.getLat(), currentGeoNode.getLon(), Math.PI);

        neighbors.add(east);
        neighbors.add(west);
        neighbors.add(north);

        return neighbors;
    }


    public static GeoPosition getNextGeoNode(double lat, double lon, double theta) {
        //uses the haversine formula to get geo positions that are geoDistance kilometers away from current geoPosition and that are theta direction away.
        //used to get nodes in North/West/East of the current node
        //the math is generated by ChatGPT, analyzed and cleaned by me.

        //we calculate the center of the next circle
        double geoDistance = 2;

        //we convert our lat and lon  to radian
        double startLatitudeRadians = Math.toRadians(lat);
        double startLongitudeRadians = Math.toRadians(lon);


        //use Haversine to calculate new lat & new lon
        double latitude = Math.asin(
                Math.sin(startLatitudeRadians) * Math.cos(geoDistance / earthRadius) +
                        Math.cos(startLatitudeRadians) * Math.sin(geoDistance / earthRadius) * Math.cos(theta)
        );
        double longitude = startLongitudeRadians + Math.atan2(
                Math.sin(theta) * Math.sin(geoDistance / earthRadius) * Math.cos(startLatitudeRadians),
                Math.cos(geoDistance / earthRadius) - Math.sin(startLatitudeRadians) * Math.sin(latitude)
        );

        //convert back to degrees
        double newLat = Math.toDegrees(latitude);
        double newLon = (Math.toDegrees(longitude) + 540) % 360 - 180;

        return GeoPosition.builder().lat(newLat).lon(newLon).build();
    }


    public double calculateDistanceBetweenGeoNodes(double lat1, double lon1, double lat2, double lon2) {
        //the haversine formula to calculate the distance between geoNodes
        //used it to verify that we're not returning nodes that are too far or too close
        //the math is generated by ChatGPT, analyzed and cleaned by me.

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c * 1000; // convert to meters
        return distance;
    }


    public GeoPosition getGeoPosition(String city){
        RestTemplate restTemplate = new RestTemplate();
        String uri = "https://opentripmap-places-v1.p.rapidapi.com/en/places/geoname?name="+city;
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RapidAPI-Key", "6a4f81847bmsh8785c9220ccebdfp1b97bfjsn74f82815c241");
        headers.add("X-RapidAPI-Host", "opentripmap-places-v1.p.rapidapi.com");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<GeoPosition> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, GeoPosition.class);
        GeoPosition geoPosition = response.getBody();
        return geoPosition;
    }

}