package utilities;

import java.nio.ByteBuffer;
import java.util.Arrays;

import fingerprinting.AudioParams;

public class Spectrum {

    // Compute the magnitude spectrum from the recorded audio (time domain)
    public static double[][] compute(byte[] audioTimeDomain) {

        final int totalSize = audioTimeDomain.length;
        
        // The number of chunks/ventanas in the recorded audio
        int chunks = totalSize / AudioParams.chunkSize;

        // We will use this bidimensional array as output variable to return the magnitude spectrum
        double[][] resultsMag = new double[chunks][];
  
                Complex[][] resultsComplex = new Complex[chunks][];
                
                for (int i = 0; i < chunks; i++) {                    
                    Complex[] complex = new Complex[AudioParams.chunkSize];
                    
                    /**********************PARTE IMPLEMENTADA*****************************/
                    
                    //Recorremos cada una de las ventanas y Realiza la FFT para el fragmento actual (FFT.fft (complejo)), 
                    //Recorre desde 0 hasta menos que 4096 (Que es el tamaño de la ventana).  
                    //Se calcula como el Indice de la ventana por el tamaño de la ventana, mas el indice en el que estamos
                    for (int j = 0; j < AudioParams.chunkSize; j++) { //para saber en que parte del audioTimeDomain nos tenemos que mover
                    	complex[j] = new Complex(audioTimeDomain[i*AudioParams.chunkSize+j], 0); //Relleno la parte real y un 0 en la parte imaginaria
                    }                               
                    
                    //Guardamos los resultados en resultsComplex [i] es un array con numeros complejos
                    //En la primera posicion metemos el array complex, del resultado de la transformada de furier para cada ventana
                    resultsComplex[i] = FFT.fft(complex);

                    /*********************FIN PARTE IMPLEMENTADA***************************/
                    
                    resultsMag[i]= new double[AudioParams.chunkSize];                       

                    for (int j = 0; j < AudioParams.chunkSize; j++) {
                        resultsMag[i][j] = Math.log(resultsComplex[i][j].abs() + 1);
                    }
                }                       
        return resultsMag;
    }
}
