package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.example.Functions.*;

public class Simulator {

    /**
     * Control Panel.
     */
    public static int DEPARTURE_INTERVAL;
    public static int CONSIDER_RANGE_U;
    public static double ARRIVAL_RATE_LAMBDA;
    public static int I_MAX;
    public static int I_MIN;
    public boolean record_position_history = false;
    public boolean use_real_world_data = false;
    /**
     * Random instance
     */
    public static final Random RANDOM = new Random();
    /**
     * Constants
     */
    public static int BUS_AMOUNT = 200;
    public static int STATION_AMOUNT = 51;
    public static final double STATION_DISTANCE = 1000.0;
    public static final double AVG_SPEED = 10;
    public static final double STD_SPEED = 2;
    public static final double Speed_max = 15;
    public static final double Speed_min = 6.6;
    public static final int BUS_CAPACITY = 90;
    public static final double LOSS_RATE_MU = 0.001;
    public static final int MINUS_INF = -65536;
    public static final double RELATIVE_R_HAT = 0.2;
    public static final double GAMMA_R = 0.5;
    public static final double RELATIVE_T_HAT = 0.2;
    public static final double GAMMA_T = 0.5;
    public static final int START_TIME = 0;
    public static final int END_TIME = 150000;
    public static final int BOARDING_TIME_PER_PASSENGER = 3;
    public static final int ALIGHTING_TIME_PER_PASSENGER = 1;
    public static final int STEP_LENGTH = 1;
    /* Average endurance: 20min, 1200seconds */
    /**
     * Parameters in numeration.
     */

    /** Map & History */
    // last departure -> to count arrival interval
    public final static int OCCUPIED = 1;
    public final static int FREE = 0;
    private int[] station_status = new int[STATION_AMOUNT]; //A station is Occupied or Free
    private int[] station_last_departure_time = new int[STATION_AMOUNT];
    private int[] bus_max_onboard_passenger = new int[BUS_AMOUNT];

    //USE waiting_passengers.get(station).add/remove(passenger)
    private ArrayList<ArrayList<Passenger>> waiting_passengers = new ArrayList<ArrayList<Passenger>>();
    public ArrayList<Passenger> all_passenger_list= new ArrayList<Passenger>();
    public ArrayList<int[]> position_history = new ArrayList<int[]>();
    /** Containers */
    /**
     * Container of all buses.
     */
    ArrayList<Bus> busArray = new ArrayList<Bus>();
    // Bus & Station Data
    public double[] speed_mean = new double[STATION_AMOUNT];
    public double[] speed_std = new double[STATION_AMOUNT];
    public double[] speed_mu = new double[STATION_AMOUNT];
    public double[] speed_sigma = new double[STATION_AMOUNT];
    /**
     * USE arrival_rate.get(time/1800)[station].
     */
    public ArrayList<double[]> arrival_rate = new ArrayList<double[]>();
    public ArrayList<Integer> departure_time_list = new ArrayList<Integer>();
    public double[] station_position = new double[STATION_AMOUNT];

    /**
     * H[i][j] Time difference between the j-th and the (j-1)-th buses' arrivals at station i.
     * esp. H[·][0] = average depart interval
     */
    public int[][] bus_headways_H = new int[STATION_AMOUNT][BUS_AMOUNT];
    /**
     * R[i][j] is the running time of the j-th bus from station i-1 to station i.
     */
    public int[][] bus_running_time_R = new int[STATION_AMOUNT][BUS_AMOUNT];
    /**
     * T[i][j] is the boarding time of the j-th bus at station i;
     */
    public int[][] bus_boarding_time_T = new int[STATION_AMOUNT][BUS_AMOUNT];
    /**
     * I[i][j] is the intervention of the j-th bus from station i-1 to station i;
     * Note. We are deciding I[i+1][j] at station i;
     */
    public int[][] bus_intervene_I = new int[STATION_AMOUNT][BUS_AMOUNT];

    // Definition
    public static final int NO_CONTROL = 0;
    public static final int AT_STATION_CONTROL = 1;
    public static final int INTER_STATION_CONTROL = 2;
    public static final int JOINT_CONTROL = 3;
    /** Method of Bartholdi and Eisenstein (2013) */
    public static final int BASE_MINIMUM_HEADWAY = 4;
    /** Method of Eberlein, Wilson and Bernstein (2001) */
    public static final int BASE_QUADRATIC_PROBLEM = 5;
    /** Method of He et al. (2020) */
    public static final int BASE_HE = 6;

    /**
     * This is the constructor of class Simulator.
     * @param real_world_data: boolean, true if we input the real world data for simulation.
     * @param position_history: boolean, true if we want to record positions of buses every second.(when we don't need to record real time position, making it false can acclerate)
     * @param departure_interval: int, the time interval between consecutive buses at the first station, denoted as $h_0$ in the paper.
     * @param consider_range_u: int, the number of considered buses, denoted as $u$ in the paper.
     * @param arrival_rate_lambda: double, if we don't use real world data, input a double; otherwise, input real world arrival rate collected from No.368 bus in Beijing.
     * @param I_max: int, the upper boundary of intervention.
     * @param I_min: int, the lower boundary of intervention.
     */

