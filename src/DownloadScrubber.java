import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class Downloadable extends File{

    private final String home = System.getProperty("user.home");
    private ArrayList<String> destinations = new ArrayList<>();
    private String[][] possibleDestinations = {
            {"txt","doc","docx","odt","pdf","xls","xlsx","ods","ppt","pptx"}, //documents
            {"jpg","png","gif","webp","tiff","psd","raw","svg","jpeg","pdf"}, //pictures
            {"wav","vox","raw","mp3","mpc","m4a","mp2"},                      //music
            {"mpeg","ogg","mp4","m4p","m4v","wmv","mov","mkv","gifv"},        //videos
            //if none of these, send to desktop import folder
    };

    public String suffix(){
        String file = this.getName();
        int dotIndex = file.lastIndexOf('.');
        return file.substring(dotIndex + 1);
    }

    public String[][] getpossibleDestinations(){
        return possibleDestinations;
    }

    public ArrayList<String> getdestinations(){
        return destinations;
    }

    private void setdestinations(){
        for(int x = 0; x < possibleDestinations.length; x++){
            if(Arrays.asList(possibleDestinations[x]).contains(suffix())){
                if(x == 0) destinations.add(home+"/Documents/ImportedFromDownloads");
                if(x == 1) destinations.add(home+"/Pictures/ImportedFromDownloads");
                if(x == 2) destinations.add(home+"/Music/ImportedFromDownloads");
                if(x == 3) destinations.add(home+"/Videos/ImportedFromDownloads");
            }
            if(destinations.size() == 0) destinations.add(home+"/Desktop/ImportedFromDownloads");
            /* means no predefined destinations. Send to default desktop import folder */

        }
    }

    public Downloadable(String file){
        super(file);
        setdestinations();
    }

}

class FileTransferThread implements Runnable{
    Downloadable downloadable;
    boolean moved = true;
    public FileTransferThread(Downloadable file){
        downloadable = file;
    }

    public void writeLog(String log){
        try {
            Files.write(Paths.get(System.getProperty("user.home")+"/.download_transfer_logs"),
                    log.getBytes(),
                    StandardOpenOption.APPEND);
        }catch (IOException e) { }
    }

    public void run() {
        for(String path : downloadable.getdestinations()){
            String destinationPath = path+"/"+downloadable.getName();
            System.out.println(downloadable.getPath());
            try {
                Files.copy(downloadable.toPath(), Paths.get(destinationPath));

            }catch (FileAlreadyExistsException e) {
                destinationPath = path+"/_"+downloadable.getName();
                try{Files.copy(downloadable.toPath(), Paths.get(destinationPath));}
                catch(IOException e1){
                    System.out.printf(e.toString());
                    moved = false;
                    continue;
                }
            }catch(FileNotFoundException e){
                System.out.printf("'%s' not found. Moving on...\n",downloadable.getName());
                moved = false;
                continue;
            }catch(IOException e){
                System.out.println(e.toString());
                System.out.printf("IOException occured on file '%s'. Moving on...\n",downloadable.getName());
                moved = false;
                continue;
            }
            writeLog(String.format("moved '%s' TO %s AT %s \n",downloadable.getName(),path,new Date()));
        }
        if(moved) {
            downloadable.delete();
        }
        System.out.printf("Moved %s\n",downloadable.getName());
    }
}

class DaemonProcess implements Runnable{
    public void run() {
        String home = System.getProperty("user.home");
        String downloads = home+"/Downloads";
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        File [] file = new File(downloads).listFiles();
        if(file.length > 0) {
            for (int x = 0; x < file.length; x++) {
                Downloadable downloadable = new Downloadable(downloads+"/"+file[x].getName());
                Runnable runnable = new FileTransferThread(downloadable);
                executorService.submit(runnable);
            }
        }

    }
}
public class DownloadScrubber{

    /*checks if needed directories exists and creates them if not*/
    public static void initializeFiles(){
        String home = System.getProperty("user.home");
        String[] mainDirs = {"Videos", "Desktop","Pictures","Documents","Music"};
        for (String mainDir : mainDirs) {
            String dirToCheck = String.format("%s/%s/ImportedFromDownloads", home, mainDir);
            if (!(new File(dirToCheck).exists()))
                new File(dirToCheck).mkdir();
        }

        File file = new File(home+"/.download_transfer_logs");
        try { file.createNewFile(); } catch (IOException e) {
            System.out.println("Warning: log file could not be created! Continuing regardless.");
        }
    }

    public static void main(String[] args) {
        initializeFiles();
        ScheduledExecutorService daemon = Executors.newSingleThreadScheduledExecutor();
        daemon.scheduleAtFixedRate(new DaemonProcess(),0,18,TimeUnit.HOURS);
    }
}
