package com.socialcops.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;
import com.socialcops.enums.DataIdentifier;
import com.socialcops.models.SocketStreamData;

/**
 * @author PratickChokhani Manages all the socket connections, receiving and
 *         sending of data.
 */
public class SocketManager {
	private static final Logger logger = LoggerFactory.getLogger(SocketManager.class);
	private static int IDENTIFIER_BYTES = DataIdentifier.FID.getNameInByte().length;

	private Socket socket = null;
	private ServerSocket server = null;
	private final String address;
	private int port;
	private boolean master;

	/**
	 * @param port
	 *            Master port
	 * @param address
	 *            Master IP address
	 * @param master
	 *            true if current run is master else false
	 */
	public SocketManager(int port, String address, boolean master) {
		this.address = address;
		this.port = port;
		this.master = master;
	}

	/**
	 * Setup connection with master
	 * 
	 * @return true if connection is established
	 */
	public boolean initiateClient() {
		// establish a connection
		try {
			logger.info("Connecting to server.");
			socket = new Socket(address, port);
			socket.setKeepAlive(true);
			logger.info("Client Connected");
			return true;
		} catch (Exception e) {
			logger.error(MessageFormat.format("Cannot initiate master at port:{0}", port), e);
			return false;
		}

	}

	/**
	 * Creates server socket and establish connection with clieck
	 * 
	 * @return true if connection is established
	 */
	public boolean initiateMaster() {
		// starts server and waits for a connection
		try {
			server = new ServerSocket(port);
			logger.info("Master started");

			logger.info("Waiting for a client ...");
			return acceptConnection();
		} catch (Exception e) {
			logger.error(MessageFormat.format("Cannot initiate master at port:{0}", port), e);
			return false;
		}
	}

	/**
	 * Accepts clients connection
	 * 
	 * @return true is connection is established
	 * @throws IOException
	 */
	public boolean acceptConnection() throws IOException {

		socket = server.accept();
		socket.setKeepAlive(true);
		logger.info("Client accepted");
		return true;
	}

	/**
	 * @return available data size in input stream
	 * @throws IOException
	 */
	public synchronized int checkDataAvailability() throws IOException {
		return socket.getInputStream().available();
	}

	/**
	 * @return true is connection is open
	 * @throws IOException
	 */
	public synchronized boolean testConnection() throws IOException {
		if (socket == null || socket.isClosed() || !socket.isConnected()) {
			if (master) {
				return acceptConnection();
			} else {
				return initiateClient();
			}
		}
		return true;
	}

	/**
	 * Reads file and store in output stream
	 * 
	 * @param out
	 *            output stream where the read file is to be stored
	 * @param fileSize
	 *            size of the file to be read
	 * @throws IOException
	 */
	public synchronized void readFile(OutputStream out, long fileSize) throws IOException {

		byte[] data = new byte[4096];
		long sizeWritten = 0;
		int currentSize = 0;
		InputStream in = socket.getInputStream();
		while (sizeWritten < fileSize) {
			if (fileSize - currentSize < data.length) {
				currentSize = in.read(data, 0, (int) (fileSize - sizeWritten));
				out.write(data, 0, currentSize);
			} else {
				currentSize = in.read(data);
				out.write(data, 0, currentSize);
			}
			sizeWritten += currentSize;
		}
	}

	/**
	 * Streams file from input stream and send to
	 * 
	 * @param size
	 *            size of the data to be sent
	 * @param in
	 *            from where file is to be read
	 * @throws IOException
	 */
	public synchronized void sendFile(long size, InputStream in) throws IOException {

		IOUtils.write(DataIdentifier.FIR.getNameInByte(), socket.getOutputStream());
		IOUtils.write(Longs.toByteArray(size), socket.getOutputStream());
		OutputStream out = socket.getOutputStream();
		IOUtils.copy(in, out);

	}

	/**
	 * Receive bytes from socket
	 * 
	 * @param size
	 *            size of data in bytes to be received from socket
	 * @return received bytes
	 * @throws IOException
	 */
	public synchronized byte[] getByteFromSocket(long size) throws IOException {

		return IOUtils.toByteArray(socket.getInputStream(), size);
	}

	/**
	 * Receive data identifier from socket stating what next data is about and
	 * will be the size of data
	 * 
	 * @return SocketStream containing data identifier and size of next data
	 *         both in bytes
	 * @throws IOException
	 */
	public synchronized SocketStreamData getDataIdentifier() throws IOException {
		byte[] dataIdentifier = IOUtils.toByteArray(socket.getInputStream(), IDENTIFIER_BYTES);
		byte[] dataSize = IOUtils.toByteArray(socket.getInputStream(), Long.BYTES);
		return new SocketStreamData(dataIdentifier, Longs.fromByteArray(dataSize));
	}

	/**
	 * Send the data with the identifier to socket
	 * 
	 * @param dataIdentifiers
	 *            identifying the type of data
	 * @param data
	 * @throws IOException
	 */
	public synchronized void sendByteToSocket(DataIdentifier dataIdentifiers, byte[] data) throws IOException {
		long length = data.length;
		IOUtils.write(dataIdentifiers.getNameInByte(), socket.getOutputStream());
		IOUtils.write(Longs.toByteArray(length), socket.getOutputStream());
		IOUtils.write(data, socket.getOutputStream());
	}

	/**
	 * Close connection
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}

		if (server != null) {
			server.close();
		}
	}
}
