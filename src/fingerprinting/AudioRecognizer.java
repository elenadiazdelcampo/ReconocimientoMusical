package fingerprinting;

import serialization.Serialization;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import utilities.HashingFunctions;
import utilities.Spectrum;

public class AudioRecognizer {
    
    // The main hashtable required in our interpretation of the algorithm to
    // store the song repository
    private Map<Long, List<KeyPoint>> hashMapSongRepository;
    
    // Variable to stop/start the listening loop
    public boolean running;

    // Constructor
    public AudioRecognizer() {
        
        // Deserialize the hash table hashMapSongRepository (our song repository)
        this.hashMapSongRepository = Serialization.deserializeHashMap();
        this.running = true;
    }

    // Method used to acquire audio from the microphone and to add/match a song fragment
    public void listening(String songId, boolean isMatching) throws LineUnavailableException {
        
        // Fill AudioFormat with the recording we want for settings
        AudioFormat audioFormat = new AudioFormat(AudioParams.sampleRate,
                AudioParams.sampleSizeInBits, AudioParams.channels,
                AudioParams.signed, AudioParams.bigEndian);
        
        // Required to get audio directly from the microphone and process it as an 
        // InputStream (using TargetDataLine) in another thread      
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();
        
        Thread listeningThread = new Thread(new Runnable() {
                        
            @Override
            public void run() {
                // Output stream 
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                // Reader buffer
                byte[] buffer = new byte[AudioParams.bufferSize];               
                int n = 0;
                try {
                    while (running) {
                        // Reading
                        int count = line.read(buffer, 0, buffer.length);
                        // If buffer is not empty
                        if (count > 0) {
                            outStream.write(buffer, 0, count);
                        }
                    }

                    byte[] audioTimeDomain = outStream.toByteArray();

                    // Compute magnitude spectrum
                    double [][] magnitudeSpectrum = Spectrum.compute(audioTimeDomain);                    
                    // Determine the shazam action (add or matching) and perform it
                    shazamAction(magnitudeSpectrum, songId, isMatching);                    
                    // Close stream
                    outStream.close();                    
                    // Serialize again the hashMapSongRepository (our song repository)
                    Serialization.serializeHashMap(hashMapSongRepository);                
                } catch (IOException e) {
                    System.err.println("I/O exception " + e);
                    System.exit(-1);
                }
            }
        });

        // Start listening
        listeningThread.start();
        
        System.out.println("Press ENTER key to stop listening...");
        try {
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(AudioRecognizer.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.running = false;               
    }   
    
    // Determine the shazam action (add or matching a song) and perform it 
    private void shazamAction(double[][] magnitudeSpectrum, String songId, boolean isMatching) {  
    	
		// Hash table used for matching (Map<songId, Map<offset,count>>)
        Map<String, Map<Integer,Integer>> matchMap = 
                new HashMap<String, Map<Integer,Integer>>(); 
    
        // Iterar sobre todos los fragmentos / ventanas del espectro de magnitud
        for (int c = 0; c < magnitudeSpectrum.length; c++) { 
		
        	/*************INICIO IMPLEMENTACION**********/
            long entryHash = computeHashEntry(magnitudeSpectrum[c]);  //Llamar a la función hash para obtener el long
            List<KeyPoint> keyPointList;
            
            /*************FIN IMPLEMENTACION*************/
            
            //En el caso de hacer coincidir un fragmento de canción, es decir en el caso de añadir una cancion nueva al repositorio
            if (!isMatching) {
            	
            	/**********INICIO IMPLEMENTACION*****************/
            	// Agregar un punto clave a la lista en su entrada de hash  que se ha calculado antes
                if((keyPointList = hashMapSongRepository.get(entryHash)) == null){ //si no hay ninguna entrada para el entero largo hay que añadir una
					
                	keyPointList = new ArrayList<KeyPoint>();
                	hashMapSongRepository.put(entryHash, keyPointList);
                } 
                
                /*************FIN IMPLEMENTACION******************/
				
                KeyPoint point = new KeyPoint(songId, c);
                keyPointList.add(point);  //En la lista de KeyPoint añado esa cancion          
            }
           
			// In the case of matching a song fragment
            else { //En el matching lo que tenemos que hacer es rellenar la tabla hash
                   /*Extraemos su huella digital acústica y la comparamos con las del repositorio, para identificar de qué canción se trata*/
            	
            	/**************** INICIO IMPLEMENTACION ***********************/
            	
            	// Iterar sobre la lista de puntos clave que coincide con la entrada hash en el fragmento actual
            	if ((keyPointList = hashMapSongRepository.get(entryHash)) != null){ //buscar el entero largo creado arriba
            		
            	   // Para cada punto clave:
                   for(KeyPoint point2 : keyPointList) { //si esta cogemos su lista de keypoints
					   
                	    //Calcular el desplazamiento de tiempo (Math.abs (point.getTimestamp () - c)) entre las ventanas del fragmento y las ventanas del repositorio
            			int offset = Math.abs(point2.getTimestamp() - c); //calculamos el offset, el trozito de la ventana original - el trozo que hemos grabado
            			Map<Integer, Integer> matchedSong = matchMap.get(point2.getSongId());
            			int counter = 0; //número de veces que se ha encontrado dicho offset como contenido
						
            			// Si todavía no se ha encontrado songId (extraído del punto clave actual) en el matchMap, agréguelo
            			if(matchedSong == null) {
            				matchedSong = new HashMap<Integer, Integer>(); /*creamos una nueva tabla  que tenga el valor del offset como clave y 
            																número de veces que se ha encontrado dicho  offset como contenido*/
            				matchedSong.put(offset, 1);
            				matchMap.put(point2.getSongId(), matchedSong);
            			} 
            			//Sino quiere decir que songId se ha agregado en un fragmento anterior
						else{
							
            				if (matchedSong.get(offset)!= null) {
								
            					counter=matchedSong.get(offset);
            					matchedSong.put(offset, counter + 1);	/*le sumamos uno al counter para indicar que ese offset ha aparecido una vez mas*/							
            				}
							else{
            					matchedSong.put(offset, 1); //si el offset esta vacio lo añadimos como si fuese la primera vez que ha aparecido
            				}
            			}
            			if (counter >1){ //Mientras que el offset haya aparecido una vez lo imprimimos por pantalla
            				System.out.println("Offset: " + offset + ", counter:" + counter );
            			}
            		}
            	} 
            	/************************** FIN IMPLEMENTACION ********************/
            	
            }    // End iterating over the chunks/ventanas of the magnitude spectrum        
        }
        if (isMatching) { //Si es la opcion de matching llamamos al metodo de mostrar la mejor coincidencia de cancion
           showBestMatching(matchMap);
        }
    }
    
    // Find out in which range the frequency is
    private int getIndex(int freq) {
       
        int i = 0;
        while (AudioParams.range[i] < freq) {
            i++;
        }
        return i;
    }  
    
    // Compute hash entry for the chunk/ventana spectra 
    private long computeHashEntry(double[] chunk) {
                
        // Variables to determine the hash entry for this chunk/window spectra
        double highscores[] = new double[AudioParams.range.length];
        int frequencyPoints[] = new int[AudioParams.range.length];
       
        for (int freq = AudioParams.lowerLimit; freq < AudioParams.unpperLimit - 1; freq++) {
            // Get the magnitude
            double mag = chunk[freq];
            // Find out which range we are in
            int index = getIndex(freq);
            // Save the highest magnitude and corresponding frequency:
            if (mag > highscores[index]) {
                highscores[index] = mag;
                frequencyPoints[index] = freq;
            }
        }        
        // Hash function 
        return HashingFunctions.hash1(frequencyPoints[0], frequencyPoints[1], 
                frequencyPoints[2],frequencyPoints[3],AudioParams.fuzzFactor);
    }
    
    // Method to find the songId with the most frequently/repeated time offset | (Map<songId, Map<offset,count>>)
    private void showBestMatching(Map<String, Map<Integer,Integer>> matchMap) {
    	
    	/***************** INICIO IMPLEMENTACIÓN **************/
       
    	/*Se itera sobre la tabla hash durante el matching donde el identificador de canción es el índice o clave de la tabla y el contenido es
    	una nueva tabla hash anidada que tenga el valor del offset como íncide/clave y el número de veces que se ha encontrado dicho offset*/
    	/*Por tanto, itero sobre las canciones en la tabla hash utilizada para hacer coincidir las canciones almacenadas con la que estamos escuchando (matchMap)
    	y para cada canción itero sobre la tabla hash anidada Map <offset, count>*/
    	int freq;
    	int Maxfreq=0;
    	String selectedSong = null;
    	for(Map.Entry<String, Map<Integer, Integer>>  songs : matchMap.entrySet()) {
    		for(Entry<Integer, Integer> songData : songs.getValue().entrySet()) { //Obtenemos el valor de los elementos del map que son de ese tipo
    		/*Obtengo el mayor desplazamiento para la canción actual y actualizo, si es necesario,
    	   el mejor resultado general encontrado hasta la iteración actual*/
    			freq = songData.getValue();
    			if(freq >= Maxfreq) {
    				Maxfreq = freq;
    				selectedSong = songs.getKey();
    			}
    		}
    	}
        /***************** FIN IMPLEMENTACIÓN ****************/
    	
        // Print the songId string which represents the best matching     
        System.out.println("Best song: "+selectedSong);
    }
}