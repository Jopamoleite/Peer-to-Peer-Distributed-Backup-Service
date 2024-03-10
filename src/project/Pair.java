package project;

import java.io.Serializable;

public class Pair<U, V> implements Serializable {

    public U first;   	// first field of a Pair
    public V second;  	// second field of a Pair

    // Constructs a new Pair with specified values
    public Pair(U first, V second)
    {
        this.first = first;
        this.second = second;
    }
}