    public Simulator(boolean real_world_data, boolean position_history,
            int departure_interval, int consider_range_u, double arrival_rate_lambda, int I_max, int I_min) throws Exception {
        DEPARTURE_INTERVAL = departure_interval;
        CONSIDER_RANGE_U = consider_range_u;
        ARRIVAL_RATE_LAMBDA = arrival_rate_lambda;
        I_MAX = I_max;
        I_MIN = I_min;
        use_real_world_data = real_world_data;
        record_position_history = position_history;
        initialize(false);
        int a = 1;
    }

    /**
     * Initialize the data.
     * @param real_world_data: boolean, true if we input the real world data for simulation.
     */

    void initialize(boolean real_world_data) throws Exception {
        station_status = new int[STATION_AMOUNT];
        station_last_departure_time = new int[STATION_AMOUNT];
        bus_max_onboard_passenger = new int[BUS_AMOUNT];
        station_position = new double[STATION_AMOUNT];
        bus_headways_H = new int[STATION_AMOUNT][BUS_AMOUNT];
        bus_running_time_R = new int[STATION_AMOUNT][BUS_AMOUNT];
        bus_boarding_time_T = new int[STATION_AMOUNT][BUS_AMOUNT];
        bus_intervene_I = new int[STATION_AMOUNT][BUS_AMOUNT];
        speed_mean = new double[STATION_AMOUNT];
        speed_std = new double[STATION_AMOUNT];
        speed_mu = new double[STATION_AMOUNT];
        speed_sigma = new double[STATION_AMOUNT];
        // If we don't use real world data:
        if(!use_real_world_data){
            /* Lambda */
            for(int i=0; i<(int)(END_TIME+1)/1800; i++){
                double[] arrival_rate_by_station = new double[STATION_AMOUNT];
                Arrays.fill(arrival_rate_by_station, ARRIVAL_RATE_LAMBDA);
                arrival_rate.add(arrival_rate_by_station);
            }
            /* speed */
            for(int j=0; j<STATION_AMOUNT; j++){
                speed_mean[j] = AVG_SPEED;
                speed_std[j] = STD_SPEED;
            }
            /* station position */
            for(int l=0; l<STATION_AMOUNT; l++){
                station_position[l] = l*STATION_DISTANCE;
            }
        }
        // If we use real world data:
        else {
            if(STATION_AMOUNT!=49){
                System.out.println("In real world situation, STATION_AMOUNT must be 49.");
                throw new Exception();
            }
            // distance from the first station of Bus No. 368.
            station_position = new double[]{
                    0,  957, 1654, 2289, 3131, 3698, 4438, 4791, 5344, 6595,
                    7211, 7574, 8998,11075,11993,12859,13593,13858,14623,15338,
                    16472,17296,18769,20642,21699,22402,23094,24221,24907,26113,
                    27211,27722,29155,29867,31025,32463,33519,34353,36397,38300,
                    39350,41699,42479,43282,44327,45631,47343,47764,48300};
            readCSV(); // read real world data.
        }
        bus_headways_H[0][0] = DEPARTURE_INTERVAL;
        for(int k=0; k<BUS_AMOUNT; k++){
            departure_time_list.add(k*DEPARTURE_INTERVAL);
        }
        for(int g=1; g<BUS_AMOUNT; g++){
            bus_headways_H[0][g] = departure_time_list.get(g) - departure_time_list.get(g-1);
        }
        for(int h=2; h<STATION_AMOUNT; h++){
            Arrays.fill(bus_intervene_I[h], MINUS_INF);
        }
        for(int i : station_status){ i = FREE; }
        for(int j : station_last_departure_time){ j = 0; }
        for(int k=0; k<STATION_AMOUNT; k++){
            double log_EX = Math.log(speed_mean[k]);
            double log_E2X = Math.log(Math.pow(speed_mean[k],2)+ Math.pow(speed_std[k],2));
            speed_mu[k] = (4*log_EX-log_E2X)/2;
            speed_sigma[k] = Math.sqrt(log_E2X-2*log_EX);
        }
        for(int l=0; l<STATION_AMOUNT; l++){
            waiting_passengers.add(new ArrayList<Passenger>());
        }
        /* Initialize Buses */
        for(int m = 0; m < BUS_AMOUNT; m++){
            Bus bus = new Bus( m , departure_time_list.get(m) );
            busArray.add(bus);
            bus.capacity = BUS_CAPACITY;
        }
        busArray.get(BUS_AMOUNT-1).capacity = Integer.MAX_VALUE;
        /* Initialize Passengers */
        Passenger.LOSS_RATE = LOSS_RATE_MU;
    }

