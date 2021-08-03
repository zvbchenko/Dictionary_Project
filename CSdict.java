
// You can use this file as a starting point for your dictionary client
// The file contains the code for command line parsing and it also
// illustrates how to read and partially parse the input typed by the user.
// Although your main class has to be in this file, there is no requirement that you
// use this template or hav all or your classes in this file.

import java.lang.System;
import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.util.Arrays;
import java.io.BufferedReader;
import java.net.*;
import java.io.*;
//
// This is an implementation of a simplified version of a command
// line dictionary client. The only argument the program takes is
// -d which turns on debugging output.
//


public class CSdict {
    static final int MAX_LEN = 255;
    static Boolean debugOn = false;
    private static final int PERMITTED_ARGUMENT_COUNT = 1;
	private static final int PERMITTED_ARGUMENT_OPEN_COUNT = 2; // for server port
	static Boolean connectionOpened = false;
	private static Socket clientSocket;
	private static PrintWriter out;
	private static BufferedReader in;
    private static String command;
    private static String[] arguments;
    static String defaultdict= "*";
    static String dictset = null;


    public static void main(String [] args) {
		for (;;){ // infinite loop for shell

        byte cmdString[] = new byte[MAX_LEN];
		int len;
		// Verify command line arguments

        if (args.length == PERMITTED_ARGUMENT_COUNT) {
            debugOn = args[0].equals("-d");
            if (debugOn) {
                System.out.println("Debugging output enabled");
            } else {
                System.out.println("997 Invalid command line option - Only -d is allowed");
                return;
            }
        } else if (args.length > PERMITTED_ARGUMENT_COUNT) {
            System.out.println("996 Too many command line options - Only -d is allowed");
            return;
        }


	// Example code to read command line input and extract arguments.

        try {
			System.out.print("csdict> ");
			System.in.read(cmdString);

			// Convert the command string to ASII
			String inputString = new String(cmdString, "ASCII");

			// Split the string into words
			String[] inputs = inputString.trim().split("( |\t)+");
			// Set the command
			command = inputs[0].toLowerCase().trim();
			// Remainder of the inputs is the arguments.
			arguments = Arrays.copyOfRange(inputs, 1, inputs.length);

			// Comment and empty strings get ignored
			if (command.startsWith("#") || command.isEmpty()){
				continue;
			}
			System.out.println("The command is: " + command);
			len = arguments.length;
			System.out.println("The arguments are: ");
			for (int i = 0; i < len; i++) {
			System.out.println("    " + arguments[i]);
			}

			try{
				switch(command){
					case "open":
						if (len == PERMITTED_ARGUMENT_OPEN_COUNT){			// checks the amount of provided arguments(should be 2)
							if (connectionOpened == false){					// checks if the connection is opened
								openconnection(arguments[0], arguments[1]); // opens connection
							}else {
								System.out.println("903 Supplied command not expected at this time");
							}
						} else{
							System.out.println("901 Incorrect number of arguments.");
						}
						break;
					case "quit":
						if (len == 0){
							if (connectionOpened == false){					// exits right away if connection is closed
								System.exit(-1);
							}else {
								clientSocket.close();						// closes the socket
								in.close();									// closes buffered reader
								out.close();								// closes print writer
								connectionOpened = false;					// changes boolean tracker of connection
								System.exit(-1);							// exits
							}
						} else{
							System.out.println("901 Incorrect number of arguments.");
						}
						break;

					case "dict":
						if (len != 0){										// no arguments should be provided
							System.out.println("901 Incorrect number of arguments");
							break;
						}
						if (connectionOpened == true){
							String msg = "SHOW DB";							// command to send to server
							out.println(msg);								// sends the command to the server
							System.out.println("Response : ");
							String response;
							if (debugOn){ 									// debug mode on
								System.out.println("> " + msg);				// print command sent to server
								while ( (response = in.readLine()) != null && !response.startsWith(".")){
									System.out.println("<-- " + response);		// print the found matches
								}
							} else{
								// response is printed from the buffered reader
								// reads up until "." or when the line is null
								while ( (response = in.readLine()) != null && !response.startsWith(".")){
									if(!response.startsWith("220") && !response.startsWith("110") ){
										// prints response without the codes
										System.out.println(response);
									}
								}
							}

						} else{
							System.out.println("903 Supplied command not expected at this time");
						}
						break;
					case "close":
						if (len != 0){										// no arguments should be provided
							System.out.println("901 Incorrect number of arguments");
							break;
						}
						if (connectionOpened == true){						// checks the connection
							clientSocket.close();							// closes the socket
							in.close();										// closes buffered reader
							out.close();									// closes print writer
							connectionOpened = false;						// changes boolean tracker of connection
						} else{
							System.out.println("903 Supplied command not expected at this time");
						}
						break;
					case "set":
						if(connectionOpened == true){						// checks the connection
							if (len <= PERMITTED_ARGUMENT_COUNT){			// checks the number of arguments
								if (len == 0){								// if none provided
									dictset = null;							// no dictionary is set
									break;
								} else{
									if (arguments[0] instanceof String == false){
										System.out.println("902 Invalid argument");
										break;								// check the type of argument
									}
									dictset = arguments[0];					// set dictionary
								}
							} else{
								System.out.println("901 Incorrect number of arguments");
							}
						} else{
							System.out.println("903 Supplied command not expected at this time");
						}
						break;
					case "define":
						if (connectionOpened == true){
							if (len <= PERMITTED_ARGUMENT_COUNT){
								if (len == 0){								// no word to define is provided
									System.out.println("901 Incorrect number of arguments");
									break;
								} else{
									if (arguments[0] instanceof String == false){
										System.out.println("902 Invalid argument");
										break;								// check the type of argument
									}
									String msg;
									if(dictset != null){					// case when dictionary has been set
										// command to send to server
										msg = "DEFINE "+ dictset+ " " + arguments[0];
										out.println(msg);
									}else{
										// case when no dictionary has been set
										// command to send to server
										msg = "DEFINE * " + arguments[0];
										out.println(msg);
									}
									System.out.println("Response : ");
									String response;
									if (debugOn){ 									// debug mode on
										System.out.println("> " + msg);				// print command sent to server
										while ( (response = in.readLine()) != null && !response.startsWith(".")){
											if (response.startsWith("552")){
												System.out.println("<-- " + response);
												break;
											}
											System.out.println("<-- " + response);		// print the found matches
										}
										if (response.startsWith("552")) {    // reads the error code, outputs the comment
											System.out.println("***No definition found***");
											if (dictset != null) {            // default matching based on dictionary set
												msg = "MATCH " + dictset + " . " + arguments[0];
												out.println(msg);
											} else {                            // default matching in all dictionaries
												msg = "MATCH * . " + arguments[0];
												out.println(msg);
											}
											System.out.println("> " + msg);                // print command sent to server
											while ((response = in.readLine()) != null && !response.startsWith(".")) {
												if (response.startsWith("552")){
													System.out.println("<-- " + response);
													break;
												}
												System.out.println("<-- " + response);        // print the found matches
											}
										}
									} else{
										while ( (response = in.readLine()) != null && !response.startsWith("250")){
											if(!response.startsWith("220") && response.startsWith("151") ){
												response = response.replace("151", "");
												response = response.replace("\"", "");
												response = response.replaceAll("(?i)"+ arguments[0], "@");
												// gets rid of the codes and quotation marks
												// substitute the word and code
												System.out.println(response);	// if definition is found print the word and
												// the dictionary
												response = in.readLine();		// read next line for definition
												System.out.println(response);	//	print the word and the first line of the definition
												response = in.readLine();
												System.out.println(response);
											}
											if (response.startsWith("552")){	// reads the error code, outputs the comment
												System.out.println("***No definition found***");
												if(dictset != null){			// default matching based on dictionary set
													msg = "MATCH "+ dictset+ " . " + arguments[0];
													out.println(msg);
												}else{							// default matching in all dictionaries
													msg = "MATCH * . " + arguments[0];
													out.println(msg);
												}
												while ( (response = in.readLine()) != null && !response.startsWith(".")){
													if(!response.startsWith("220") && !response.startsWith("152")
															&& !response.startsWith("250") && !response.startsWith("552") ) {
														System.out.println(response);
														// prints response if something was found
													}
													if (response.startsWith("552")){
														System.out.println("****No matches found****");
														// response if nothing was found
														break;
													}

												}
												break;
											}

										}
									}

								}
							} else{
								System.out.println("901 Incorrect number of arguments");
							}
						} else{
							System.out.println("903 Supplied command not expected at this time");

						}
						break;
					case "match":
						if (connectionOpened == true){
							if (len <= PERMITTED_ARGUMENT_COUNT){			// checks whether the amount of arguments is correct
								if (len == 0){								// error - 0 arguments provided
									System.out.println("901 Incorrect number of arguments");
									break;
								} else{
									if (arguments[0] instanceof String == false){
										System.out.println("902 Invalid argument");
										break;								// check the type of argument
									}
									String msg;
									if(dictset != null){					// match exactly in the dictionary set
										msg = "MATCH "+ dictset+ " exact " + arguments[0];
										out.println(msg);
									}else{									// match exactly in all dictionaries
										msg = "MATCH * exact " + arguments[0];
										out.println(msg);
									}
									System.out.println("Response : ");
									String response;
									if (debugOn){ 									// debug mode on
										System.out.println("> " + msg);				// print command sent to server
										while ( (response = in.readLine()) != null && !response.startsWith(".")){
											System.out.println("<-- " + response);		// print the found matches
										}
									}else{
										while ( (response = in.readLine()) != null && !response.startsWith(".")){
											if(!response.startsWith("220") && !response.startsWith("152")
													&& !response.startsWith("250") ) {
												if (response.startsWith("552")){	// if nothing was found
													System.out.println("*****No matching word(s) found*****");
													break;
												}
													System.out.println(response);	// print the found matches
											}

										}
									}
								}
							} else{
								System.out.println("901 Incorrect number of arguments");
							}
						} else{
							System.out.println("903 Supplied command not expected at this time");
						}
						break;
					case "prefixmatch":
						if (connectionOpened == true){
							if (len <= PERMITTED_ARGUMENT_COUNT){
								if (len == 0){
									System.out.println("901 Incorrect number of arguments");
									break;
								} else{
									if (arguments[0] instanceof String == false){
										System.out.println("902 Invalid argument");
										break;								// check the type of argument
									}
									String msg;
									if(dictset != null){					// match by prefix in the dictionary set
										msg = "MATCH "+ dictset+ " prefix " + arguments[0];
										out.println(msg);
									}else{									// match by prefix in all dictionaries
										msg = "MATCH * prefix " + arguments[0];
										out.println(msg);
									}
									System.out.println("Response : ");
									String response;
									if (debugOn){ 									// debug mode on
										System.out.println("> " + msg);				// print command sent to server
										while ( (response = in.readLine()) != null && !response.startsWith(".")){
											System.out.println("<-- " + response);		// print the found matches
										}
									} else{
										while ( (response = in.readLine()) != null && !response.startsWith(".")){
											if(!response.startsWith("220") && !response.startsWith("152") && !response.startsWith("250") ) {
												if (response.startsWith("552")){	// if nothing was found
													System.out.println("***No matching word(s) found****");
													break;
												}
													System.out.println(response);	// print the found matches
											}

										}
									}
								}
							} else{
								System.out.println("901 Incorrect number of arguments");
							}
						} else{
							System.out.println("903 Supplied command not expected at this time");
						}
						break;
					default:
						System.out.println("900 Invalid command.");
				}
			}catch(IOException ioexception){
				clientSocket.close();							// closes the socket
				in.close();										// closes buffered reader
				out.close();									// closes print writer
				connectionOpened = false;						// changes boolean tracker of connection
				System.out.println("925 Control connection I/O error, closing control connection");
			}
	} catch (IOException exception) {
			System.err.println("998 Input error while reading commands, terminating.");
            System.exit(-1);
	}
	}
	}


	public static void openconnection(String server, String port){
		try {
			clientSocket = new Socket(server, Integer.parseInt(port));	// establish a connection with input as PrintWriter and output as Buffreader
			out	= new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			connectionOpened = true;
			if (debugOn){ 									// debug mode on
				String response = in.readLine();
				System.out.println("<-- " + response);		// print the found matches
			}
		} catch(IllegalArgumentException illegalport){					// illegal port was provided
			System.err.println("902 Invalid argument");
		} catch(IOException exception) {								// connection failed
			System.err.println("920 Control connection to " + server+ " on port" + port +" failed to open" );

		}
	}



}


