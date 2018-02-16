/*
Developer: Hung Pham
 */

public class JuliaSet implements Julia{

    public int id; // task id

    public int maxIterations;
    public int columns;

    public int[] results;

    public int rows;

    public void compute() {
        results = new int[columns];
        double newRe, newIm, oldRe, oldIm;
        for (int i = 0; i < columns; i++) {
            int count = 0;
            newRe = 1.5 * (i - columns / 2) / (0.5 * ZOOM * columns);
            newIm = (id - rows / 2) / (0.5 * ZOOM * rows);
            while (count < maxIterations && (newRe * newRe + newIm * newIm) < 4) {
                count++;
                oldRe = newRe;
                oldIm = newIm;
                newRe = oldRe * oldRe - oldIm * oldIm + C_REAL;
                newIm = 2 * oldRe * oldIm + C_IMAGINATION;
            }
            results[i] = count;
        }
    }

}