    /**
     * Read real world data from disk. <br>
     * Please remember to modify the paths in this function to fit the absolute path of the given files.
     */
    void readCSV() throws Exception {
        String filePathLambda = "D:\\NumericalSimulation\\RealWorldData\\ArrivalRate.csv";
        BufferedReader readerLambda = new BufferedReader(new FileReader(filePathLambda));
        String line = readerLambda.readLine();  // Neglect the column title.
        arrival_rate = new ArrayList<double[]>();
        while ( ( line = readerLambda.readLine() )!= null ){   // The second line is the first line of data.
            String[] tempStr = line.split(",");
            String[] arrivalRateStr = Arrays.copyOfRange( tempStr , 1, tempStr.length);
            double[] arrivalRateByTime = new double[arrivalRateStr.length];
            for (int i=0; i<arrivalRateStr.length; i++) {
                double x = Double.parseDouble(arrivalRateStr[i]);
                arrivalRateByTime[i] = x / BOARDING_TIME_PER_PASSENGER;
            }
            arrival_rate.add(arrivalRateByTime);
        }
        String filePathSpeed = "D:\\NumericalSimulation\\RealWorldData\\Speed.csv";
        BufferedReader readerSpeed = new BufferedReader(new FileReader(filePathSpeed));
        speed_mean = new double[STATION_AMOUNT];
        speed_std = new double[STATION_AMOUNT];
        for (int j = 0; j< STATION_AMOUNT; j++ ){
            line = readerSpeed.readLine();
            String[] tempStr = line.split(",");
            speed_mean[j] = Double.parseDouble(tempStr[1]);
            speed_std[j]  = Double.parseDouble(tempStr[2]);
        }
    }

