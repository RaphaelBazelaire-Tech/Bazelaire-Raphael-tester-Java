package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        double durationInMinutes = durationInMillis / 60_000.0;
        double durationInHours = durationInMillis / 3_600_000.0;

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {

                if (durationInMinutes <= 30) {
                    ticket.setPrice(0.0);
                } else {
                    ticket.setPrice(durationInHours * Fare.CAR_RATE_PER_HOUR);
                }
                break;
            }
            case BIKE: {

                if (durationInMinutes <= 30) {
                    ticket.setPrice(0.0);
                } else {
                    ticket.setPrice(durationInHours * Fare.BIKE_RATE_PER_HOUR);
                }
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}