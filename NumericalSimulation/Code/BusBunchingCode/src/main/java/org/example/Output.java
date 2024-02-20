package org.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Output {

    public static final int STATION_AMOUNT = Simulator.STATION_AMOUNT;
    public static final int BUS_AMOUNT = Simulator.BUS_AMOUNT;

    public static double[] showGeneralResult(Simulator sim){
        // Local Variables
        int general_total_passenger = 0;
        int general_board_passenger = 0;
        int general_total_passenger_waiting_time = 0;

        for(Passenger pax : sim.all_passenger_list){
            if (pax.getAboardStation()!=0){
                general_total_passenger ++;
                if(pax.getPassengerStatus()){
                    general_board_passenger++;
                    general_total_passenger_waiting_time += pax.getEndofWaitingTime() - pax.getStartWaitingTime();
                }
            }
        }
        // Output data
        double general_PBR = (double) general_board_passenger/ (double) general_total_passenger;
        double general_AWT = (double) general_total_passenger_waiting_time/ (double) general_total_passenger;

        return new double[]{
                general_board_passenger,
                general_total_passenger,
                general_PBR,
                general_AWT,
        };
    }

    public static ArrayList<double[]> showStationResult(Simulator sim){
        // Local Variables
        int[] total_passenger = new int[Simulator.STATION_AMOUNT];
        int[] board_passenger = new int[Simulator.STATION_AMOUNT];
        int[] passenger_waiting_time = new int[Simulator.STATION_AMOUNT];

        for(Passenger pax : sim.all_passenger_list){
            int aboard_station = pax.getAboardStation();
            if (aboard_station!=0){
                total_passenger[aboard_station] ++;
                if(pax.getPassengerStatus()){
                    board_passenger[aboard_station] ++;
                    passenger_waiting_time[aboard_station] += pax.getEndofWaitingTime() - pax.getStartWaitingTime();
                }
            }
        }
        // Output data
        double[] PBR = new double[Simulator.STATION_AMOUNT];
        double[] AWT = new double[Simulator.STATION_AMOUNT];
        for(int s=1; s<Simulator.STATION_AMOUNT; s++){
            PBR[s] = (double) board_passenger[s] / (double) total_passenger[s];
            AWT[s] = (double) passenger_waiting_time[s] / (double) total_passenger[s];
        }
        ArrayList<double[]> result = new ArrayList<double[]>();
        result.add(PBR);
        result.add(AWT);
        return result;
    }

    public static void timePositionFigure(int control_method, Simulator sim) throws Exception {
        /**
         * Output to csv File.
         */
        String method = Main.getMethodName(control_method);
        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("MMdd-HHmmss");
        String path = "D:\\NumericalSimulation\\Results\\" + method + ".csv";
//      + "_" + dateFormat.format(date)
        File file = new File(path);
        OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), "gbk");
        BufferedWriter bw = new BufferedWriter(ow);
        int row_number = sim.position_history.size();
        int col_number = BUS_AMOUNT;
        for(int c = 0; c < col_number; c++){
            bw.write("Bus " + c + ",");
        }
        bw.write("\r\n");
        for(int r = 0; r < row_number; r++){
            for (int c = 0; c < col_number; c++){
                String string_item = String.valueOf(sim.position_history.get(r)[c]);
                bw.write(string_item);
                bw.write(" , ");
            }
            bw.write("\r\n");
        }
        bw.flush();
        bw.close();
        System.out.println("Have export position history to : " + path);
    }

    public static final int NO_CONTROL = 0;
    public static final int AT_STATION_CONTROL = 1;
    public static final int INTER_STATION_CONTROL = 2;
    public static final int JOINT_CONTROL = 3;
    public static final int BASE_MINIMUM_HEADWAY = 4;
    public static final int BASE_QUADRATIC_PROBLEM = 5;
    public static final int BASE_HE = 6;

    public void exportNumericalResults_numerical() throws Exception {
        int[] method_array = new int[]{NO_CONTROL, AT_STATION_CONTROL, INTER_STATION_CONTROL, JOINT_CONTROL, BASE_MINIMUM_HEADWAY, BASE_QUADRATIC_PROBLEM, BASE_HE};
        int iteration = 100;

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\NumericalExpeiments_PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\NumericalExpeiments_AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);

        String control_method_string = null;
        int I_min = 0;
        int I_max = 0;
        int departure_interval = 300;
        int consider_range_u = 5;
        double arrival_rate_lambda = 0.03;
        for (int control_method : method_array) {
            control_method_string = Main.getMethodName(control_method);
            bw_PBR.write( control_method_string + ",");
            bw_AWT.write( control_method_string + ",");
            if (control_method == AT_STATION_CONTROL || control_method == BASE_MINIMUM_HEADWAY) {
                I_max = 60;
                I_min = 0;
            } else if (control_method == INTER_STATION_CONTROL || control_method == JOINT_CONTROL) {
                I_max = 60;
                I_min = -20;
            }
            System.out.println("* Control Method == " + control_method_string + " *");
            for (int iter = 0; iter < iteration; iter++) {
                Simulator simulator = new Simulator(false, false, departure_interval, consider_range_u, arrival_rate_lambda, I_max, I_min);
                simulator.simulation(control_method);
                double[] result = showGeneralResult(simulator);
                System.out.println("Iter = " + iter + " : PBR = " + result[2] + " / AWT = " + result[3]);
                bw_PBR.write(result[2]+",");
                bw_AWT.write(result[3]+",");
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
            bw_PBR.flush();
            bw_AWT.flush();
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    public void exportNumericalResults_real() throws Exception {
        int[] method_array = new int[]{AT_STATION_CONTROL, INTER_STATION_CONTROL, JOINT_CONTROL, BASE_MINIMUM_HEADWAY, BASE_QUADRATIC_PROBLEM, BASE_HE};
        int iteration = 100;
        double[] PUR = new double[iteration];
        double[] AWT = new double[iteration];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\RealWorldExpeiments_PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\RealWorldExpeiments_AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);

        String control_method_string = null;
        int I_min = 0;
        int I_max = 0;
        int departure_interval = 500;
        int consider_range_u = 5;
        Simulator.STATION_AMOUNT = 49;
        Simulator.BUS_AMOUNT = 120;
        for (int control_method : method_array) {
            control_method_string = Main.getMethodName(control_method);
            bw_PBR.write( control_method_string + ",");
            bw_AWT.write( control_method_string + ",");
            if (control_method == AT_STATION_CONTROL || control_method == BASE_MINIMUM_HEADWAY) {
                I_max = 100;
                I_min = 0;
            } else if (control_method == INTER_STATION_CONTROL || control_method == JOINT_CONTROL) {
                I_max = 100;
                I_min = -33;
            }
            System.out.println("* Control Method == " + control_method_string + " *");
            for (int iter = 0; iter < iteration; iter++) {
                Simulator simulator = new Simulator(true, false, departure_interval, consider_range_u, 0, I_max, I_min);
                simulator.simulation(control_method);
                double[] result = showGeneralResult(simulator);
                System.out.println("Iter = " + iter + " : PBR = " + result[2] + " / AWT = " + result[3]);
                bw_PBR.write(result[2]+",");
                bw_AWT.write(result[3]+",");
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
            bw_PBR.flush();
            bw_AWT.flush();
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    public void exportNumericalResults_byStation() throws Exception{
        int[] method_array = new int[]{NO_CONTROL, AT_STATION_CONTROL, INTER_STATION_CONTROL, JOINT_CONTROL, BASE_MINIMUM_HEADWAY, BASE_QUADRATIC_PROBLEM, BASE_HE};
        int iteration = 100;
        double[] PUR = new double[iteration];
        double[] AWT = new double[iteration];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\StationExpeiments_PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\StationExpeiments_AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);

        String control_method_string = null;
        int I_min = 0;
        int I_max = 0;
        int departure_interval = 300;
        int consider_range_u = 5;
        double arrival_rate_lambda = 0.03;
        for (int control_method : method_array) {
            control_method_string = Main.getMethodName(control_method);
            bw_PBR.write( control_method_string + ",");
            bw_AWT.write( control_method_string + ",");
            double[] PBR_sum = new double[Simulator.STATION_AMOUNT];
            double[] AWT_sum = new double[Simulator.STATION_AMOUNT];
            if (control_method == AT_STATION_CONTROL || control_method == BASE_MINIMUM_HEADWAY) {
                I_max = 100;
                I_min = 0;
            } else if (control_method == INTER_STATION_CONTROL || control_method == JOINT_CONTROL) {
                I_max = 100;
                I_min = -33;
            }
            System.out.println("* Control Method == " + control_method_string + " *");
            for (int iter = 0; iter < iteration; iter++) {
                Simulator simulator = new Simulator(false, false, departure_interval, consider_range_u, arrival_rate_lambda, I_max, I_min);
                simulator.simulation(control_method);
                ArrayList<double[]> result = showStationResult(simulator);
                System.out.println("Iter = " + iter);
                for(int s=0; s<Simulator.STATION_AMOUNT; s++){
                    PBR_sum[s] += result.get(0)[s];
                    AWT_sum[s] += result.get(1)[s];
                }
            }
            for(int r=0; r<Simulator.STATION_AMOUNT; r++){
                bw_PBR.write(PBR_sum[r] / iteration + ",");
                bw_AWT.write(AWT_sum[r] / iteration + ",");
                bw_PBR.flush();
                bw_AWT.flush();
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    class RecursiveComputeTask extends RecursiveTask<double[]>{
        private int threshold = 1;
        private int begin;
        private int end;
        private int control_method;
        private int departure_interval;
        private int consider_range_u;
        private double arrival_rate;
        private int I_max;
        private int I_min;

        public RecursiveComputeTask(int begin, int end, int control_method, int departure_interval, int consider_range_u, double arrival_rate, int I_max, int I_min) {
            this.begin = begin;
            this.end = end;
            this.control_method = control_method;
            this.departure_interval = departure_interval;
            this.consider_range_u = consider_range_u;
            this.arrival_rate = arrival_rate;
            this.I_max = I_max;
            this.I_min = I_min;
        }

        @Override
        protected double[] compute() {
            double PBR_sum = 0;
            double AWT_sum = 0;
            if(end-begin<=threshold){
                Simulator simulator = null;
                try {
                    simulator = new Simulator(false, false, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                    simulator.simulation(control_method);
                } catch (Exception e) { throw new RuntimeException(e); }
                double[] result = showGeneralResult(simulator);
                System.out.printf("Method = %s, Iter = %d : PBR = %.3f, AWT = %.3f \n", control_method, begin, result[2], result[3]);
                PBR_sum += result[2];
                AWT_sum += result[3];
            } else {
                int middle = (begin + end) / 2;
                RecursiveComputeTask leftTask = new RecursiveComputeTask(begin, middle, control_method, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                RecursiveComputeTask rightTask = new RecursiveComputeTask(middle, end, control_method, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                invokeAll(leftTask,rightTask);
                double[] leftResult = leftTask.join();
                double[] rightResult = rightTask.join();
                PBR_sum = leftResult[0] + rightResult[0];
                AWT_sum = leftResult[1] + rightResult[1];
            }
            double[] result = new double[2];
            result[0] = PBR_sum;
            result[1] = AWT_sum;
            return result;
        }
    }
    public void sensitivityAnalysis_ArrivalRate() throws Exception{
        int[] method_array = new int[]{NO_CONTROL, AT_STATION_CONTROL, INTER_STATION_CONTROL, JOINT_CONTROL, BASE_MINIMUM_HEADWAY, BASE_QUADRATIC_PROBLEM, BASE_HE};
        int iteration = 100;
        double[] arrival_rate_array = new double[20];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_ArrivalRate-PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_ArrivalRate-AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);
        bw_PBR.write(",");
        bw_AWT.write(",");
        for(int i=0; i<20; i++){
            arrival_rate_array[i] = 0.005 * (i+1);
            bw_PBR.write(arrival_rate_array[i] + ",");
            bw_AWT.write(arrival_rate_array[i] + ",");
        }
        bw_PBR.write("\r\n");
        bw_AWT.write("\r\n");

        String control_method_string = null;
        int I_min = 0;
        int I_max = 0;
        int departure_interval = 300;
        int consider_range_u = 5;
        for (int control_method : method_array) {
            control_method_string = Main.getMethodName(control_method);
            bw_PBR.write( control_method_string + ",");
            bw_AWT.write( control_method_string + ",");
            if (control_method == AT_STATION_CONTROL || control_method == BASE_MINIMUM_HEADWAY) {
                I_max = 60;
                I_min = 0;
            } else if (control_method == INTER_STATION_CONTROL || control_method == JOINT_CONTROL) {
                I_max = 60;
                I_min = -20;
            }

            System.out.println("* Control Method == " + control_method_string + " *");
            for(double arrival_rate : arrival_rate_array){
                System.out.println("* Lambda = " + arrival_rate );
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                RecursiveComputeTask task = new RecursiveComputeTask(0, iteration, control_method, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                double result[] = forkJoinPool.invoke(task);
                forkJoinPool.shutdown();
                double average_PBR = result[0]/iteration;
                double average_AWT = result[1]/iteration;
                bw_PBR.write(average_PBR+",");
                bw_AWT.write(average_AWT+",");
                bw_PBR.flush();
                bw_AWT.flush();
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    public void sensitivityAnalysis_Imax() throws Exception{
        int[] method_array = new int[]{ AT_STATION_CONTROL, BASE_MINIMUM_HEADWAY};
        int iteration = 100;
        int[] Imax_array = new int[25];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_Imax-PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_Imax-AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);
        bw_PBR.write(",");
        bw_AWT.write(",");
        for(int i=0; i<25; i++){
            Imax_array[i] = 5 * i;
            bw_PBR.write(Imax_array[i] + ",");
            bw_AWT.write(Imax_array[i] + ",");
        }
        bw_PBR.write("\r\n");
        bw_AWT.write("\r\n");

        String control_method_string = null;
        int I_min = 0;
        int departure_interval = 300;
        int consider_range_u = 5;
        double arrival_rate = 0.03;

        for (int control_method : method_array) {
            control_method_string = Main.getMethodName(control_method);
            bw_PBR.write( control_method_string + ",");
            bw_AWT.write( control_method_string + ",");
            System.out.println("* Control Method == " + control_method_string + " *");
            for(int I_max : Imax_array){
                System.out.println("* I_max = " + I_max );
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                RecursiveComputeTask task = new RecursiveComputeTask(0, iteration, control_method, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                double result[] = forkJoinPool.invoke(task);
                forkJoinPool.shutdown();
                double average_PBR = result[0]/iteration;
                double average_AWT = result[1]/iteration;
                bw_PBR.write(average_PBR+",");
                bw_AWT.write(average_AWT+",");
                bw_PBR.flush();
                bw_AWT.flush();
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    public void sensitivityAnalysis_ImaxImin() throws Exception{
        int control_method = JOINT_CONTROL;
        int iteration = 100;
        int[] Imax_array = new int[25];
        int[] Imin_array = new int[13];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_ImaxImin-PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_ImaxImin-AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);
        bw_PBR.write(",");
        bw_AWT.write(",");
        for(int i=0; i<Imax_array.length; i++){
            Imax_array[i] = 5 * i;
            bw_PBR.write(Imax_array[i] + ",");
            bw_AWT.write(Imax_array[i] + ",");
        }
        for(int j=0; j<Imin_array.length; j++){
            Imin_array[j] = -5 * j;
        }
        bw_PBR.write("\r\n");
        bw_AWT.write("\r\n");

        String control_method_string = null;
        int departure_interval = 300;
        int consider_range_u = 5;
        double arrival_rate = 0.03;

        for(int I_min : Imin_array) {
            bw_PBR.write(I_min + ",");
            bw_AWT.write(I_min + ",");
            control_method_string = Main.getMethodName(control_method);
            System.out.println("* Control Method == " + control_method_string + " *");
            for (int I_max : Imax_array) {
                System.out.println("* I_min = " + I_min + " / I_max = " + I_max);
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                RecursiveComputeTask task = new RecursiveComputeTask(0, iteration, control_method, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                double result[] = forkJoinPool.invoke(task);
                forkJoinPool.shutdown();
                double average_PBR = result[0] / iteration;
                double average_AWT = result[1] / iteration;
                bw_PBR.write(average_PBR + ",");
                bw_AWT.write(average_AWT + ",");
                bw_PBR.flush();
                bw_AWT.flush();
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    public void sensitivityAnalysis_ConsiderationRangeU() throws Exception{
        int[] method_array = new int[]{NO_CONTROL, AT_STATION_CONTROL, INTER_STATION_CONTROL, JOINT_CONTROL, BASE_MINIMUM_HEADWAY, BASE_QUADRATIC_PROBLEM, BASE_HE};
        int iteration = 100;
        int[] U_array = new int[11];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path_PBR = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_U-PBR_"+ dateFormat.format(date) + ".csv";
        File file_PBR = new File(path_PBR);
        OutputStreamWriter ow_PBR = new OutputStreamWriter(new FileOutputStream(file_PBR), "gbk");
        BufferedWriter bw_PBR = new BufferedWriter(ow_PBR);
        String path_AWT = "D:\\NumericalSimulation\\Results\\SensitivityAnalysis_U-AWT_"+ dateFormat.format(date) + ".csv";
        File file_AWT = new File(path_AWT);
        OutputStreamWriter ow_AWT = new OutputStreamWriter(new FileOutputStream(file_AWT), "gbk");
        BufferedWriter bw_AWT = new BufferedWriter(ow_AWT);
        bw_PBR.write(",");
        bw_AWT.write(",");
        for(int i=0; i<U_array.length; i++){
            U_array[i] = i;
            bw_PBR.write(U_array[i] + ",");
            bw_AWT.write(U_array[i] + ",");
        }
        bw_PBR.write("\r\n");
        bw_AWT.write("\r\n");

        String control_method_string = null;
        int departure_interval = 300;
        double arrival_rate = 0.03;
        int I_min = 0;
        int I_max = 0;
        for (int control_method : method_array) {
            control_method_string = Main.getMethodName(control_method);
            bw_PBR.write( control_method_string + ",");
            bw_AWT.write( control_method_string + ",");
            if (control_method == AT_STATION_CONTROL || control_method == BASE_MINIMUM_HEADWAY) {
                I_max = 60;
                I_min = 0;
            } else if (control_method == INTER_STATION_CONTROL || control_method == JOINT_CONTROL) {
                I_max = 60;
                I_min = -20;
            }
            System.out.println("* Control Method == " + control_method_string + " *");
            for (int consider_range_u : U_array) {
                System.out.println("* U = " + consider_range_u);
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                RecursiveComputeTask task = new RecursiveComputeTask(0, iteration, control_method, departure_interval, consider_range_u, arrival_rate, I_max, I_min);
                double result[] = forkJoinPool.invoke(task);
                forkJoinPool.shutdown();
                double average_PBR = result[0] / iteration;
                double average_AWT = result[1] / iteration;
                bw_PBR.write(average_PBR + ",");
                bw_AWT.write(average_AWT + ",");
            }
            bw_PBR.write("\r\n");
            bw_AWT.write("\r\n");
            bw_PBR.flush();
            bw_AWT.flush();
        }
        bw_PBR.close();
        System.out.println("Have PBR Analysis to : " + path_PBR);
        bw_AWT.close();
        System.out.println("Have PBR Analysis to : " + path_AWT);
    }

    public void runtimeAnalysis() throws Exception{
        int control_method = JOINT_CONTROL;
        int[] bus_amount_array = new int[20];
        int[] station_amount_array = new int[10];

        Date date = new Date();
        SimpleDateFormat dateFormat= new SimpleDateFormat("HHmmss");
        String path = "D:\\NumericalSimulation\\Results\\Runtime_"+ dateFormat.format(date) + ".csv";
        File file = new File(path);
        OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(file), "gbk");
        BufferedWriter bw = new BufferedWriter(ow);
        bw.write(",");

        for(int i=0; i<bus_amount_array.length; i++){
            bus_amount_array[i] = 10 * (i+1);
            bw.write(bus_amount_array[i] + ",");
        }
        for(int j=0; j<station_amount_array.length; j++){
            station_amount_array[j] = 10 * (j+1);
        }
        bw.write("\r\n");

        int departure_interval = 300;
        int consider_range_u = 5;
        double arrival_rate_lambda = 0.03;
        int I_min = -20;
        int I_max = 60;

        for(int station_amount : station_amount_array) {
            bw.write(station_amount + ",");
            for (int bus_amount : bus_amount_array) {
                long start_time = System.currentTimeMillis();
                Simulator.BUS_AMOUNT = bus_amount;
                Simulator.STATION_AMOUNT = station_amount;
                Simulator simulator = new Simulator(false, false, departure_interval,consider_range_u,arrival_rate_lambda,I_max,I_min);
                simulator.simulation(control_method);
                long end_time = System.currentTimeMillis();
                long time_length = end_time - start_time;
                bw.write(time_length + ",");
                System.out.printf("Station = %d, Bus = %d, runtime %d.\n", station_amount,bus_amount,time_length);
            }
            bw.write("\r\n");
            bw.flush();
        }
        bw.close();
        System.out.println("Have PBR Analysis to : " + path);
    }

}