    /**
     * Entrance of operation
     * @param operationMethod different operation methods
     */
    void simulation(int operationMethod) throws Exception {
        int departedBusNo = 0;
        int time = 0;
        // Main part of operation.
        for(time = START_TIME; time< END_TIME; time++){
            // 0. Status check, if all bus have arrived at the terminal, the simulation terminates
            boolean activeness = false;
            for(Bus bus : busArray) {
                if(bus.isActive()){
                    activeness = true;
                    break;
                }
            }
            if(!activeness && time > 1000){
                break;
            }
            if(time>=100000){
                int a=1;
            }
            // 1. Check: if there's bus to depart?
            if(departure_time_list.contains(time)){
                Bus thisBus = busArray.get(departedBusNo);
                // The very first bus skips the very first station.
                if(thisBus.getNumber()==0){
                    thisBus.setPosition(0.01);
                    thisBus.setNextStation(1);
                } else {
                    thisBus.setPosition(0);
                    thisBus.setNextStation(0);
                }
                thisBus.setActiveness(true);
                double sigma = this.speed_sigma[0];
                double mu = this.speed_mu[0];
                thisBus.setSpeed(randomSpeed(sigma,mu));
                thisBus.setAtStation(false);
                departedBusNo ++;
            }
            // 2. Move all active bus a step forward
            for(Bus bus : busArray){
                if(bus.isActive()){
                    /* if a bus is not at a station, the "station" is the index of its heading station;
                     * if a bus is at a station, the "station" is the station it is stopping at;
                     * "station" changes when the bus departs from a station.
                    */
                    int station = bus.getNextStation();
                    // 2.1. Bus is not at station
                    double next_station_position = this.station_position[bus.getNextStation()];
                    if(bus.isAtStation()==false){
                        // 2.1.1. Bus is about to arrive at a station
                        if (bus.isAboutToArriveNextStation(next_station_position)){
                            /* If the station is the terminal, inactive the bus.
                               Continue to the iteration of the next bus. */
                            if(station == STATION_AMOUNT-1) {
                                bus.setPosition(station_position[station]);
                                bus.alightPassengers(station);
                                bus.setActiveness(false);
                                bus.setNextStation(STATION_AMOUNT);
                                continue;
                            }
                            /* Else if the station is occupied, stop outside the station and wait until the station is freed.
                               Continue to the iteration of the next bus.*/
                            else if(station_status[station] == OCCUPIED){
                                /* */
                                continue;
                            }
                            /* Else if the station is free, */
                            else if(station_status[station] == FREE) {
                                /* occupies the station, and arrive at the station */
                                station_status[station] = OCCUPIED;
                                bus.setAtStation(true);
                                bus.setPosition(station_position[station]);
                                /* renew R[i][j] */
                                bus_running_time_R[station][bus.getNumber()] = time - bus.getLastDepartureTime();
                                // The bus alight passengers
                                int alight_passenger_num = bus.alightPassengers(station);
                                int total_alighting_time = ALIGHTING_TIME_PER_PASSENGER * alight_passenger_num;

                                // The bus board passengers
                                /* old passengers who have not boarded the last bus and waiting at the station may have loss */
                                int time_interval = bus.getNumber() == 0 ? (int)(1.5 * DEPARTURE_INTERVAL) : time - station_last_departure_time[station];
                                double loss_probability_of_old_passengers = Passenger.lossPossibility(time_interval);
                                for (Passenger old_pax : waiting_passengers.get(station)) {
                                    if (loss_probability_of_old_passengers >= RANDOM.nextDouble()) {
                                        old_pax.setStatus(false);
                                    }
                                }
                                waiting_passengers.get(station).removeIf(pax -> pax.getPassengerStatus()==false);

                                /* new arrive passengers accumulate since last departure, and board them */
                                int accumulation_start = station_last_departure_time[station];
                                int accumulation_end = time;
                                int new_pax_number = 0;
                                int total_aboard_pax_number = 0;
                                int new_aboard_pax_number = 0;
                                do{
                                    new_pax_number = accumlateNewPassengers(accumulation_end - accumulation_start, station, time);
                                    new_aboard_pax_number = 0;
                                    for(int p=0; p<new_pax_number; p++){
                                        int arrive_time = RANDOM.nextInt(accumulation_start, accumulation_end);
                                        double loss_probability = Passenger.lossPossibility(accumulation_end - arrive_time);
                                        if (loss_probability >= RANDOM.nextDouble()) {
                                            Passenger pax = new Passenger(station, arrive_time);
                                            pax.setStatus(false);
                                            all_passenger_list.add(pax);
                                        } else {
                                            Passenger pax = new Passenger(station, arrive_time);
                                            pax.setStatus(true);
                                            all_passenger_list.add(pax);
                                            waiting_passengers.get(station).add(pax);
                                        }
                                    }
                                    // Vacancy is enough? board all passengers.
                                    if (bus.getVacancy() >= waiting_passengers.get(station).size()) {
                                        for (int i = 0; i < waiting_passengers.get(station).size(); i++) {
                                            Passenger pax = waiting_passengers.get(station).get(i);
                                            bus.boardAPassenger(pax);
                                            new_aboard_pax_number++;
                                            pax.setBusNumber(bus.getNumber());
                                        }
                                        waiting_passengers.get(station).clear();
                                        accumulation_start = time;
                                        accumulation_end = time + new_aboard_pax_number * BOARDING_TIME_PER_PASSENGER;
                                        total_aboard_pax_number += new_aboard_pax_number;
                                    }
                                    // Vacancy is not enough?
                                    else {
                                        int vacancy = bus.getVacancy();
                                        for (int i = 0; i < vacancy; i++) {
                                            int size = waiting_passengers.get(station).size();
                                            int who_to_board_index = RANDOM.nextInt(size);
                                            Passenger pax = waiting_passengers.get(station).get(who_to_board_index);
                                            bus.boardAPassenger(pax);
                                            new_aboard_pax_number++;
                                            pax.setBusNumber(bus.getNumber());
                                            waiting_passengers.get(station).remove(who_to_board_index);
                                        }
                                        break;
                                    }
                                } while(new_aboard_pax_number>0);
                                int total_boarding_time = total_aboard_pax_number * BOARDING_TIME_PER_PASSENGER;
                                int dwell_time = total_boarding_time > total_alighting_time ? total_boarding_time : total_alighting_time;
                                bus.dwell_time_left = dwell_time;
                                /* renew T[i][j] */
                                bus_boarding_time_T[station][bus.getNumber()] = dwell_time;
                            }
                        }
                        // 2.1.2. Bus is not about to arrive at a station
                        else{
                            bus.setPosition(bus.getPosition() + bus.getSpeed());
                        }
                    }
                    // 2.2. Bus is stopping at a station
                    else if (bus.isAtStation()){
                        // 2.2.1. Bus is boarding & alighting passengers;
                        if (bus.dwell_time_left!=0) {
                            bus.dwell_time_left--;
                            // When the bus is about to depart, decide the intervention.
                            if (bus.dwell_time_left == 0) {
                                bus_headways_H[station][bus.getNumber()] = station_last_departure_time[station] == 0 ? DEPARTURE_INTERVAL : time - station_last_departure_time[station];
                                double sigma = this.speed_sigma[station];
                                double mu = this.speed_mu[station];
                                switch (operationMethod) {
                                    // In each Method, set speed & holding_time;
                                    case NO_CONTROL -> {
                                        bus.setSpeed(randomSpeed(sigma,mu));
                                        bus.holding_time_left = 0;
                                        break;
                                    }
                                    case AT_STATION_CONTROL -> {
                                        int intervention = interventionTime(bus,time);
                                        bus.setSpeed(randomSpeed(sigma,mu));
                                        bus.holding_time_left = intervention;
                                        break;
                                    }
                                    case INTER_STATION_CONTROL -> {
                                        int intervention = interventionTime(bus,time);
                                        bus.setSpeed(interventionSpeed(bus,time,intervention,sigma,mu));
                                        bus.holding_time_left = 0;
                                        break;
                                    }
                                    case JOINT_CONTROL -> {
                                        int intervention = interventionTime(bus,time);
                                        if(intervention>=0){
                                            bus.setSpeed(randomSpeed(sigma,mu));
                                            bus.holding_time_left = intervention;
                                        } else {
                                            bus.setSpeed(interventionSpeed(bus,time,intervention,sigma,mu));
                                            bus.holding_time_left = 0;
                                        }
                                        break;
                                    }
                                    case BASE_MINIMUM_HEADWAY -> {
                                        int headway_to_previous_bus = bus_headways_H[station][bus.getNumber()];
                                        int intervention = headway_to_previous_bus > 0.5*DEPARTURE_INTERVAL ? 0 : (int) (0.5*DEPARTURE_INTERVAL - headway_to_previous_bus);
                                        intervention = intervention < I_MAX ? intervention : I_MAX;
                                        bus.setSpeed(randomSpeed(sigma,mu));
                                        bus.holding_time_left = intervention;
                                        break;
                                    }
                                    case BASE_QUADRATIC_PROBLEM -> {
                                        int intervention = interventionTime_Eberlein(bus,time);
                                        bus.setSpeed(randomSpeed(sigma,mu));
                                        bus.holding_time_left = intervention;
                                        break;
                                    }
                                    case BASE_HE -> {
                                        int intervention = interventionTime_He(bus,time);
                                        bus.setSpeed(randomSpeed(sigma,mu));
                                        bus.holding_time_left = intervention;
                                        break;
                                    }

                                    default -> {
                                        throw new Exception();
                                    }
                                }
                                // If a bus is being hold at a station, it may board more passengers.
                                if(bus.holding_time_left!=0){
                                    int new_pax_during_holding = accumlateNewPassengers(bus.holding_time_left,station,time);
                                    int max_boarding_constrained_by_time = (int) (bus.holding_time_left/BOARDING_TIME_PER_PASSENGER);
                                    int max_boarding_constrained_by_vacancy = bus.getVacancy();
                                    int max_boarding = max_boarding_constrained_by_time < max_boarding_constrained_by_vacancy ?
                                            max_boarding_constrained_by_time : max_boarding_constrained_by_vacancy;
                                    for(int i=0; i<new_pax_during_holding; i++){
                                        int arrive_time = RANDOM.nextInt(time, time+bus.holding_time_left);
                                        Passenger pax = new Passenger(station,arrive_time);
                                        if(i<max_boarding){
                                            pax.setStatus(true);
                                            all_passenger_list.add(pax);
                                            bus.boardAPassenger(pax);
                                        } else {
                                            pax.setStatus(true);
                                            all_passenger_list.add(pax);
                                            waiting_passengers.get(station).add(pax);
                                        }
                                    }
                                }
                            }
                        }
                        // 2.2.2. (ONLY IN AT-STATION CONTROL) Bus is being hold at a station.
                        if (bus.dwell_time_left==0 && bus.holding_time_left!=0){
                            bus.holding_time_left--;
                        }
                        // 2.2.3. If dwell_time and holding_time are both equal to zero, the bus departs.
                        if (bus.dwell_time_left==0 && bus.holding_time_left==0){
                            station_last_departure_time[station] = time;
                            bus.setAtStation(false);
                            station_status[station] = FREE;
                            bus.setPosition(bus.getPosition() - 0.1 + bus.getSpeed());
                            bus.setNextStation(station + 1);
                            bus.setLastDepartureTime(time);
                            /* The end of waiting time for passengers boarding at the station is the time for departure
                             * = current_time + boarding time */
                            for (Passenger pax : bus.passenger_on_board) {
                                if (pax.getAboardStation() == station) {
                                    pax.setEndofWaitingTime(time);
                                }
                            }
                        }
                    }
                }
            }
            // 3. Record History
            if(this.record_position_history){
                int[] position_now = new int[BUS_AMOUNT];
                for(int i=0; i<BUS_AMOUNT; i++){
                    position_now[i] = (int) (busArray.get(i).getPosition()+0.5);
                }
                position_history.add(position_now);
            }
        }
//        System.out.println("END TIME = " + time);
    }

