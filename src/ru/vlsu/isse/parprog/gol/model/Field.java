package ru.vlsu.isse.parprog.gol.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Arrays;

public class Field {

    private static final int overlap = 1;
    private static final int headerSize = Integer.SIZE * 2 / 8;
    private static final char EMPTY_CELL = ' ';
    private static final char ALIVE_CELL = '*';

    private int width;
    private int height;
    private long size;
    protected byte[] cells;

    public Field(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }

        size = width * height;

        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("field size is too big");
        }

        if (size % 8 != 0) {
            throw new IllegalArgumentException("field size must be a multiple of 8");
        }

        this.width = width;
        this.height = height;
        this.cells = new byte[(int) (size / 8)];
    }

    protected Field(Field field) {
        this.width = field.width;
        this.height = field.height;
        this.size = field.size;
        this.cells = new byte[(int) (size / 8)];
        System.arraycopy(field.cells, 0, cells, 0, cells.length);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return (int) size;
    }

    public byte[] getRow(int index) {
        byte[] row = new byte[width / 8];
        System.arraycopy(cells, index * width / 8, row, 0, row.length);
        return row;
    }

    public void setRow(byte[] row, int index) {
        System.arraycopy(row, 0, cells, width * index / 8, row.length);
    }

    public void set(int x, int y, final boolean alive) {
        checkRange(x, y);
        final int position = width * x + y;
        final int index = position / 8;
        final int mask = (alive ? 0x80 : 0xFF7F) >> position % 8;
        cells[index] = alive
                ? (byte) (cells[index] | mask)
                : (byte) (cells[index] & mask);
    }

    public void init(String vector) {
        if (size() != vector.length()) {
            throw new IllegalArgumentException(
                    MessageFormat.format(
                            "Size of initial vector ({0}) should be equal to field size ({1})",
                            vector.length(), cells.length));
        }

        int index = 0;

        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                char ch = vector.charAt(index);

                if (ch == EMPTY_CELL) {
                    set(x, y, false);
                } else if (ch == ALIVE_CELL) {
                    set(x, y, true);
                } else {
                    throw new IllegalArgumentException("Illegal character at index "
                            + index + " (" + ch + "), could be one of: '"
                            + ALIVE_CELL + "' (alive) or '" + EMPTY_CELL + "' (empty)");
                }
                index++;
            }
        }
    }

    public String toInitVector() {
        StringBuilder builder = new StringBuilder();
        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                builder.append(isAlive(x, y) ? ALIVE_CELL : EMPTY_CELL);
            }
        }
        return builder.toString();
    }

    public boolean isAlive(int x, int y) {
        checkRange(x, y);
        final int position = width * x + y;
        final int index = position / 8;
        final int mask = (0x80) >> position % 8;
        return (cells[index] & mask) == mask;
    }

    private void checkRange(int x, int y) {
        if (x < 0 || x >= height) {
            throw new IllegalArgumentException("x is out of range: " + x);
        }
        if (y < 0 || y >= width) {
            throw new IllegalArgumentException("y is out of range: " + y);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                builder.append(isAlive(x, y) ? ALIVE_CELL : EMPTY_CELL);
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public void saveToFile(File target) throws IOException {
        OutputStream output = new FileOutputStream(target);
        try {
            writeInt(output, width);
            writeInt(output, height);
            output.write(cells);
        } finally {
            output.close();
        }
    }

    public void saveToFile(File target, int firstLine, int lastLine, int part) throws IOException {
        int bytesInLine = width / 8;

        RandomAccessFile output = new RandomAccessFile(target, "rw");
        int startPosition = (height - overlap * 2) * part * bytesInLine + headerSize;
        try {
            output.seek(startPosition);
            output.write(cells, firstLine * bytesInLine, (lastLine - firstLine - overlap) * bytesInLine);
        } finally {
            output.close();
        }
    }

    public void saveHeader(File target, int multipl) throws IOException {
        OutputStream output = new FileOutputStream(target);
        try {
            writeInt(output, width);
            writeInt(output, (height - overlap * 2) * multipl);
        } finally {
            output.close();
        }
    }

    public static Field loadFromFileWithBorder(File source, int neededPart, int totalPart) throws IOException {
        RandomAccessFile input;
        input = new RandomAccessFile(source, "r");
        try {
            int width = readInt(input);
            int height = readInt(input);
            int partLength = height / totalPart;
            if (neededPart == totalPart - 1) {
                partLength = height - (partLength * (totalPart - 1));
            }
            Field field = new Field(width, partLength + overlap * 2);

            int bytesInLine = width / 8;
            int offset = partLength * neededPart - overlap;
            int readFromStart = 0;
            if (offset < 0) {
                input.seek(input.length() + offset * bytesInLine);
                input.read(field.cells, 0, -offset * bytesInLine);
                readFromStart = -offset;
                offset = 0;
            }
            int length = partLength + overlap * 2 - readFromStart;
            if (offset + length > height) {
                int endOverlap = offset + length - height; 
                input.seek(headerSize);
                input.read(field.cells, (partLength - endOverlap + overlap + 1) * bytesInLine, endOverlap * bytesInLine);
                length = length - endOverlap;
            }

            input.seek((offset) * bytesInLine + headerSize);
            input.read(field.cells, readFromStart * bytesInLine, length * bytesInLine);

            return field;
        } finally {
            input.close();
        }
    }

    private void writeInt(OutputStream output, int value) throws IOException {
        for (int i = 0; i < 4; i++) {
            output.write(value);
            value >>>= 8;
        }
    }

    public static Field loadFromFile(File source) throws IOException {
        InputStream input = new FileInputStream(source);
        try {
            int width = readInt(input);
            int height = readInt(input);
            Field field = new Field(width, height);
            input.read(field.cells);
            return field;
        } finally {
            input.close();
        }
    }

    public static Field loadFromFile(File source, int part, int totalPart) throws IOException {
        InputStream input = new FileInputStream(source);
        try {
            int width = readInt(input);
            int height = readInt(input);

            int partLength = height / totalPart;
            if (part == totalPart - 1) {
                partLength = height - (partLength * (totalPart - 1));
            }
            Field field = new Field(width, partLength);

            int offset = partLength * part * width / 8;
            int length = partLength * width / 8;
            input.skip(offset);
            input.read(field.cells, 0, length);
            return field;
        } finally {
            input.close();
        }
    }

    private static int readInt(RandomAccessFile input) throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int nextByte = input.read();
            nextByte <<= i * 8;
            value |= nextByte;
        }
        return value;
    }

    private static int readInt(InputStream input) throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int nextByte = input.read();
            nextByte <<= i * 8;
            value |= nextByte;
        }
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Field)) {
            return false;
        }
        Field field = (Field) obj;
        return width == field.width
                && height == field.height
                && Arrays.equals(cells, field.cells);
    }
}
