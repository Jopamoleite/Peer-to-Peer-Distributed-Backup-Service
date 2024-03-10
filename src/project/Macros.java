package project;

public class Macros {
    public final static byte CR = 0xD;
    public final static byte LF = 0xA;

    public static final Integer CHUNK_MAX_SIZE = 256000; //in bytes
    public static final Integer MAX_NUMBER_CHUNKS = 1000000;
    public static final long MAX_FILE_SIZE = ((long) Macros.MAX_NUMBER_CHUNKS) * ((long) Macros.CHUNK_MAX_SIZE);

    public static final long INITIAL_STORAGE = 1000000000;

    public static final String SUCCESS = "SUCCESS";
    public static final String FAIL = "FAIL";

    public static boolean checkPort(Integer port){
        if( port <1024 || port>= 1 << 16){
            System.err.println("\t <port_no> must be a 16 bit integer");
            return false;
        }
        return true;
    }
}