    /**
     * See how many customers accumulates during two buses
     * @param time_interval time interval between two consecutive buses.
     * @param station the considered station
     * @param time the current time, it matters when we use real world data (arrival rate varies with time)
     * @return the total number of accumulated passengers, which is a random variable generated by Poisson.
     */
    int accumlateNewPassengers(int time_interval, int station, int time){
        double lam = arrival_rate.get(time/1800)[station] * time_interval;
        int new_arrive_passenger_num = Poisson(lam);
        return new_arrive_passenger_num;
    }

    /**
     * This is the implementation of Algorithm 3.
     * @param bus current bus
     * @param time current time
     * @return the optimal intervention time given by the robust algorithm.
     * @throws Exception from copies methods.
     */
    int interventionTime(Bus bus, int time) throws Exception {
        int bus_number = bus.getNumber();
        int station_now = bus.getNextStation();
        if (station_now==0){
            return 0;
        }
        else {
            int u = (bus_number + CONSIDER_RANGE_U > BUS_AMOUNT - 1) ? BUS_AMOUNT - 1 - bus_number : CONSIDER_RANGE_U;
            int[] bus_next_station = new int[u+1];
            for(int p = 0; p<=u; p++){
                bus_next_station[p] = busArray.get(bus_number+p).getNextStation();
            }
            int last_defined_station = Arrays.stream(bus_next_station).min().getAsInt();
            int intervention = 0;
            int[] H = Arrays.copyOfRange(bus_headways_H[last_defined_station], bus_number, bus_number+u+1);
            //initialize: start from i_u
            int[] worst_H = new int[u+1];
            int[] optimized_H = H.clone();
            for(int station = last_defined_station+1 ; station<=bus.getNextStation(); station++){
                // find the worst case (Algorithm 1)
                worst_H = worstCaseAtNextStation(optimized_H, station, bus, time);
                // optimize based on the worst case (Algorithm 2)
                int[] optimization_result = optimizedHeadways(worst_H, station, bus, time);
                intervention = optimization_result[0];
                optimized_H = Arrays.copyOfRange(optimization_result,1,optimization_result.length);
            }
            return (int) (intervention);
        }
    }

