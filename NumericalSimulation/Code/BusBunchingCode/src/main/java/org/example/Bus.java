package org.example;
import java.util.ArrayList;
import java.util.Random;

class Bus {
    // Random instance
    public static final Random random = new Random();
    // Bus initial attribute
    public int capacity = 100;
    private int number = -1;
    private int departure_time_from_terminal = 0;
    private int departure_time_from_last_station = 0;
    // Bus activeness
    private boolean activeness = false;
    private boolean atStation = true;
    // Variable parameters
    private double position = 0.0;
    private int next_station = 0;
    private double speed = 0.0;
    /**
     * dwell time is caused by boarding or alighting.
     */
    public int dwell_time_left = 0;
    /**
     * intervention time is caused by control.
     */
    public int holding_time_left = 0;
    public ArrayList<Passenger> passenger_on_board = new ArrayList<Passenger>(capacity);
    public static final double Speed_max = 15;
    public static final double Speed_min = 6.6;

    /**
     * Initializer of class Bus
     * This initializer is only invoked when a Bus depart from the terminal.
     *
     * @param sequence the sequence of departure, i.e. the bus's number.
     * @param departure_time time when the bus depart from the terminal.
     */
    Bus(int sequence, int departure_time){
        this.number = sequence;
        this.departure_time_from_terminal = departure_time;
        this.departure_time_from_last_station = departure_time;
    }

    // Get private variables
    public boolean isActive(){
        return activeness;
    }
    public boolean isAtStation(){
        return atStation;
    }
    public int getNumber(){
        return number;
    }
    public int getNextStation(){
        return next_station;
    }
    public double getSpeed(){
        return speed;
    }
    public int getDwellTimeLeft(){
        return dwell_time_left;
    }
    public int getVacancy(){
        return capacity - passenger_on_board.size();
    }
    public double getPosition() {
        return position;
    }
    public int getLastDepartureTime(){
        return departure_time_from_last_station;
    }

    // Set variables
    public void setActiveness(boolean activeness){
        this.activeness = activeness;
    }
    public void setAtStation(boolean atStation) { this.atStation = atStation; }
    public void setInactive(){
        this.activeness = false;
    }
    public void setSpeed(double speed){
        this.speed = speed;
    }
    public void setPosition(double position){
        this.position = position;
    }
    public void setDwellTime(int dwellTime){
        this.dwell_time_left = dwellTime;
    }
    public void setNextStation(int next_station){
        this.next_station = next_station;
    }
    public void setLastDepartureTime(int departure_time){
        this.departure_time_from_last_station = departure_time;
    }

    public boolean isAboutToArriveNextStation(double next_station_position){
        if (this.position <= next_station_position &&
                this.position + this.speed > next_station_position){
            return true;
        }
        else{
            return false;
        }
    }
    public void boardAPassenger(Passenger p){
        passenger_on_board.add(p);
    }

    /**
     * Alight passengers who are going to alight at the station.
     * @param station The station the bus just arrive. (it should be equal to "next_station")
     * @return number of passengers get off.
     */
    public int alightPassengers(int station) throws Exception {
        if(station != next_station) {
            System.out.println("Wrong station number");
            throw new Exception();
        }
        int alight_passenger_number = 0;
        ArrayList<Passenger> passenger_remove_list = new ArrayList<Passenger>();
        for(Passenger p : passenger_on_board){
            if( p.getAlightStation()== station ){
                passenger_remove_list.add(p);
                alight_passenger_number ++ ;
            }
        }
        for(Passenger q : passenger_remove_list){
            passenger_on_board.remove(q);
        }
        return alight_passenger_number;
    }
}
