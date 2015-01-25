package ru.vlsu.isse.parprog.gol.model;

public class ClosedField extends Field
{
    public ClosedField(int width, int height)
    {
        super(width, height);
    }

    public ClosedField(Field field)
    {
        super(field);
    }
    
    @Override
    public boolean isAlive(int x, int y)
    {
        return super.isAlive(mod(x, height()), mod(y, width()));
    }

    @Override
    public void set(int x, int y, boolean alive)
    {
        super.set(mod(x, height()), mod(y, width()), alive);
    }
    
    private int mod(int n, int length)
    {
        if (n < 0)
        {
            n = n % length == 0
              ? 0
              : length + n % length;
        }
        else if (n >= length)
        {
            n = n % length;
        }
        return n;
    }
}