    /**
     * If we use inter-station control, we should convert the intervention time to an advised speed.
     * @param bus current bus
     * @param time current time
     * @param intervention intervention time returned by FUNC interventionTime
     * @param sigma param of speed (log-normal distribution)
     * @param mu param of speed (log-normal distribution)
     * @return optimal speed for inter-station control.
     * @throws Exception
     */
    double interventionSpeed(Bus bus, int time, int intervention, double sigma, double mu) throws Exception{
        int station = bus.getNextStation();
        double distance_to_next_station = (station_position[station+1]-station_position[station]);
        double average_travel_time = ( distance_to_next_station / speed_mean[station] );
        double expected_speed_under_control = distance_to_next_station / (average_travel_time + (double) intervention);
        double speed_noice = randomSpeed(sigma,mu) - speed_mean[station];
        double real_speed = expected_speed_under_control + speed_noice;
        double maximum_speed = Simulator.Speed_max * 1.2;
        double minimum_speed = Simulator.Speed_min * 0.8;
        if(real_speed > maximum_speed){
            real_speed = maximum_speed;
        } else if (real_speed < minimum_speed){
            real_speed = minimum_speed;
        }
        return real_speed;
    }

    /**
     * Method of Eberlein, Wilson and Bernstein (2001)
     * @param bus current bus
     * @param time current time
     * @return intervention
     */
    int interventionTime_Eberlein(Bus bus, int time){
        int bus_number = bus.getNumber();
        int station = bus.getNextStation();
        int u = (bus_number + CONSIDER_RANGE_U > BUS_AMOUNT - 1) ? BUS_AMOUNT - 1 - bus_number : CONSIDER_RANGE_U;
        int[] departure_time_list = new int[u+1];
        if(u==0){
            return 0;
        }
        departure_time_list[0] = time;
        int[] last_departure_time = Arrays.copyOfRange(station_last_departure_time,0,STATION_AMOUNT+1);
        for(int p=1; p<=u; p++){
            Bus bus_p = busArray.get(bus_number+p);
            int last_station_p = bus_p.getNextStation();
            int departure_time_p;
            if(last_station_p==0){
                departure_time_p = (bus_number+p) * DEPARTURE_INTERVAL;
            } else{
                departure_time_p = last_departure_time[last_station_p];
            }
            for(int s=last_station_p; s<station; s++){
                departure_time_p += (int) ((station_position[s+1] - station_position[s]) / speed_mean[s]);
                int headway_s_p = departure_time_p - last_departure_time[s+1];
                headway_s_p = headway_s_p>0 ? headway_s_p : 0;
                double r_s = ARRIVAL_RATE_LAMBDA;
                int dwell_time = (int) (headway_s_p * r_s / (1-r_s)) * BOARDING_TIME_PER_PASSENGER;
                departure_time_p += dwell_time;
                last_departure_time[s+1] = departure_time_p;
            }
            departure_time_list[p] = departure_time_p;
        }
        int hold_time = 0;
        double current_obj = objectiveFuction_Eberlein(departure_time_list,station);
        double next_obj = 0.0;
        while(true){
            hold_time++ ;
            int[] departure_time_list_next = new int[u+1];
            for(int j=0; j<=u; j++){
                departure_time_list_next[j] = departure_time_list[j]
                        + (int) (hold_time * Math.pow(-1*BOARDING_TIME_PER_PASSENGER*ARRIVAL_RATE_LAMBDA,j));
            }
            next_obj = objectiveFuction_Eberlein(departure_time_list_next,station);
            if (departure_time_list_next[1] - departure_time_list_next[0] <= 100){
                break;
            }
            if (next_obj>current_obj){
                break;
            } else {
                current_obj = next_obj;
            }
        }
        return hold_time;
    }
    /**
     * Method of He et al. (2020)
     * @param bus current bus
     * @param time current time
     * @return intervention
     */
    int interventionTime_He(Bus bus, int time){
        int bus_number = bus.getNumber();
        int station = bus.getNextStation();
        int u = (bus_number + CONSIDER_RANGE_U > BUS_AMOUNT - 1) ? BUS_AMOUNT - 1 - bus_number : CONSIDER_RANGE_U;
        if(u==0){
            return 0;
        }
        int[] headway_list = new int[u];
        for(int p=0; p<u; p++){
            headway_list[p] = (int) ((busArray.get(bus_number+p+1).getPosition() - busArray.get(bus_number+p).getPosition()) / AVG_SPEED);
        }
        int avg_headway = (int) (Arrays.stream(headway_list).average().getAsDouble());
        int hold_time = avg_headway - headway_list[0];
        if(hold_time>=0){
            return hold_time;
        } else {
            return 0;
        }
    }

