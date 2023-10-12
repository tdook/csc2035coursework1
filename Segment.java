
	import java.io.Serializable;

	//Segment type - either Data segment or Acknowledgment segment
	enum SegmentType {
	    Data, Ack 
	}
	
	public class Segment implements Serializable {

	private static final long serialVersionUID = 1L;
	private int size;            //size of the payload
	private int sq;              //sequence number of segment
    private SegmentType type;    //segment type
    private String payLoad;      //payload data (file content in chunks)
    private int checksum;        //checksum of payload
    
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public int getSq() {
		return sq;
	}
	public void setSq(int sq) {
		this.sq = sq;
	}
	public SegmentType getType() {
		return type;
	}
	public void setType(SegmentType type) {
		this.type = type;
	}
	public String getPayLoad() {
		return payLoad;
	}
	public void setPayLoad(String payLoad) {
		this.payLoad = payLoad;
	}
	public int getChecksum() {
		return checksum;
	}
	public void setChecksum(int checksum) {
		this.checksum = checksum;
	}

 

	 

}
