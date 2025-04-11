package javaeight;

import java.util.function.Function;

public class EmptyLambda {
    public static void main(String[] args) {
        
    }

    private void doit() {
        Function <Integer, Integer> x = i -> i + 1;
    }
}
