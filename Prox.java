/**
 * This class merely stores relevant information for a Proxy
 */
public class Prox
{
    String ip, port;
    public Prox(String i, String portNumber){
        ip = i;
        port = portNumber;
    }
    
    public boolean equals(Object o){
        Prox other = (Prox)o;
        return ip.equals(other.ip) && port.equals(other.port);
    }
}
