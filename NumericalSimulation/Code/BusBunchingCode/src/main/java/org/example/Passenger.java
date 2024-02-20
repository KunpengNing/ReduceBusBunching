package org.example;
import java.util.Random;

class Passenger{
    // Random instance
    public static final Random RANDOM = new Random();
    public static double LOSS_RATE = Simulator.LOSS_RATE_MU;
    // Passenger initial attributes
    private int aboard_station;
    private int alight_station;
    private int start_waiting_time;
    private int endof_waiting_time = -1;
    private int bus_no = -1;
    private boolean status = false; /* True: passenger is still waiting; False: passenger has left*/

    Passenger(int board_at, int arrive_time){
        this.aboard_station = board_at;
        this.alight_station = alightStation(board_at);
        this.start_waiting_time = arrive_time;
    }

    /**
     * Get private variables
     * */
    public int getAboardStation(){
        return aboard_station;
    }
    public int getAlightStation(){
        return alight_station;
    }
    public int getStartWaitingTime(){
        return start_waiting_time;
    }
    public int getEndofWaitingTime(){
        return endof_waiting_time;
    }
    public boolean getPassengerStatus(){
        return status;
    }
    public int getBusNumber(){
        return bus_no;
    }

    public static double lossPossibility(int waiting_time){
        double lossPossibility = 1 - Math.exp(-1 * LOSS_RATE * waiting_time);
        return lossPossibility;
    }
    private static int alightStation(int board_station){
        /**
         * 90% passengers get off in the next 10 stations (ride within 6km), and only
         * 10% passengers ride further than 10 stations
         */
        int total_station_number = Simulator.STATION_AMOUNT;
        double rand_uniform = RANDOM.nextDouble();
        int alight_station;
        if( total_station_number - board_station >=6 && rand_uniform <= 0.9 ){
            alight_station = RANDOM.nextInt(board_station+1, board_station+7);
        } else {
            alight_station = RANDOM.nextInt( board_station+1, total_station_number);
        }
        return alight_station;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
    public void setBusNumber(int bus_no){
        this.bus_no = bus_no;
    }
    public void setEndofWaitingTime(int board_time){
        this.endof_waiting_time = board_time;
    }
}
