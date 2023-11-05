import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
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



				DatagramPacket sentPacket = new DatagramPacket(byteArray, byteArray.length, IPAddress, portNumber);
				socket.send(sentPacket);
				System.out.println("SENDER: Sending segment:"+ seg0.getSq()+", size:"+ seg0.getSize()+
						", checksum:"+ seg0.getChecksum()+", content:("+seg0.getPayLoad()+")\n");

				//ack receive code
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
				System.out.println("ACK sq="+ ackSeg.getSq()+" RECEIVED.\n----------------------------------------\n");


			}
			System.out.println("Total segments sent: "+totalSegs+"\n");
			socket.close();




	}
	/* TODO: This function is essentially the same as the sendFileNormal function
	 *      except that it resends data segments if no ACK for a segment is
	 *      received from the server.*/
	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		byte[] buffer = new byte[4]; //reads 4 bytes at one time
		int bytesRead;
		int sequenceCount=0;
		int totalSegs=0;
		int maxRetriesPerSeg = 3;


		System.out.println("SENDER: Start Sending File\n\n----------------------------------------");
		while((bytesRead = fileInputStream.read(buffer)) != -1) {
			totalSegs++;
			int asciiSum = 0;
			int retry = 0;
			socket.setSoTimeout(1000);

			Segment seg0 = new Segment();

			for (byte myByte : buffer) {
				int asciiValue = myByte; // Convert to ASCII value
				asciiSum += asciiValue;

			}
			String text = new String(buffer, StandardCharsets.UTF_8);
			boolean retryLoop = true;
			while (retryLoop){
				seg0.setType(SegmentType.Data);
				seg0.setSize(bytesRead);
				if (sequenceCount % 2 == 0) {
					seg0.setSq(0);
				}
				else{
					seg0.setSq(1);
				}
				sequenceCount++;
				seg0.setPayLoad(text);
				boolean checkSumCorrupted= isCorrupted(loss);
				int byteCheck = checksum(text, checkSumCorrupted);

				if (checkSumCorrupted == true) {

					seg0.setChecksum(0); // Set the checksum to 0 if it's corrupted
					System.out.println("Corrupted Segment");
					retry++;

					break;


				} else {
					seg0.setChecksum(byteCheck); // Set the checksum to byteCheck if it's not corrupted
				}
				seg0.setChecksum(byteCheck);

				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
				objectOutputStream.writeObject(seg0);
				byte[] byteArray = byteArrayOutputStream.toByteArray();

				System.out.println("PACKET SEND");
				DatagramPacket sentPacket = new DatagramPacket(byteArray, byteArray.length, IPAddress, portNumber);
				socket.send(sentPacket);
				System.out.println("SENDER: Sending segment:"+ seg0.getSq()+", size:"+ seg0.getSize()+
						", checksum:"+ seg0.getChecksum()+", content:("+seg0.getPayLoad()+")\n");

				//ack receive code
				try {
					System.out.println("ACK RECEIVE");
					//buffer = new byte[bytesRead];
					Segment ackSeg = new Segment();
					byte[] ackReceive = new byte[65535];
					DatagramPacket ackReceivePacket = new DatagramPacket(ackReceive,ackReceive.length);
					socket.receive(ackReceivePacket);
					System.out.println("SOCKET ACK RECEIVE");
					byte[] ackData = ackReceivePacket.getData();
					ByteArrayInputStream ackIn = new ByteArrayInputStream(ackData);
					ObjectInputStream ackIs =  new ObjectInputStream(ackIn);
					try {
						ackSeg = (Segment) ackIs.readObject();

					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
					System.out.println("SENDER: Waiting for an ack\n");
					System.out.println("ACK sq="+ ackSeg.getSq()+" RECEIVED.\n----------------------------------------\n");
				} catch (SocketTimeoutException e) {
					retry++;
					if (retry>maxRetriesPerSeg){
						System.out.println("Max retries exceeded");
						return;
					}
					System.out.println("Attempt # "+retry);
				}


				//retryLoop = false;

				//File input stream close maybe
			}}
		System.out.println("Total segments sent: "+totalSegs+"\n");
		socket.close();




	}
}