package ru.vlsu.ispi.parallel.mpigol;

import java.io.IOException;
import ru.vlsu.isse.parprog.gol.Game;

public class MpjGameOfLife {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        if (args.length <= 3 ){
            String[] temp = new String[6];
            for (int i=0; i<3; i++) {
                temp[i] = args[i];
            }
            temp[3]= "G:\\parallel\\dmitrygusev-game-of-life-contest-52ce37a2e976\\src\\in128.dat"; 
            temp[4] = "outp.dat";   
            temp[5] = "100";        
            args = temp;
        }

        Game game = new Game(args[3], args);          
        long time = System.currentTimeMillis();
        game.update(Integer.parseInt(args[5]));
        
        System.out.println("Total time: " + (System.currentTimeMillis() - time));

        game.saveField(args[4]); 
        game.finalizeMPI();
    }
}