    /**
     * The objective function of Eberlein, Wilson and Bernstein (2001)
     */
    double objectiveFuction_Eberlein(int[] departure_time,int station){
        int len = departure_time.length;
        double obj = 0.0;
        for(int i=1; i<len; i++){
            obj += ARRIVAL_RATE_LAMBDA * Math.pow(departure_time[i]-departure_time[i-1],2);
        }
        int[] departure_time_next = new int[len];
        departure_time_next[0] = departure_time[0] + (departure_time[0] - station_last_departure_time[station+1]);
        for(int j=1; j<len; j++){
            departure_time_next[j] = departure_time[j] + (int) ((departure_time[j]-departure_time[j-1]) * ARRIVAL_RATE_LAMBDA / (1-ARRIVAL_RATE_LAMBDA));
        }
        for(int k=1; k<len; k++){
            obj += ARRIVAL_RATE_LAMBDA * Math.pow(departure_time_next[k]-departure_time_next[k-1],2);
        }
        return obj;
    }

    /**
     * The implication of Algorithm 1.
     * @param worst_case_of_H The optimized worst case of Headways at the station
     * @param station current station
     * @param bus current bus
     * @param time current time
     * @return the worst case possible with in the uncertainty set at the next station
     * @throws Exception
     */
    int[] worstCaseAtNextStation(int[] worst_case_of_H, int station, Bus bus, int time) throws Exception {
        int u = worst_case_of_H.length - 1;
        int[] H_next_station = worst_case_of_H.clone();
        for(int j=0; j<=u; j++){
            if(bus_headways_H[station][bus.getNumber()+j] != 0){
                H_next_station[j] = bus_headways_H[station][bus.getNumber()+j];
            }
        }
        // For R:
        int[] R = Arrays.copyOfRange(bus_running_time_R[station], bus.getNumber(), bus.getNumber()+u+1);
        int expect_R = (int) ((station_position[station]-station_position[station-1]) / speed_mean[station] +0.5);
        ArrayList<Integer> adjustable_bus_list_R = new ArrayList<Integer>();
        for(int i=0 ; i<=u; i++){
            if(R[i]==0){
                adjustable_bus_list_R.add(i);
                R[i] = expect_R;
            }
        }
        int undefined_bus_R = adjustable_bus_list_R.size();
        double R_hat = RELATIVE_R_HAT * expect_R;
        double upper_bound_of_uncertainty_R = undefined_bus_R * GAMMA_R;
        double distributed_uncertainty_R = 0.0;
        while(distributed_uncertainty_R < upper_bound_of_uncertainty_R){
            double[] partial_H = new double[H_next_station.length];
            for(int i : adjustable_bus_list_R){
                partial_H[i] = partialOfVariance(H_next_station,i);
            }
            int[] indexSortByDerivative = indexSortByDerivative(partial_H);
            for(int index : indexSortByDerivative){
                if(adjustable_bus_list_R.contains(new Integer(index))){
                    int sign = partial_H[index] >=0 ? 1 : -1;
                    double uncertainty_left = upper_bound_of_uncertainty_R-distributed_uncertainty_R;
                    double xi = uncertainty_left > 1 ? 1 : uncertainty_left;
                    double adjustment = sign * xi * RELATIVE_R_HAT * expect_R;
                    R[index] += adjustment;
                    H_next_station[index] += adjustment;
                    if (index<=u-1) {
                        H_next_station[index+1] -= adjustment;
                    }
                    distributed_uncertainty_R += xi;
                    adjustable_bus_list_R.remove(new Integer(index));
                    break;
                }
            }
            if(adjustable_bus_list_R.isEmpty()){
                break;
            }
        }
        // For T:

        int[] T = Arrays.copyOfRange(bus_running_time_R[station], bus.getNumber(), bus.getNumber()+u);
        double[] T_hat = new double[u];
        ArrayList<Integer> adjustable_bus_list_T = new ArrayList<Integer>();
        for(int i=0 ; i<u; i++){
            if(T[i]==0){
                adjustable_bus_list_T.add(i);
                double expect_T = arrival_rate.get(time/1800)[station] * H_next_station[i] * BOARDING_TIME_PER_PASSENGER;
                T_hat[i] = RELATIVE_T_HAT * expect_T;
                T[i] = (int) (expect_T+0.5);
            }
        }
        int undefined_bus_T = adjustable_bus_list_T.size();
        double upper_bound_of_uncertainty_T = undefined_bus_R * GAMMA_R;
        double distributed_uncertainty_T = 0.0;
        while(distributed_uncertainty_T < upper_bound_of_uncertainty_T){
            double[] partial_H = new double[H_next_station.length];
            for(int j : adjustable_bus_list_T){
                partial_H[j] = partialOfVariance(H_next_station,j);
            }
            int[] indexSortByDerivative = indexSortByDerivative(partial_H);
            for(int index : indexSortByDerivative){
                if(adjustable_bus_list_T.contains(new Integer(index))){
                    int sign = partial_H[index] >=0 ? 1 : -1;
                    double uncertainty_left = upper_bound_of_uncertainty_T-distributed_uncertainty_T;
                    double xi = uncertainty_left > 1 ? 1 : uncertainty_left;
                    double adjustment = sign * xi * T_hat[index];
                    T[index] += adjustment;
                    H_next_station[index] += adjustment;
                    if (index<=u-1) {
                        H_next_station[index+1] -= adjustment;
                    }
                    distributed_uncertainty_T += xi;
                    adjustable_bus_list_T.remove(new Integer(index));
                    break;
                }
            }
            if(adjustable_bus_list_T.isEmpty()){
                break;
            }
        }
        return H_next_station;
    }

