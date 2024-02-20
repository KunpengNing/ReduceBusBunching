package org.example;
import java.util.Arrays;
import java.util.Random;

public class Functions {
    public static final Random RANDOM = new Random();

    public static double exponential(double lambda){
        double u = RANDOM.nextDouble();
        double x = (-1 / lambda) * Math.log(u);
        return x;
    }

    public static int Poisson(double lambda) {
        int passenger = 0;
        int time = 1;
        double total_time = exponential(lambda);
        while (total_time <= time){
            passenger++;
            total_time += exponential(lambda);
        }
        return passenger;
    }

    static double partialOfVariance(int[] H, int position){
        int len = H.length;
        if(0 <= position && position < len - 1){
            return 2 * (H[position] - H[position+1]) / (double)len;
        } else if (position == len - 1){
            return 2 * (H[position] - Simulator.DEPARTURE_INTERVAL)/ (double)len;
        }
        return 0;
    }

    static int[] indexSortByDerivative(double[] H){
        int minus_inf = -65536;
        double[] H_temp = H.clone();
        for(int i=0; i<H_temp.length; i++){
            H_temp[i] = H_temp[i]>=0 ? H_temp[i] : H_temp[i] * -1;
        }
        int[] index_sequence = new int[H_temp.length];
        for(int j=0; j<H_temp.length; j++){
            double max = Arrays.stream(H_temp).max().getAsDouble();
            for(int k=0; k<H_temp.length; k++){
                if(H_temp[k] == max){
                    index_sequence[j] = k;
                    H_temp[k] = minus_inf;
                    break;
                }
            }
        }
        return index_sequence;
    }

    static int[] sortIndexByValue(double[] H){
        int minus_inf = -65536;
        double[] H_temp = H.clone();
        int[] index_sequence = new int[H.length];
        for(int j=0; j<H.length; j++){
            double max = Arrays.stream(H_temp).max().getAsDouble();
            for(int k=0; k<H.length; k++){
                if(H_temp[k] == max){
                    index_sequence[j] = k;
                    H_temp[k] = minus_inf;
                    break;
                }
            }
        }
        return  index_sequence;
    }

    static double randomSpeed (double sigma, double mu){
        double speed = Math.exp(sigma * RANDOM.nextGaussian() + mu);
        if(speed > Simulator.Speed_max){
            speed = Simulator.Speed_max;
        } else if (speed < Simulator.Speed_min){
            speed = Simulator.Speed_min;
        }
        return speed;
    }
}
