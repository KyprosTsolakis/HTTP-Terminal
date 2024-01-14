package com.mycompany.echohttpserver;

import static com.mycompany.echohttpserver.database.conn;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.Base64;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.FileUtils;
import java.util.Scanner;

public class EchoHTTPServer extends Thread {

    boolean serververfication = false;
    String message = "";
    static String currentLocation = "";
    String superCommand_execution = "";
    int superCommand_execution_step = 0;
    String postData2 = "";
    String returnUserType = "";
    String currentUsername = "";
    String workingDirectory = "";
    String tempPostData2 = "";
    String historyFileLocation = "";
    String tempFinalCommand = "";
    Boolean logInVerified = false;
    private final int PORT = 9091;
    database dbObject = new database();
    public static List<String> standardCommands = Arrays.asList("touch", "rm", "chmod", "ls", "cp", "mv", "mkdir", "rmdir", "pwd", "ps", "which", "clear", "cat");
    public static List<String> builtInCommands = Arrays.asList("history", "move", "copy", "whoAmI", "addUser", "super", "showDir", "help", "delUser", "login", "logoff", "create", "encrypt", "decrypt", "cd");
    public static List<String> builtInCommandsAttrubutesList = new ArrayList<String>();

    public static String commandExecution(String[] finalCommandParts) {
        /*In this function we pass the string command of the user.
        Then, we break it into its individual parts, which in turn are added into a String List Array.
        This list array is then passed to the process builder to execute the command.*/
        String returnMessage = "";
        try {
            List<String> processBuilderAttrubutesList = new ArrayList<String>();
            for (int k = 0; k < finalCommandParts.length; k++) {
                processBuilderAttrubutesList.add(finalCommandParts[k]);
            }

            var processBuilder = new ProcessBuilder();
            processBuilder.command(processBuilderAttrubutesList);
            var process = processBuilder.start();
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String Templine = "";
                while ((Templine = reader.readLine()) != null) {
                    returnMessage += "<br/>" + Templine;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnMessage;
    }

    public static void main(String[] args) {
        builtInCommandsAttrubutesList.add("");
        builtInCommandsAttrubutesList.add("");
        builtInCommandsAttrubutesList.add("");
        builtInCommandsAttrubutesList.add("");

        //We get the working directory of our http server java software.
        currentLocation = System.getProperty("user.dir");
        EchoHTTPServer gtp = new EchoHTTPServer();
        gtp.start();
    }

    public void run() {
        try {
            File File = new File("/home/ntu-user/Users");
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setEncryptFiles(true);
            zipParameters.setCompressionLevel(CompressionLevel.MAXIMUM);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);
            if (!File.exists()) {
                if (File.mkdir()) {
                    System.out.println("directory was created");
                } else {
                    System.out.println("Directory was not created");
                }
            }
            ServerSocket server = new ServerSocket(PORT);
            System.out.println("Echo Server listening port: " + PORT);
            boolean shudown = true;

            //Get working directory
            workingDirectory = System.getProperty("user.dir");
            historyFileLocation = workingDirectory + "/history.txt";
            System.out.println(workingDirectory);

            while (shudown) {
                Socket socket = server.accept();
                InputStream is = socket.getInputStream();
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                line = in.readLine();
                String auxLine = line;

                line = "";
                // looks for post data
                int postDataI = -1;
                while ((line = in.readLine()) != null && (line.length() != 0)) {
                    System.out.println(line);
                    if (line.indexOf("Content-Length:") > -1) {
                        postDataI = new Integer(line
                                .substring(
                                        line.indexOf("Content-Length:") + 16,
                                        line.length())).intValue();

                    }
                }

                String postData = "";
                for (int i = 0; i < postDataI; i++) {
                    int intParser = in.read();
                    postData += (char) intParser;
                }

                postData = URLDecoder.decode(postData, "UTF-8");

                int index = postData.indexOf('+');
                while (index > -1) {

                    postData = postData.substring(0, index) + ' ' + postData.substring(index + 1);
                    index = postData.indexOf('+');
                }

                if (!postData.isEmpty() && postData != null && postData.length() != 0) {

                    //We remove the unneeded string from the user command just to clear the command up from unecessary string.
                    String userCommand = postData.trim().replace("user=", "");

                    /*Each command which is written by the user in the text box, it is saved in the history.txt file.
                    This is how our history command is implemented.*/
                    if (!superCommand_execution.equals("super_adduser") && !superCommand_execution.equals("logincommand")) {
                        File historyFile2 = new File(historyFileLocation);
                        FileWriter fw = new FileWriter(historyFile2, true);
                        fw.write(userCommand + "<br/>");
                        fw.close();
                    }

                    /*Each command is split into its various parts.*/
                    String[] finalCommandParts = userCommand.split(" ");

                    /*Using the serververification variable, we can understand whether the user is logged into the system or not.
                    To be able to execute any commands, a user must be first log in to the terminal.*/
                    if (serververfication == true) {
                        /*There are some commands which are executed in multiple steps.
                        Two such commands are the login, and the 'super addUser' command.
                        To be able to distinguish if we are indeed using one such command, and in which step of that command we are at, we use the following
                        two variables.
                        For instance, the login command is executed in 3 steps, whereas the super addUser command is executed in 5 steps.
                        By keeping track in which step of the command we are at, we can display to the user the appropriate messages on the terminal screen.
                        For istance, in one step of the login command we ask the user to input his password, while in the second step we ask the user to input
                        his password.
                        If we are not in one such 'step' command (else part), then the command is supposed to be a one-liner command, and thus no step tracking
                        is needed.
                        After all the steps of a step-command are executed, then those variables are again initialized to their initial values.*/
                        if (superCommand_execution.equals("") && superCommand_execution_step == 0) {
                            postData2 = "";
                            postData = "<span style='color: blue;'>" + postData + "</span>";

                            /*We check whether the given command is a standard command or not.
                            There are command which take just one argument, and commands which have multiple arguments.
                            To figure out with which type of command we are dealing with, after splitting the command,
                            we count the total number of the words containing in each command.
                            In the commands which deal with files or folders, we make sure to use the 'currentLocation' variable to denote the
                            actual path of each file/folder.
                            This is done, after spliting the command and taking its individual arguments, by concatenating the currentLocation variable
                            to the start of the affected arguments.*/
                            if (standardCommands.contains(finalCommandParts[0])) {
                                if (finalCommandParts.length > 1) {
                                    if (finalCommandParts[0].equals("chmod")) {
                                        finalCommandParts[2] = currentLocation + "/" + finalCommandParts[2];
                                        message = commandExecution(finalCommandParts);
                                    } else if (finalCommandParts[0].equals("rm")) {
                                        if (finalCommandParts.length == 2) {
                                            finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                            message = commandExecution(finalCommandParts);
                                        } else {
                                            finalCommandParts[2] = currentLocation + "/" + finalCommandParts[2];
                                            message = commandExecution(finalCommandParts);
                                        }
                                    } else if (finalCommandParts[0].equals("cp")) {
                                        finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                        finalCommandParts[2] = currentLocation + "/" + finalCommandParts[2];
                                        message = commandExecution(finalCommandParts);
                                    } else if (finalCommandParts[0].equals("mv")) {
                                        finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                        finalCommandParts[2] = currentLocation + "/" + finalCommandParts[2];
                                        message = commandExecution(finalCommandParts);
                                    } else if (finalCommandParts[0].equals("ls")) {
                                        finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                        message = commandExecution(finalCommandParts);
                                    } else if (finalCommandParts[0].equals("which")) {
                                        message = message = commandExecution(finalCommandParts);
                                    } else {
                                        finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                        message = commandExecution(finalCommandParts);
                                    }
                                    System.out.println(Arrays.deepToString(finalCommandParts));

                                } else {
                                    /*The commands listed here are commands consisting of just a single word, and thus, no string splitting is needed.*/
                                    if (finalCommandParts[0].equals("pwd")) {
                                        message = "</br>" + currentLocation;
                                    } else if (finalCommandParts[0].equals("ps")) {
                                        message = message = commandExecution(finalCommandParts);
                                    } else {
                                        String finalCommand1 = finalCommandParts[0] + " " + currentLocation;
                                        String[] finalCommandParts1 = finalCommand1.split(" ");
                                        message = commandExecution(finalCommandParts1);
                                    }
                                }

                            } else if (builtInCommands.contains(finalCommandParts[0])) {

                                /*The commands listed below, are the builtInCommand.
                                Those commands are basically created by us, to mimic the work of some actual commands.
                                For instance, the buildInCommand 'move', seen below, is just the build-in linux command 'mv'.
                                So basically what we did, is to just take the word 'move' and without the user knowing anything,
                                change it to the actual linux command which is identified by the system.
                                Moreover, as before, when we had to deal with commands which had the location of the file/folder in them, 
                                we made sure to concatenate the currentLocation variable at the start of the file/folder as before.*/
                                if (finalCommandParts[0].equals("move")) {
                                    finalCommandParts[0] = "mv";

                                    finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                    finalCommandParts[2] = currentLocation + "/" + finalCommandParts[2];

                                    message = commandExecution(finalCommandParts);
                                } else if (finalCommandParts[0].equals("copy")) {
                                    finalCommandParts[0] = "cp";

                                    finalCommandParts[1] = currentLocation + "/" + finalCommandParts[1];
                                    finalCommandParts[2] = currentLocation + "/" + finalCommandParts[2];

                                    message = commandExecution(finalCommandParts);
                                } else if (finalCommandParts[0].equals("whoAmI")) {

                                    /*The whoami command in linux, just outputs the username of the user who is logged into the system at the time of asking.
                                    So, our built-in whoAmI command, just takes the currentLocation, breaks it into its individual words, and then outputs
                                    the last one of those words, which is basically the username of the user who is logged in.*/
                                    String[] whoamiparts = currentLocation.split("/");
                                    message = "</br>" + whoamiparts[4];
                                } else if (finalCommandParts[0].equals("history")) {

                                    /*As descripted above, the history file contains all the strings written by the user in the dedicated text box.
                                    So, when the user executes the history command, the software reads the contents of that txt file, and outputs to
                                    the user all the commants he has written to the terminal up until that point.*/
                                    File historyFile = new File("history.txt");
                                    historyFile.createNewFile();

                                    try {
                                        String data = "";
                                        File myObj = new File("history.txt");
                                        Scanner myReader = new Scanner(myObj);
                                        while (myReader.hasNextLine()) {
                                            data += myReader.nextLine() + "</br>";

                                            message = "</br>" + data;
                                        }
                                        myReader.close();
                                    } catch (FileNotFoundException e) {
                                        message = "<br/> An error occured";
                                    }
                                } else if (finalCommandParts[0].equals("help")) {

                                    /*When the user executes the help command, he gets an output string showing him what are the available commands
                                    of the terminal.
                                    That string output is included in the help.txt file, and it's that txt file's contents which are displayed to the user.*/
                                    try {
                                        tempFinalCommand = "help";
                                        String helpPathFolder = System.getProperty("user.dir");
                                        String data = "";
                                        File myObj = new File(helpPathFolder + "/help.txt");
                                        Scanner myReader = new Scanner(myObj);
                                        String helpData = "";
                                        while (myReader.hasNextLine()) {
                                            helpData = myReader.nextLine();
                                            data += helpData + "</br>";
                                            message = "</br>" + data;
                                        }
                                        myReader.close();
                                    } catch (FileNotFoundException e) {
                                        message = "<br/> An error occured";
                                    }
                                } else if (finalCommandParts[0].equals("showDir")) {

                                    /*The showDir builtIn command, basically just outputs the current folder location of user.*/
                                    message = "</br>" + currentLocation;
                                } else if (finalCommandParts[0].equals("cd")) {
                                    /*Each user has its unique folder in the system.
                                        So, when a user logs into the system, his current location, is the location of his dedicated folder.
                                        When files, and folders are created using the corresponding builtin or standard commands, are basically created
                                        with respect to that dedicated user folder.
                                        So, the cd command is allowed only to be used to navigated in between folders which are invluded in the dedicated
                                        user file.
                                        If a user issues the cd .. command to move one, or more folders back, the software firt checks whether the user
                                        is indeed allowed to move so many folders back.
                                        The only reason a user is not allowed to move a number of folders back, is if he is trying to move out of the dedicated
                                        initial user folder.
                                        This is done with the use of two variables.
                                        The first variable, called allowedGoBack, is used to calculated how many folders a user is indeed allowed to go back.
                                        For instance, if the user is in the path /home/ntu-user/Users/kypros6/testfolder/testfolder2/ then he is allowed to only
                                        go back to folders.
                                        He is not allowed to go pass beyond the path /home/ntu-user/Users/kypros6/.
                                        So the allowedGoBack variable is equal to 2.
                                        The other variable we use is the goBack variable which counts the number of folders the user asks the system to move him back to.
                                        For instance, if the user has issued the command cd ../.. then that means that the goBack variable is equal to 2.
                                        So, the the allowedGoBack variable is greater than the goBack variable, then moving back directories is permitted.
                                        Otherwise, if the goBack variable is greater than the allowedGoBack variable, that means that the user has asked to move 
                                        back more directories than what he is allowed to. i.e. he asked to move back, past the point /home/ntu-user/Users/kypros6/
                                        which is not allowed.
                                        For calculating the aforementioned two variable, we use the split command with the "/" argument and then we count the number of 
                                        words from the split.
                                     */
                                    if (finalCommandParts[1].contains("..")) {
                                        String[] currentLocationParts = currentLocation.split("/home/ntu-user/Users/" + currentUsername + "/");
                                        String[] tempcurrentLocationParts = null;
                                        int allowedGoBack = 0;
                                        int goBack = 1;
                                        if (currentLocationParts.length > 1) {
                                            tempcurrentLocationParts = currentLocationParts[1].split("/");
                                            allowedGoBack = tempcurrentLocationParts.length;
                                            if (finalCommandParts[1].contains("/")) {
                                                String[] backFolders = finalCommandParts[1].split("/");
                                                goBack = backFolders.length;
                                            } else {
                                                goBack = 1;
                                            }
                                        }

                                        System.out.println(allowedGoBack);
                                        System.out.println(goBack);

                                        if (allowedGoBack >= goBack) {
                                            String baseFolder = "/home/ntu-user/Users/" + currentUsername + "/";

                                            String newLocation = baseFolder;
                                            for (int i = 0; i <= tempcurrentLocationParts.length - 1 - goBack; i++) {
                                                newLocation += tempcurrentLocationParts[i] + "/";
                                            }
                                            currentLocation = newLocation;
                                            currentLocation = currentLocation.replace("//", "/");
                                        }
                                    } else {
                                        currentLocation += "/" + finalCommandParts[1];
                                        currentLocation = currentLocation.replace("//", "/");
                                    }
                                } else if (finalCommandParts[0].equals("logoff")) {

                                    /*As mentioned before, for identifying whether a user is logged in or not, we use the serververfication variable.
                                    If the user is successfully logged in, then that variable is set to true.
                                    So, when a user issues the logoff builtin command, we set that variable to false to denote that the user is no longer logged
                                    into the system.
                                    We also initialize to their initial values some other variables.
                                    If the user is not logged in and he tries to issue any command, other than the login command, he gets a message saying that
                                    he must first log in to be able to use the terminal.*/
                                    if (serververfication == true) {
                                        serververfication = false;
                                        System.setProperty("user.name", "ntu-user");
                                        postData2 = "";
                                        superCommand_execution_step = 0;
                                        superCommand_execution = "";
                                        builtInCommandsAttrubutesList.set(0, "");
                                        builtInCommandsAttrubutesList.set(1, "");
                                        currentLocation = "/home/ntu-user/Users/";
                                        message = "<br/>  <span style='color:red;'>Logged off successfully.</span>";
                                    } else {
                                        message = "<br/>  <span style='color:red;'>Please log in first.</span>";
                                    }

                                } else if (finalCommandParts[0].equals("encrypt")) {
                                    try {
                                        File newUser = new File("/home/ntu-user/Users/" + finalCommandParts[1]);
                                        ZipFile zipFile = new ZipFile("/home/ntu-user/Users/" + finalCommandParts[1] + ".zip", finalCommandParts[2].toCharArray());
                                        zipFile.addFolder(new File("/home/ntu-user/Users/" + finalCommandParts[1] + "/" + finalCommandParts[1]), zipParameters);
                                        zipFile.addFile(new File("/home/ntu-user/Users/" + finalCommandParts[1] + "/" + finalCommandParts[1] + "/" + "usernamepassword.txt"), zipParameters);

                                        if (!newUser.exists()) {

                                            message = "</br> User not Found check in files to ensure user exists";
                                        } else {
                                            FileUtils.cleanDirectory(newUser);
                                            FileUtils.forceDelete(newUser);
                                            System.out.println("deleted");
                                        }
                                        // delfile.close();
                                        message = "<br/> Folder Encrypted. use command 'decrypt' to unlock ";
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                } else if (finalCommandParts[0].equals("decrypt")) {
                                    try {

                                        ZipFile zipFile = new ZipFile("/home/ntu-user/Users/" + finalCommandParts[1] + ".zip", finalCommandParts[2].toCharArray());
                                        zipFile.extractAll("/home/ntu-user/Users/" + finalCommandParts[1]);
                                        zipFile.removeFile("/home/ntu-user/Users/" + finalCommandParts[1] + ".zip");
                                        File Zip = new File("/home/ntu-user/Users/" + finalCommandParts[1] + ".zip");
                                        if (!Zip.exists()) {
                                            System.out.println("Not found");
                                        } else {
                                            Zip.delete();
                                        }
                                        message = "<br/> File unzipped with success";
                                    } catch (Exception e) {
                                        e.printStackTrace();

                                    }

                                } else if (finalCommandParts[0].equals("create")) {
                                    dbObject.makeJDBCConnection();

                                    dbObject.addDataToDB(finalCommandParts[1], finalCommandParts[2], finalCommandParts[3]);
                                    message = commandExecution(finalCommandParts);
                                    dbObject.closeJDBCConnection();

                                    File newUser = new File("/home/ntu-user/Users/" + finalCommandParts[1]);

                                    if (!newUser.exists()) {
                                        if (newUser.mkdir()) {
                                            File usernamepasswordtxt = new File("/home/ntu-user/Users/" + finalCommandParts[1] + "/usernamepassword.txt");
                                            if (usernamepasswordtxt.createNewFile()) {
                                                FileWriter writetouser = new FileWriter("/home/ntu-user/Users/" + finalCommandParts[1] + "/usernamepassword.txt");
                                                writetouser.write("Username: " + finalCommandParts[1] + " Password: " + finalCommandParts[2]);
                                                writetouser.close();
                                                message = "<br/> Account Created " + finalCommandParts[1];

                                            }
                                            message = "<br/> Account Created " + finalCommandParts[1];
                                        } else {
                                            message = "<br/> Account not Created. Try Again " + finalCommandParts[1];
                                        }
                                    }
                                    ZipFile zipFile = new ZipFile("/home/ntu-user/Users/" + finalCommandParts[1] + ".zip", finalCommandParts[2].toCharArray());
                                    zipFile.addFolder(new File("/home/ntu-user/Users/" + finalCommandParts[1]), zipParameters);
                                    zipFile.addFile(new File("/home/ntu-user/Users/" + finalCommandParts[1] + "/usernamepassword.txt"), zipParameters);
                                    FileUtils.cleanDirectory(newUser);
                                    FileUtils.deleteDirectory(newUser);

                                    message = "<br/> Folder Encrypted. use command 'decrypt' to unlock ";
                                } else if (finalCommandParts[0].equals("super")) {
                                    System.out.println(returnUserType);
                                    if (returnUserType.equals("super")) {
                                        /*The super command, works in a similar fashion to the super command in linux.
                                                Meaning, for the user to be able to execute some particular commands, the word super must be included at the 
                                                beginning of that command.
                                                For instance, for creating a user, one must write the following command.
                                                super addUser.
                                                Without the word super at the beginning of the command, the command will not be executed.*/
                                        if (finalCommandParts[1].equals("addUser")) {

                                            /*if the user has executed the super addUser command, then as mentioned before, we are basically executing 
                                                    a command which consists of multiple steps.
                                                    First, we use the variable superCommand_execution so that the software identifies that the addUser command has been
                                                    executed, and then the superCommand_execution_step to identify in which step of the command we are at.
                                                    After each step, as seen below, the superCommand_execution_step is increased.
                                             */
                                            superCommand_execution = "super_adduser";
                                            superCommand_execution_step = 1;
                                            postData2 += "</br>Enter username: ";
                                        }

                                        if (finalCommandParts[1].equals("delUser")) {

                                            /*The necessary connection is being made to the database and the user given in the delUser command is being deleted
                                                    from the database.*/
                                            dbObject.makeJDBCConnection();

                                            dbObject.removeDataFromDB(finalCommandParts[2]);

                                            dbObject.closeJDBCConnection();
                                            message = commandExecution(finalCommandParts);
                                            postData += "<br/>User " + finalCommandParts[2] + " deleted.";
                                        }

                                        if (finalCommandParts[1].equals("chPass")) {

                                            /*The necessary connection is being made to the database and the password of a given user is being changed accordingly.*/
                                            dbObject.makeJDBCConnection();

                                            dbObject.changeUserPassword(finalCommandParts[2], finalCommandParts[3]);

                                            dbObject.closeJDBCConnection();

                                            postData += "<br/>Password for user " + finalCommandParts[2] + " changed.";
                                        }

                                        if (finalCommandParts[1].equals("chUserType")) {

                                            /*The necessary connection is being made to the database and the user type of a given user is being changed accordingly.*/
                                            dbObject.makeJDBCConnection();

                                            dbObject.changeUserType(finalCommandParts[2], finalCommandParts[3]);

                                            dbObject.closeJDBCConnection();

                                            postData += "<br/>Type for user " + finalCommandParts[2] + " changed.";
                                        }
                                    } else {
                                        message = "<br/><span style='color:red;'>Super commands can only be executed by super users.</span>";
                                    }
                                } else {
                                    message = "<br/>Command not found";
                                }
                            } else {
                                message = "<br/>Command not found";
                            }

                            postData += "<span style='color: #40E0D0'>" + message + "</span>";
                        } else {
                            String tempPostData = postData.replace("user=", "");

                            /*If superCommand_execution is equal to super_adduser, that means that we have executed the addUser command, and in that case,
                            the software will take as through the series of the necessary steps until that command is completed.
                            As mentioned before, after each step, the superCommand_execution_step is increased.
                            Whatever the user inputs at each step, it is stored in the builtInCommandsAttrubutesList list array.
                            Those variables stored at each step, will be used at the final step for the command to be executed.
                            For instance, in the first step, the user is asked to enter the username of the user which is about to be created, while in the second 
                            step, he is being asked to input the password and so on...
                            During the final step, all those variables are used so that the user is correctly created in the database.
                            Before adding the user to the database, we check whether that account already exists in the database, and that the two given password stings
                            (password and re-typed password) match exactly.
                            For each newly creatd user, a dedicated folder is created also for that user. This is his home directory.
                            At the final step of this procedure, the necessary variables are initialized back to their default values.
                             */
                            if (superCommand_execution.equals("super_adduser")) {
                                if (superCommand_execution_step == 1) {
                                    postData2 += tempPostData + "</br>Enter password:";
                                    builtInCommandsAttrubutesList.set(0, tempPostData);

                                    superCommand_execution_step++;
                                } else if (superCommand_execution_step == 2) {
                                    String asterisk = "*".repeat(tempPostData.length());
                                    postData2 += asterisk + "</br>Retype password:";
                                    builtInCommandsAttrubutesList.set(1, tempPostData);

                                    superCommand_execution_step++;
                                } else if (superCommand_execution_step == 3) {
                                    String asterisk = "*".repeat(tempPostData.length());
                                    postData2 += asterisk + "</br>User type (standard/super):";
                                    builtInCommandsAttrubutesList.set(2, tempPostData);

                                    superCommand_execution_step++;
                                } else {
                                    builtInCommandsAttrubutesList.set(3, tempPostData);

                                    superCommand_execution_step++;

                                    if ((builtInCommandsAttrubutesList.get(1).toString()).equals(builtInCommandsAttrubutesList.get(2).toString())) {

                                        dbObject.makeJDBCConnection();

                                        String tempUsername = builtInCommandsAttrubutesList.get(0).toString();
                                        if ((builtInCommandsAttrubutesList.get(3).toString()).equals("standard")) {
                                            dbObject.addDataToDB(tempUsername, builtInCommandsAttrubutesList.get(1).toString(), "Standard");
                                            //message = commandExecution(finalCommandParts);

                                            File newUser = new File("/home/ntu-user/Users/" + tempUsername);

                                            if (!newUser.exists()) {
                                                if (newUser.mkdir()) {
                                                    File usernamepasswordtxt = new File("/home/ntu-user/Users/" + tempUsername + "/usernamepassword.txt");
                                                    if (usernamepasswordtxt.createNewFile()) {
                                                        FileWriter writetouser = new FileWriter("/home/ntu-user/Users/" + tempUsername + "/usernamepassword.txt");
                                                        writetouser.write("Username: " + tempUsername + " Password: " + builtInCommandsAttrubutesList.get(1).toString());
                                                        writetouser.close();
                                                        message = "<br/> Account Created " + tempUsername;

                                                    }
                                                    message = "<br/> Account Created " + tempUsername;
                                                } else {
                                                    message = "<br/> Account not Created. Try Again " + tempUsername;
                                                }
                                            }
                                            ZipFile zipFile = new ZipFile("/home/ntu-user/Users/" + tempUsername + ".zip", builtInCommandsAttrubutesList.get(1).toCharArray());
                                            zipFile.addFolder(new File("/home/ntu-user/Users/" + tempUsername), zipParameters);
                                            zipFile.addFile(new File("/home/ntu-user/Users/" + tempUsername + "/usernamepassword.txt"), zipParameters);
                                            FileUtils.cleanDirectory(newUser);
                                            FileUtils.deleteDirectory(newUser);

                                            postData2 += tempPostData + "</br>User added with success.";
                                        } else if ((builtInCommandsAttrubutesList.get(3).toString()).equals("super")) {
                                            dbObject.addDataToDB(tempUsername, builtInCommandsAttrubutesList.get(1).toString(), "Super");

                                            //message = commandExecution(finalCommandParts);
                                            File newUser = new File("/home/ntu-user/Users/" + tempUsername);

                                            if (!newUser.exists()) {
                                                if (newUser.mkdir()) {
                                                    File usernamepasswordtxt = new File("/home/ntu-user/Users/" + tempUsername + "/usernamepassword.txt");
                                                    if (usernamepasswordtxt.createNewFile()) {
                                                        FileWriter writetouser = new FileWriter("/home/ntu-user/Users/" + tempUsername + "/usernamepassword.txt");
                                                        writetouser.write("Username: " + tempUsername + " Password: " + builtInCommandsAttrubutesList.get(1).toString());
                                                        writetouser.close();
                                                        message = "<br/> Account Created " + tempUsername;

                                                    }
                                                    message = "<br/> Account Created " + tempUsername;
                                                } else {
                                                    message = "<br/> Account not Created. Try Again " + tempUsername;
                                                }
                                            }
                                            ZipFile zipFile = new ZipFile("/home/ntu-user/Users/" + tempUsername + ".zip", builtInCommandsAttrubutesList.get(1).toCharArray());
                                            zipFile.addFolder(new File("/home/ntu-user/Users/" + tempUsername), zipParameters);
                                            zipFile.addFile(new File("/home/ntu-user/Users/" + tempUsername + "/usernamepassword.txt"), zipParameters);
                                            FileUtils.cleanDirectory(newUser);
                                            FileUtils.deleteDirectory(newUser);

                                            postData2 += tempPostData + "</br>User added with success.";
                                        } else {
                                            postData2 += tempPostData + "</br>Please choose a correct user type.";

                                        }
                                        builtInCommandsAttrubutesList.set(0, "");
                                        builtInCommandsAttrubutesList.set(1, "");
                                        builtInCommandsAttrubutesList.set(2, "");
                                        builtInCommandsAttrubutesList.set(3, "");

                                        superCommand_execution = "";
                                        superCommand_execution_step = 0;

                                        dbObject.closeJDBCConnection();

                                    } else {
                                        postData2 += tempPostData + "</br>Passwords do not match.";

                                        builtInCommandsAttrubutesList.set(0, "");
                                        builtInCommandsAttrubutesList.set(1, "");
                                        builtInCommandsAttrubutesList.set(2, "");
                                        builtInCommandsAttrubutesList.set(3, "");

                                        superCommand_execution = "";
                                        superCommand_execution_step = 0;
                                    }

                                }

                            }

                        }
                    } else {

                        if (serververfication == false) {
                            /*If the user is not logged to the system (else part) he is not allowed to issue any command other than the login command, and thus, the necessary message
                            is being given to the user.*/
                            if (finalCommandParts[0].equals("login") && superCommand_execution_step == 0) {
                                /*If the user has issued the login command, then the superCommand_execution is set accordingly so that the software knows that this
                                    has happen and that it must then pass through the various steps of that procedure.
                                    This is why the superCommand_execution_step is used. To keep track of the steps of the login process.
                                    At each step, the appropriate message is shown to the users.
                                    i.e. first to enter his username, and then to enter his password.
                                    The strings given by the user in each of those steps, are saved into a list array, and are used in the final step to construct
                                    the appropriate query.
                                    At the end of the final step, those variables, along with some other are initialized back to their default values just to denote
                                    that the process has finished and the software can return back to its default execution*/
                                superCommand_execution = "logincommand";
                                superCommand_execution_step = 1;
                                postData2 += "</br>Enter username: ";

                            } else {
                                if (!finalCommandParts[0].equals("login") && superCommand_execution_step == 0) {
                                    message = "<br/>  <span style='color:red;'>Please use the 'login' command before issuing any other commands.</span>";
                                }

                                if (superCommand_execution_step == 1) {
                                    superCommand_execution_step = 2;
                                    tempPostData2 = postData.replace("user=", "");
                                    builtInCommandsAttrubutesList.set(0, tempPostData2);
                                    postData2 += "</br>Enter Password: ";
                                } else {
                                    if (superCommand_execution_step == 2) {
                                        tempPostData2 = postData.replace("user=", "");
                                        builtInCommandsAttrubutesList.set(1, tempPostData2);

                                        dbObject.makeJDBCConnection();
                                        String tempUsername = builtInCommandsAttrubutesList.get(0).toString();
                                        dbObject.validateUser(tempUsername, builtInCommandsAttrubutesList.get(1).toString());
                                        if (dbObject.verification == true) {
                                            serververfication = true;
                                            returnUserType = (dbObject.returnUserType).toLowerCase();
                                            System.setProperty("user.name", tempUsername);
                                            postData2 = "";
                                            superCommand_execution = "";
                                            superCommand_execution_step = 0;
                                            builtInCommandsAttrubutesList.set(0, "");
                                            builtInCommandsAttrubutesList.set(1, "");
                                            currentLocation = "/home/ntu-user/Users/" + tempUsername;

                                            System.out.println(currentLocation);

                                            message = "<br/> <span style='color:green;'>Welcome</span> " + tempUsername;
                                        } else {
                                            postData2 = "";
                                            serververfication = false;
                                            superCommand_execution_step = 0;
                                            superCommand_execution = "";
                                            builtInCommandsAttrubutesList.set(0, "");
                                            builtInCommandsAttrubutesList.set(1, "");
                                            superCommand_execution = "";
                                            message = "<br/>  <span style='color:red;'>Wrong Username or Password.</span>";
                                        }
                                    }

                                }
                            }
                        }
                        postData += message;
                    }

                }

                out.println("HTTP/1.0 200 OK");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println("Server: MINISERVER");
                // this blank line signals the end of the headers
                out.println("");
                // Send the HTML page               
                out.println("<H1>Systems Software - Echo server</H1>");
                out.println("<H2>Example of a echo server</H2>");
                // out.println("<p style=\"margin-left:5px\">GET->" + auxLine + "</H1>");
                
                System.out.println(postData);
                
                
                if(tempFinalCommand.equals("help"))
                    {
                        out.println("<p style=\"margin-left:5px\" >Post->" + postData + "</p>");
                        tempFinalCommand = "";
                    }
                else
                    {
                        if (!((serververfication == true) && (superCommand_execution.equals("super_adduser")))) {
                            if (postData.contains("Welcome")) {
                                String[] postDataParts = postData.split("<span ");

                                out.println("<p style=\"margin-left:5px\" >Post-><span " + postDataParts[1] + "</p>");
                            } else {
                                out.println("<p style=\"margin-left:5px\" >Post->" + postData + "</p>");
                            }
                        }
                    }
                

                out.println("<p style=\"margin-left:5px\" >" + postData2 + "</p>");
                out.println("<form name=\"input\" action=\"imback\" method=\"post\">");

                //Get current usernam
                currentUsername = System.getProperty("user.name");

                /*Following are the lines used for giving the user the input text box which he can use to enter his commands.
                The 'input' HTML method is used, in which we made the followin important additions.
                1) We used the autofocus=on feature just to always put the cursor of the page into the text box by default so that the user does not have to use 
                his mouse to click in the text box.
                2) When the user has issued the login command, and he is in the step in which he is asked to put his password, we used the type=password feature
                just to make sure that dots are shown instead of the user-written password.*/
                if (((serververfication == false) && (superCommand_execution.equals("logincommand")) && (superCommand_execution_step == 2)) || ((serververfication == true) && (superCommand_execution.equals("super_adduser")) && (superCommand_execution_step == 2 || superCommand_execution_step == 3))) {
                    out.println("<span style='color: red;'>" + currentUsername + "</span>" + ":~" + currentLocation + "$ &nbsp<input autofocus='on' type=\"password\" name=\"user\"><input type=\"submit\" value=\"Submit\"></form>");
                } else {
                    out.println("<span style='color: red;'>" + currentUsername + "</span>" + ":~" + currentLocation + "$ &nbsp<input autofocus='on' type=\"text\" name=\"user\"><input type=\"submit\" value=\"Submit\"></form>");
                }
                message = "";

                out.close();
                socket.close();
            }
            server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}