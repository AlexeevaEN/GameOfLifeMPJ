package ru.vlsu.isse.parprog.gol;

import java.io.File;
import java.io.IOException;
import mpi.*;
import ru.vlsu.isse.parprog.gol.model.ClosedField;
import ru.vlsu.isse.parprog.gol.model.Field;

public class Game {

    private Field field;
    private Field buffer;
    private int fieldSize;
    private int me;
    private int size;
    private int preNeigh;
    private int pastNeigh;

    public Game(String path, String[] args) throws IOException {     
        MPI.Init(args);
        this.me = MPI.COMM_WORLD.Rank();
        this.size = MPI.COMM_WORLD.Size();

        this.preNeigh = me - 1 >= 0 ? me - 1 : size - 1;
        this.pastNeigh = me + 1 < size ? me + 1 : 0;
        
        ClosedField part = new ClosedField(Field.loadFromFileWithBorder(new File(path), me, size));
        this.field = part;
        this.buffer = new ClosedField(field);
        fieldSize = field.size();       
    }

    public Field getField() {
        return field;
    }

    public void update(int numberOfEpochs) {
        for (int i = 0; i < numberOfEpochs; i++) {
            update();
            if (me % 2 != 0) {          
                sendLines(field, preNeigh, pastNeigh);
                reciveLines(field, preNeigh, pastNeigh);
            } else { 
                reciveLines(field, preNeigh, pastNeigh);
                sendLines(field, preNeigh, pastNeigh);
            }
        }
    }

    private void update() {
        for (int position = field.width(); position < fieldSize - field.width(); position++) {
            int x = position / field.width();
            int y = position - field.width() * x;

            int neighbours = (field.isAlive(x - 1, y - 1) ? 1 : 0)
                    + (field.isAlive(x - 1, y + 0) ? 1 : 0)
                    + (field.isAlive(x - 1, y + 1) ? 1 : 0)
                    + (field.isAlive(x + 0, y - 1) ? 1 : 0)
                    + (field.isAlive(x + 0, y + 1) ? 1 : 0)
                    + (field.isAlive(x + 1, y - 1) ? 1 : 0)
                    + (field.isAlive(x + 1, y + 0) ? 1 : 0)
                    + (field.isAlive(x + 1, y + 1) ? 1 : 0);

            if (neighbours == 3) {
                buffer.set(x, y, true);
            } else if (neighbours < 2 || neighbours > 3) {
                buffer.set(x, y, false);
            } else {
                buffer.set(x, y, field.isAlive(x, y));
            }
        }
        Field temp = field;
        field = buffer;
        buffer = temp;
    }

    private void reciveLines(Field cf, int preNeigh, int pastNeigh) {
        byte[] row = new byte[cf.width() / 8];
        MPI.COMM_WORLD.Recv(row, 0, row.length, MPI.BYTE, pastNeigh, 0);
        cf.setRow(row, cf.height() - 1);

        MPI.COMM_WORLD.Recv(row, 0, row.length, MPI.BYTE, preNeigh, 1);
        cf.setRow(row, 0);
    }

    private void sendLines(Field cf, int preNeigh, int pastNeigh) {
        byte[] row = cf.getRow(1);
        MPI.COMM_WORLD.Send(row, 0, row.length, MPI.BYTE, preNeigh, 0);

        row = cf.getRow(cf.height() - 2);
        MPI.COMM_WORLD.Send(row, 0, row.length, MPI.BYTE, pastNeigh, 1);
    }
    
    public void saveField(String path) throws IOException{
        File outputFile = new File(path);
        if (me == 0) {
            field.saveHeader(outputFile, size);
        }      
        field.saveToFile(outputFile, 1, field.height(), me);
    }
    
    public void finalizeMPI(){
        MPI.Finalize();
    }
}
