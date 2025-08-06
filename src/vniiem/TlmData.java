
package vniiem;

public class TlmData {
    private int syncMarker;    
    private int packetCounter; 
    private double timestamp;  
    private double value;      
    private int crc;           
    private boolean crcValid;  
    private boolean syncValid; 


    public TlmData(int syncMarker, int packetCounter, double timestamp, double value, int crc, boolean crcValid, boolean syncValid) {
        this.syncMarker = syncMarker;
        this.packetCounter = packetCounter;
        this.timestamp = timestamp;
        this.value = value;
        this.crc = crc;
        this.crcValid = crcValid;
        this.syncValid = syncValid;
    }

    public int getSyncMarker() { return syncMarker; }
    public int getPacketCounter() { return packetCounter; }
    public double getTimestamp() { return timestamp; }
    public double getValue() { return value; }
    public int getCrc() { return crc; }
    public boolean isCrcValid() { return crcValid; }
    public boolean isSyncValid() { return syncValid; }
    
    public boolean isValid() {
        return syncValid && crcValid;
    }

    @Override
    public String toString() {
        return "TlmData{" +
                "syncMarker=0x" + Integer.toHexString(syncMarker) +
                ", packetCounter=" + packetCounter +
                ", timestamp=" + timestamp +
                ", value=" + value +
                ", crc=0x" + Integer.toHexString(crc) +
                ", crcValid=" + crcValid +
                ", syncValid=" + syncValid +
                '}';
    }
}