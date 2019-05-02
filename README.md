# NP2019FinalProject

Authors:
Stephen R
Tomas S

### Install & Run:

### IntelliJ

#Step 1:
Launch IntelliJ and create a new project from version control and git.
![Step 1](http://prntscr.com/nedu4v)

#Step 2:
Copy HTTPS clone link from GitHub.
![Step 2](http://prntscr.com/nedum3)

#Step 3:
Paste the link into URL: textbox and press clone button.
![Step 3](http://prntscr.com/nedutw)

#Step 4:
Go to project structure settings.
![Step 4](http://prntscr.com/nedv56)

#Step 5:
Select Java SDK version 11 or above. Then choose the directory of the compiler files. It's suggested to name the folder "out" and place it under the project folder. 
 ![Step 5a](http://prntscr.com/nedvhd)
 ![Step 5b](http://prntscr.com/nedznc)

#Step 6:
Under the Modules tab, select the src folder and mark the folder as "Sources".
 ![Step 6](http://prntscr.com/nedvr8)

#Step 7:
Under the Dependencies category of Modules, click on the + sign, and then onto "New Library", then "Java".
 ![Step 7](http://prntscr.com/nedwow)

#Step 8:
From there navigate Internal_Libraries -> javafx-sdk-11.0.2 -> lib and select all the avaliable .jar files. Then press "OK".
 ![Step 8](http://prntscr.com/nedx2h)

#Step 9:
Press "OK" again.
 ![Step 9](http://prntscr.com/nedxgt)

#Step 10:
Select the new Java library and press "Add Selected". Then Apply the changes and exit project structure.
 ![Step 10](http://prntscr.com/nedxob)

#Step 11:
In the top right corner press "Add Configurations".
 ![Step 11](http://prntscr.com/nedxx8)

#Step 12:
Press the + button and select Application. Then fill in the information below, for "Main-Class" enter:

com.rossisurna.launcher.application.Main 

then under the VM Options enter: 

--module-path "..\NP2019FinalProject\Internal_Libraries\javafx-sdk-11.0.2\lib" --add-modules=javafx.controls,javafx.fxml

lastly, press "OK"

 ![Step 12](http://prntscr.com/nedyc3)

#Step 13:
Press the green run button.
 ![Step 13](http://prntscr.com/nedz49)

#Step 14:
On launch, the controller UI will allow you to stop or launch the server, and or launch clients. Once here you are able to connect to available groups or create your own. Once in a group, you can send your friends the URL and server IP address so they can join too!
 ![Step 14](http://prntscr.com/nee06e)
