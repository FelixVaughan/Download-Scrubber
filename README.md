Script that periodically checks Downloads folder and clears out files to pre-specified directories. The moved files log is stored in a file in the home directory.
To set up this application follow the steps bellow.
1. In a terminal, navigate to the src folder
2. Run: javac *.java
3. Run: jar cvfe DScrub.jar DownloadScrubber *.class
4. Copy the jar file to your root directory (or any directory of your choice)
5. Create a batch file with the following contents: start javaw -jar "/path/to/jar/file"
6. Move this batch file to the startup folder. 
7. Restart.

