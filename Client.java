import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
	DatagramSocket socket;
	static final int RETRY_LIMIT = 4;	/*
	 * UTILITY METHODS PROVIDED FOR YOU
	 * Do NOT edit the following functions:
	 *      exitErr
	 *      checksum
	 *      checkFile
	 *      isCorrupted
	 *
	 */

	/* exit unimplemented method */
	public void exitErr(String msg) {
		System.out.println("Error: " + msg);
		System.exit(0);
	}

	/* calculate the segment checksum by adding the payload */
	public int checksum(String content, Boolean corrupted)
	{
		if (!corrupted)
		{
			int i;
			int sum = 0;
			for (i = 0; i < content.length(); i++)
				sum += (int)content.charAt(i);
			return sum;
		}
		return 0;
	}


	/* check if the input file does exist */
	File checkFile(String fileName)
	{
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("SENDER: File does not exists");
			System.out.println("SENDER: Exit ..");
			System.exit(0);
		}
		return file;
	}


	/*
	 * returns true with the given probability
	 *
	 * The result can be passed to the checksum function to "corrupt" a
	 * checksum with the given probability to simulate network errors in
	 * file transfer
	 */
	public boolean isCorrupted(float prob)
	{
		double randomValue = Math.random();
		return randomValue <= prob;
	}



	/*
	 * The main method for the client.
	 * Do NOT change anything in this method.
	 *
	 * Only specify one transfer mode. That is, either nm or wt
	 */

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 5) {
			System.err.println("Usage: java Client <host name> <port number> <input file name> <output file name> <nm|wt>");
			System.err.println("host name: is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.err.println("input file name: is the file to send");
			System.err.println("output file name: is the name of the output file");
			System.err.println("nm selects normal transfer|wt selects transfer with time out");
			System.exit(1);
		}

		Client client = new Client();
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		InetAddress ip = InetAddress.getByName(hostName);
		File file = client.checkFile(args[2]);
		String outputFile =  args[3];
		System.out.println ("----------------------------------------------------");
		System.out.println ("SENDER: File "+ args[2] +" exists  " );
		System.out.println ("----------------------------------------------------");
		System.out.println ("----------------------------------------------------");
		String choice=args[4];
		float loss = 0;
		Scanner sc=new Scanner(System.in);


		System.out.println ("SENDER: Sending meta data");
		client.sendMetaData(portNumber, ip, file, outputFile);

		if (choice.equalsIgnoreCase("wt")) {
			System.out.println("Enter the probability of a corrupted checksum (between 0 and 1): ");
			loss = sc.nextFloat();
		}

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		switch(choice)
		{
			case "nm":
				client.sendFileNormal (portNumber, ip, file);
				break;

			case "wt":
				client.sendFileWithTimeOut(portNumber, ip, file, loss);
				break;
			default:
				System.out.println("Error! mode is not recognised");
		}


		System.out.println("SENDER: File is sent\n");
		sc.close();
	}


	/*
	 * THE THREE METHODS THAT YOU HAVE TO IMPLEMENT FOR PART 1 and PART 2
	 *
	 * Do not change any method signatures
	 */

	/* TODO: send metadata (file size and file name to create) to the server
	 * outputFile: is the name of the file that the server will create
	 */
	public void sendMetaData(int portNumber, InetAddress IPAddress, File file, String outputFile) {
		Object DatagramSocket = null;
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			socket.close();
			throw new RuntimeException(e);
		}
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			socket.close();
			throw new RuntimeException(e);
		}
		int bitRead;
		int totalBits = 0;
		while (true) {
			try {
				if (!((bitRead = fileInputStream.read()) != -1)) break;
			} catch (IOException e) {
				socket.close();
				throw new RuntimeException(e);
			}
			totalBits++;
		}
		// READ IN file length using stream somehow
		try {
			fileInputStream.close();
		} catch (IOException e) {
			socket.close();
			throw new RuntimeException(e);
		}

		MetaData metaData = new MetaData();
		metaData.setName("output.txt");
		metaData.setSize(totalBits);
	 	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(outputStream);
		} catch (IOException e) {
			socket.close();
			throw new RuntimeException(e);
		}
		try {
			os.writeObject(metaData);
		} catch (IOException e) {
			socket.close();
			throw new RuntimeException(e);
		}
		byte[] send = outputStream.toByteArray();
		DatagramPacket sendMetaData = new DatagramPacket(send, send.length, IPAddress, portNumber );
		try {
			socket.send(sendMetaData);
			System.out.println("SENDER: meta data is sent (file name, size): ("+ metaData.getName()+", "+ metaData.getSize()+")");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}


	/* TODO: Send the file to the server without corruption*/
	public void sendFileNormal(int portNumber, InetAddress IPAddress, File file) throws IOException {
		    FileInputStream fileInputStream = new FileInputStream(file);
			byte[] buffer = new byte[4]; //reads 4 bytes at one time
			int bytesRead;
			int sequenceCount=0;
			int totalSegs=0;

		System.out.println("SENDER: Start Sending File\n\n----------------------------------------");
			while((bytesRead = fileInputStream.read(buffer)) != -1) {

			//	System.out.println("Bytes read"+ bytesRead);

				int asciiSum = 0;

				for (byte myByte : buffer) {
					int asciiValue = myByte; // Convert to ASCII value
					asciiSum += asciiValue;

				}
				//System.out.println("ASCII SUM LOOP"+asciiSum);

				String text = new String(buffer, StandardCharsets.UTF_8);
			//	byte[] bytes = text.getBytes("US-ASCII");
				//System.out.println("Byte ascii sum"+ bytes);
				Segment seg0 = new Segment();
				seg0.setPayLoad(text);

				if (sequenceCount % 2 == 0) {
					seg0.setSq(0);
				}
				else{
					seg0.setSq(1);
				}
				sequenceCount++;
				totalSegs++;

				seg0.setSize(bytesRead);
				seg0.setType(SegmentType.Data);
				seg0.setChecksum(asciiSum);

				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
				objectOutputStream.writeObject(seg0);
				byte[] byteArray = byteArrayOutputStream.toByteArray();

				//use receive metadata to construct code to receive the network ack and use segtype.ack to check it's receiving it

				DatagramPacket sentPacket = new DatagramPacket(byteArray, byteArray.length, IPAddress, portNumber);
				socket.send(sentPacket);
				System.out.println("SENDER: Sending segment:"+ seg0.getSq()+", size:"+ seg0.getSize()+
						", checksum:"+ seg0.getChecksum()+", content:("+seg0.getPayLoad()+")\n");


				buffer = new byte[bytesRead];
				Segment ackSeg = new Segment();
				byte[] ackReceive = new byte[65535];
				DatagramPacket ackReceivePacket = new DatagramPacket(ackReceive,ackReceive.length);
				socket.receive(ackReceivePacket);
				byte[] ackData = ackReceivePacket.getData();
				ByteArrayInputStream ackIn = new ByteArrayInputStream(ackData);
				ObjectInputStream ackIs =  new ObjectInputStream(ackIn);
				try {
					ackSeg = (Segment) ackIs.readObject();
			//		System.out.println("Seg"+ ackSeg.getType() + ackSeg.getPayLoad() + ackSeg.getSq());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				System.out.println("SENDER: Waiting for an ack\n");
				System.out.println("ACK "+ ackSeg.getSq()+" RECEIVED.\n----------------------------------------\n");


			}
			System.out.println("Total segments sent: "+totalSegs+"\n");
			socket.close();




	}
	/* TODO: This function is essentially the same as the sendFileNormal function
	 *      except that it resends data segments if no ACK for a segment is
	 *      received from the server.*/
	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) {
		exitErr("sendFileWithTimeOut is not implemented");
	}


}