    /**
     * The implication of Algorithm 2.
     * @param worst_case_of_H the worst case possible with in the uncertainty set at the station
     * @param station current station
     * @param bus current bus
     * @param time current time
     * @return The optimized worst case of Headways at the station
     * @throws Exception
     */
    int[] optimizedHeadways(int[] worst_case_of_H, int station, Bus bus, int time) throws Exception {
        int u = worst_case_of_H.length - 1;
        int[] I = Arrays.copyOfRange(bus_intervene_I[station+1], bus.getNumber(), bus.getNumber()+u+1);
        int[] optimized_H = worst_case_of_H.clone();
        ArrayList<Integer> adjustable_bus_list = new ArrayList<Integer>();
        for(int i=0 ; i<=u; i++){
            if(I[i]==MINUS_INF){
                adjustable_bus_list.add(i);
                I[i] = 0;
            }
        }
        double threshold = CONSIDER_RANGE_U + 0.01;
        whileLoop:
        while(true) {
            double[] partial_I = new double[u + 1];
            for (int j : adjustable_bus_list) {
                partial_I[j] = partialOfVariance(optimized_H, j);
            }
            int[] indexSortByDerivative = indexSortByDerivative(partial_I);
            for (int i = 0; i < indexSortByDerivative.length; i++) {
                int index = indexSortByDerivative[i];
                int sign = partial_I[index] >= 0 ? 1 : -1;
                int adjustment = -1 * STEP_LENGTH * sign;
                // If satisfy the adjustment condition.
                if (adjustable_bus_list.contains(new Integer(index)) && I[index] + adjustment <= I_MAX && I[index] + adjustment >= I_MIN) {
                    // Can adjust, However it has alredy been very close to the optimal.
                    if( sign * partial_I[index] < threshold){
                        break whileLoop;
                    }
                    // Else, carry out the adjustment.
                    else {
                        I[index] += adjustment;
                        optimized_H[index] += adjustment;
                        if (index <= u - 1) {
                            optimized_H[index + 1] -= adjustment;
                        }
                        break;
                    }
                }
                // If all adjustable I do not satisfy the adjustment condition.
                else if ( index == indexSortByDerivative.length-1 ){
                    break whileLoop;
                }
            }
        }
        int[] return_I_H = new int[optimized_H.length+1];
        return_I_H[0] = I[0];
        for(int p=0; p<optimized_H.length; p++){
            return_I_H[p+1] = optimized_H[p];
        }
        return return_I_H;
    }
}
