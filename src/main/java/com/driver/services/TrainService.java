package com.driver.services;

import com.driver.EntryDto.AddTrainEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainService {

    @Autowired
    TrainRepository trainRepository;

    public Integer addTrain(AddTrainEntryDto trainEntryDto){

        //Add the train to the trainRepository
        //and route String logic to be taken from the Problem statement.
        //Save the train and return the trainId that is generated from the database.
        //Avoid using the lombok library
        List<Station> stations = trainEntryDto.getStationRoute();
        String route = "";
        for(Station s : stations){
            route+=s+",";
        }
        route = route.substring(0,route.length()-1);
        Train train = new Train();
        train.setRoute(route);
        train.setNoOfSeats(trainEntryDto.getNoOfSeats());
        train.setDepartureTime(trainEntryDto.getDepartureTime());
        Train savedTrain = trainRepository.save(train);
        return savedTrain.getTrainId();
    }

    public Integer calculateAvailableSeats(SeatAvailabilityEntryDto seatAvailabilityEntryDto){

        int trainId = seatAvailabilityEntryDto.getTrainId();
        Station fromStation = seatAvailabilityEntryDto.getFromStation();
        Station toStation = seatAvailabilityEntryDto.getToStation();
        Optional<Train> optionalTrain = trainRepository.findById(trainId);
        if (!optionalTrain.isPresent()) {
            throw new RuntimeException("Train doesn't exist");
        }
        Train train = optionalTrain.get();

        // Calculate the total seats available between the given two stations
        // Suppose the route is A -> B -> C -> D, and there are 2 seats available in total in the train
        // and 2 tickets are booked from A to C and B to D.
        // The seat is available only between A to C and A to B.
        // If a seat is empty between 2 stations, it will be counted towards our final answer,
        // even if that seat is booked post the destStation or before the boardingStation.

        // Find the index of fromStation and toStation in the route
        String[] stations = train.getRoute().split(",");
        int fromIndex = -1;
        int toIndex = -1;
        for (int i = 0; i < stations.length; i++) {
            if (stations[i].equals(fromStation.toString())) {
                fromIndex = i;
            }
            if (stations[i].equals(toStation.toString())) {
                toIndex = i;
            }
        }

        if (fromIndex == -1 || toIndex == -1) {
            throw new RuntimeException("Invalid fromStation or toStation");
        }

        // Calculate the number of booked seats between fromStation and toStation
        int bookedSeats = 0;
        for (Ticket ticket : train.getBookedTickets()) {
            List<Passenger> passengers = ticket.getPassengersList();
            int ticketFromIndex = -1;
            int ticketToIndex = -1;

            // Find the index of ticket's fromStation and toStation in the route
            for (int i = 0; i < stations.length; i++) {
                if (stations[i].equals(ticket.getFromStation().toString())) {
                    ticketFromIndex = i;
                }
                if (stations[i].equals(ticket.getToStation().toString())) {
                    ticketToIndex = i;
                }
            }

            // If the ticket is between fromStation and toStation, add the booked seats to total bookedSeats
            if (ticketFromIndex >= fromIndex && ticketToIndex <= toIndex) {
                bookedSeats += passengers.size();
            }
        }

        // Calculate the total available seats between fromStation and toStation
        int totalSeats = train.getNoOfSeats();
        int availableSeats = totalSeats - bookedSeats;

        return availableSeats;
    }

    public Integer calculatePeopleBoardingAtAStation(Integer trainId,Station station) throws Exception{

        //We need to find out the number of people who will be boarding a train from a particular station
        //if the trainId is not passing through that station
        //throw new Exception("Train is not passing from this station");
        //  in a happy case we need to find out the number of such people.
        Optional<Train> optionalTrain = trainRepository.findById(trainId);
        if(!optionalTrain.isPresent()){
            throw new RuntimeException("Train doesn't exists");
        }
        Train train = optionalTrain.get();
        String route = train.getRoute();
        if (!route.contains(station.toString())) {
            throw new Exception("Train is not passing through this station");
        }
        int passenger = 0;
        for(Ticket t : train.getBookedTickets()){
            if(t.getFromStation()==station) passenger+=t.getPassengersList().size();
        }
        return passenger;
    }

    public Integer calculateOldestPersonTravelling(Integer trainId){

        //Throughout the journey of the train between any 2 stations
        //We need to find out the age of the oldest person that is travelling the train
        //If there are no people travelling in that train you can return 0
        Optional<Train> optionalTrain = trainRepository.findById(trainId);
        if(!optionalTrain.isPresent()){
            throw new RuntimeException("Train doesn't exists");
        }
        Train train = optionalTrain.get();
        int old = Integer.MAX_VALUE;
        for(Ticket t : train.getBookedTickets()){
            for(Passenger p : t.getPassengersList()){
                old = Math.max(old, p.getAge());
            }
        }
        return old==Integer.MAX_VALUE ? 0 : old;
    }

    public List<Integer> trainsBetweenAGivenTime(Station station, LocalTime startTime, LocalTime endTime){

        //When you are at a particular station you need to find out the number of trains that will pass through a given station
        //between a particular time frame both start time and end time included.
        //You can assume that the date change doesn't need to be done ie the travel will certainly happen with the same date (More details
        //in problem statement)
        //You can also assume the seconds and milli seconds value will be 0 in a LocalTime format.
        List<Train> trains = trainRepository.findAll();
        List<Integer> trainIds  = new ArrayList<>();
        for(Train train : trains){
            String route = train.getRoute();
            String[] stations = route.split(",");
            int getStationNumber = -1;
            for(int i=0;i<stations.length;i++){
                if(stations[i].equals(stations.toString())){
                    getStationNumber=i;
                    break;
                }
            }
            if(getStationNumber!=-1){
                LocalTime getTimeToReachStation = train.getDepartureTime().plusHours(1);
                if((startTime.isBefore(getTimeToReachStation) && endTime.isAfter(getTimeToReachStation)) ||
                        getTimeToReachStation.equals(startTime) ||
                        getTimeToReachStation.equals(endTime)
                )   trainIds.add(train.getTrainId());
            }
        }
        return trainIds;
    }

}
