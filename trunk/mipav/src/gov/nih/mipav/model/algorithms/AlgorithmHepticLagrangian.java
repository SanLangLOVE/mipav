package gov.nih.mipav.model.algorithms;


/**
 * This is a polynomial p of degree 7 which interpolates a given function f at the points x(-3), x(-2), x(-1), x(0),
 * x(1), x(2), x(3), and x(4) and is given by p(x) = sum from i = -3 to i = 4 of li(x)*f(xi), where li, i =
 * -3,-2,-1,0,1,2,3,4 which are called the fundamental polynomials, are given by li(x) = product from k = -3 to k = 4
 * for k != i of (x - xk)/(xi - xk). li(-3) = (x - -2)/(-3 - -2) * (x - -1)/(-3 - -1) * (x - 0)/(-3 - 0) * (x-1)/(-3 -
 * 1) * (x - 2)/(-3 - 2) * (x - 3)/(-3 - 3) * (x - 4)/(-3 - 4) = (x * (x*x - 1) * (x*x - 4) * (x - 3) * (4 - x))/5040
 * li(-2) = (x - -3)/(-2 - -3) * (x - -1)/(-2 - -1) * (x - 0)/(-2 - 0) * (x-1)/(-2 - 1) * (x-2)/(-2 - 2) * (x-3)/(-2 -
 * 3) * (x - 4)/(-2 - 4) = (x * (x*x - 1) * (x - 2) * (x*x - 9) * (x - 4))/720 li(-1) = (x - -3)/(-1 - -3) * (x -
 * -2)/(-1 - -2) * (x - 0)/(-1 - 0)* (x-1)/(-1 - 1) * (x - 2)/(-1 - 2) * (x - 3)/(-1 - 3) * (x - 4)/(-1 - 4) = (x * (x -
 * 1) * (x*x- 4) * (x*x - 9) * (4 - x)))/240 li(0) = (x - -3)/(0 - -3) * (x - -2)/(0 - -2) * (x - -1)/(0 - -1) * (x -
 * 1)/(0 - 1) * (x - 2)/(0 - 2) * (x - 3)/(0 - 3) * (x - 4)/(0 - 4) = ((x*x - 1) * (x*x - 4) * (x*x - 9) * (x - 4))/144
 * li(1) = (x - -3)/(1 - -3) * (x - -2)/(1 - -2) * (x - -1)/(1 - -1) * (x - 0)/(1 - 0) * (x - 2)/(1 - 2) * (x - 3)/(1 -
 * 3) * (x - 4)/(1 - 4) = (x * (x+1) * (x*x - 4) * (x*x - 9) * (4 - x))/144 li(2) = (x - -3)/(2 - -3) * (x - -2)/(2 -
 * -2) * (x - -1)/(2 - -1) * (x - 0)/(2 - 0) * (x - 1)/(2 - 1) * (x - 3)/(2 - 3) * (x - 4)/(2 - 4) = (x * (x*x - 1) *
 * (x+2) * (x*x - 9) * (x - 4))/240 li(3) = (x - -3)/(3 - -3) *(x - -2)/(3 - -2) * (x - -1)/(3 - -1) * (x - 0)/(3 - 0) *
 * (x - 1)/(3 - 1) * (x - 2)/(3 - 2) * (x - 4)/(3 - 4) = (x * (x*x - 1) * (x*x - 4) * (x + 3) * (4 - x))/720 li(4) = (x
 * - -3)/(4 - -3) * (x - -2)/(4 - -2) * (x - -1)/(4 - -1) * (x - 0)/(4 - 0) * (x - 1)/(4 - 1) * (x - 2)/(4 - 2) * (x -
 * 3)/(4 - 3) = (x * (x*x - 1) * (x*x - 4) * (x*x - 9))/5040
 */

public class AlgorithmHepticLagrangian extends AlgorithmBase {

    //~ Instance fields ------------------------------------------------------------------------------------------------

    /** DOCUMENT ME! */
    private double alphaMax = 255.0;

    /** DOCUMENT ME! */
    private double alphaMin = 0.0;

    /** DOCUMENT ME! */
    private double blueMax = 255.0;

    /** DOCUMENT ME! */
    private double blueMin = 0.0;

    /** DOCUMENT ME! */
    private boolean clip;

    /** DOCUMENT ME! */
    private double greenMax = 255.0;

    /** DOCUMENT ME! */
    private double greenMin = 0.0;

    /** DOCUMENT ME! */
    private double inputMax;

    /** DOCUMENT ME! */
    private double inputMin;

    /** DOCUMENT ME! */
    private double redMax = 255.0;

    /** DOCUMENT ME! */
    private double redMin = 0.0;

    /** DOCUMENT ME! */
    private int sliceSize;

    /** DOCUMENT ME! */
    private double[] volume = null;

    /** DOCUMENT ME! */
    private double[][] wt = null;

    /** DOCUMENT ME! */
    private int xD, yD, zD;

    /** DOCUMENT ME! */
    private int xdim, ydim, zdim;

    //~ Constructors ---------------------------------------------------------------------------------------------------

    /**
     * AlgorithmHepticLagrangian - default constructor.
     */
    public AlgorithmHepticLagrangian() { }

    //~ Methods --------------------------------------------------------------------------------------------------------

    /**
     * finalize -
     */
    public void finalize() {
        volume = null;
        wt = null;
        super.finalize();
    }

    /**
     * 2D heptic Lagrangian function.
     *
     * @param   x  float point index
     * @param   y  float point index
     *
     * @return  the heptic Lagrangian interpolated data point
     */
    public double hepticLagrangian2D(float x, float y) {

        int xbase, ybase;
        int j0, j1;
        int l0, l1;
        int ix, iy;
        double diffX, diffY;
        double sum;
        int indexX, indexY;
        double ySum;

        xbase = (int) x;
        ybase = (int) y;
        diffX = x - xbase;
        diffY = y - ybase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);

        sum = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                ySum += wt[iy][indexY] * volume[(l1 * xdim) + l0];
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++

            sum += wt[ix][indexX] * ySum;
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        if (clip) {
            sum = Math.max(Math.min(sum, inputMax), inputMin);
        }

        return sum;
    }
    
    /**
     * 2D heptic Lagrangian function.
     *
     * @param   x  double point index
     * @param   y  double point index
     *
     * @return  the heptic Lagrangian interpolated data point
     */
    public double hepticLagrangian2D(double x, double y) {

        int xbase, ybase;
        int j0, j1;
        int l0, l1;
        int ix, iy;
        double diffX, diffY;
        double sum;
        int indexX, indexY;
        double ySum;

        xbase = (int) x;
        ybase = (int) y;
        diffX = x - xbase;
        diffY = y - ybase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);

        sum = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                ySum += wt[iy][indexY] * volume[(l1 * xdim) + l0];
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++

            sum += wt[ix][indexX] * ySum;
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        if (clip) {
            sum = Math.max(Math.min(sum, inputMax), inputMin);
        }

        return sum;
    }

    /**
     * 2D heptic Lagrangian function for color.
     *
     * @param   x  float point index
     * @param   y  float point index
     *
     * @return  the heptic Lagrangian interpolated data point
     */
    public double[] hepticLagrangian2DC(float x, float y) {

        int xbase, ybase;
        int j0, j1;
        int l0, l1;
        int ix, iy;
        double diffX, diffY;
        double[] ySum = new double[4];
        double[] sum = new double[4];
        int offset;
        int indexX, indexY;

        xbase = (int) x;
        ybase = (int) y;
        diffX = x - xbase;
        diffY = y - ybase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);

        sum[0] = 0.0;
        sum[1] = 0.0;
        sum[2] = 0.0;
        sum[3] = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum[0] = 0.0;
            ySum[1] = 0.0;
            ySum[2] = 0.0;
            ySum[3] = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                offset = 4 * ((l1 * xdim) + l0);
                ySum[0] += wt[iy][indexY] * volume[offset];
                ySum[1] += wt[iy][indexY] * volume[offset + 1];
                ySum[2] += wt[iy][indexY] * volume[offset + 2];
                ySum[3] += wt[iy][indexY] * volume[offset + 3];
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++

            sum[0] += wt[ix][indexX] * ySum[0];
            sum[1] += wt[ix][indexX] * ySum[1];
            sum[2] += wt[ix][indexX] * ySum[2];
            sum[3] += wt[ix][indexX] * ySum[3];
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        // Since color is usually stored as ARGB with values limited to ranges from 0 to
        // 255, clamp the values between 0 and 255 if clip is false or restrict further if
        // clip is true
        sum[0] = Math.max(Math.min(sum[0], alphaMax), alphaMin);
        sum[1] = Math.max(Math.min(sum[1], redMax), redMin);
        sum[2] = Math.max(Math.min(sum[2], greenMax), greenMin);
        sum[3] = Math.max(Math.min(sum[3], blueMax), blueMin);

        return sum;
    }
    
    /**
     * 2D heptic Lagrangian function for color.
     *
     * @param   x  double point index
     * @param   y  double point index
     *
     * @return  the heptic Lagrangian interpolated data point
     */
    public double[] hepticLagrangian2DC(double x, double y) {

        int xbase, ybase;
        int j0, j1;
        int l0, l1;
        int ix, iy;
        double diffX, diffY;
        double[] ySum = new double[4];
        double[] sum = new double[4];
        int offset;
        int indexX, indexY;

        xbase = (int) x;
        ybase = (int) y;
        diffX = x - xbase;
        diffY = y - ybase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);

        sum[0] = 0.0;
        sum[1] = 0.0;
        sum[2] = 0.0;
        sum[3] = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum[0] = 0.0;
            ySum[1] = 0.0;
            ySum[2] = 0.0;
            ySum[3] = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                offset = 4 * ((l1 * xdim) + l0);
                ySum[0] += wt[iy][indexY] * volume[offset];
                ySum[1] += wt[iy][indexY] * volume[offset + 1];
                ySum[2] += wt[iy][indexY] * volume[offset + 2];
                ySum[3] += wt[iy][indexY] * volume[offset + 3];
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++

            sum[0] += wt[ix][indexX] * ySum[0];
            sum[1] += wt[ix][indexX] * ySum[1];
            sum[2] += wt[ix][indexX] * ySum[2];
            sum[3] += wt[ix][indexX] * ySum[3];
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        // Since color is usually stored as ARGB with values limited to ranges from 0 to
        // 255, clamp the values between 0 and 255 if clip is false or restrict further if
        // clip is true
        sum[0] = Math.max(Math.min(sum[0], alphaMax), alphaMin);
        sum[1] = Math.max(Math.min(sum[1], redMax), redMin);
        sum[2] = Math.max(Math.min(sum[2], greenMax), greenMin);
        sum[3] = Math.max(Math.min(sum[3], blueMax), blueMin);

        return sum;
    }

    /**
     * 3D heptic Lagrangian function.
     *
     * @param   x  float point index
     * @param   y  float point index
     * @param   z  float point index
     *
     * @return  the hepticLagrangian3D interpolated data point
     */
    public final double hepticLagrangian3D(float x, float y, float z) {

        int xbase, ybase, zbase;
        int j0, j1, j2;
        int l0, l1, l2;
        int ix, iy, iz;
        int indexX, indexY, indexZ;
        double diffX, diffY, diffZ;
        double sum;
        double ySum, zSum;
        int offset;

        xbase = (int) x;
        ybase = (int) y;
        zbase = (int) z;
        diffX = x - xbase;
        diffY = y - ybase;
        diffZ = z - zbase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);
        indexZ = (int) (999.0 * diffZ);

        // 15% - 20% faster since Math.max and Math.min are function calls
        // I also replaced the Math.abs but saw no speed improvement.
        sum = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                zSum = 0.0;
                offset = (l1 * xdim) + l0;

                for (iz = 0, j2 = zbase - 3; j2 <= (zbase + 4); iz++, j2++) {
                    l2 = zD; // zdim-1;

                    if (j2 < l2) {
                        l2 = j2;
                    }

                    if (l2 < 0) {
                        l2 = 0;
                    }

                    zSum += wt[iz][indexZ] * volume[(l2 * sliceSize) + offset];
                } // for (iz = 0, j2 = zbase - 3; j2 <= zbase + 4;iz++, j2++)

                ySum += wt[iy][indexY] * zSum;
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++)

            sum += wt[ix][indexX] * ySum;
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        if (clip) {
            sum = Math.max(Math.min(sum, inputMax), inputMin);
        }

        return sum;
    }
    
    /**
     * 3D heptic Lagrangian function.
     *
     * @param   x  double point index
     * @param   y  double point index
     * @param   z  double point index
     *
     * @return  the hepticLagrangian3D interpolated data point
     */
    public final double hepticLagrangian3D(double x, double y, double z) {

        int xbase, ybase, zbase;
        int j0, j1, j2;
        int l0, l1, l2;
        int ix, iy, iz;
        int indexX, indexY, indexZ;
        double diffX, diffY, diffZ;
        double sum;
        double ySum, zSum;
        int offset;

        xbase = (int) x;
        ybase = (int) y;
        zbase = (int) z;
        diffX = x - xbase;
        diffY = y - ybase;
        diffZ = z - zbase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);
        indexZ = (int) (999.0 * diffZ);

        // 15% - 20% faster since Math.max and Math.min are function calls
        // I also replaced the Math.abs but saw no speed improvement.
        sum = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                zSum = 0.0;
                offset = (l1 * xdim) + l0;

                for (iz = 0, j2 = zbase - 3; j2 <= (zbase + 4); iz++, j2++) {
                    l2 = zD; // zdim-1;

                    if (j2 < l2) {
                        l2 = j2;
                    }

                    if (l2 < 0) {
                        l2 = 0;
                    }

                    zSum += wt[iz][indexZ] * volume[(l2 * sliceSize) + offset];
                } // for (iz = 0, j2 = zbase - 3; j2 <= zbase + 4;iz++, j2++)

                ySum += wt[iy][indexY] * zSum;
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++)

            sum += wt[ix][indexX] * ySum;
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        if (clip) {
            sum = Math.max(Math.min(sum, inputMax), inputMin);
        }

        return sum;
    }

    /**
     * 3D heptic Lagrangian function for color (3 channel images).
     *
     * @param   x  float point index
     * @param   y  float point index
     * @param   z  float point index
     *
     * @return  the hepticLagrangian3D interpolated data point
     */
    public double[] hepticLagrangian3DC(float x, float y, float z) {

        int xbase, ybase, zbase;
        int j0, j1, j2;
        int l0, l1, l2;
        int ix, iy, iz;
        double diffX, diffY, diffZ;
        double[] sum = new double[4];
        int offset, offset2;
        int indexX, indexY, indexZ;
        double[] ySum = new double[4];
        double[] zSum = new double[4];

        xbase = (int) x;
        ybase = (int) y;
        zbase = (int) z;
        diffX = x - xbase;
        diffY = y - ybase;
        diffZ = z - zbase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);
        indexZ = (int) (999.0 * diffZ);

        sum[0] = 0.0;
        sum[1] = 0.0;
        sum[2] = 0.f;
        sum[3] = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum[0] = 0.0;
            ySum[1] = 0.0;
            ySum[2] = 0.0;
            ySum[3] = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                zSum[0] = 0.0;
                zSum[1] = 0.0;
                zSum[2] = 0.0;
                zSum[3] = 0.0;
                offset = (l1 * xdim) + l0;

                for (iz = 0, j2 = zbase - 3; j2 <= (zbase + 4); iz++, j2++) {
                    l2 = zD; // zdim-1;

                    if (j2 < l2) {
                        l2 = j2;
                    }

                    if (l2 < 0) {
                        l2 = 0;
                    }

                    offset2 = 4 * ((sliceSize * l2) + offset);
                    zSum[0] += wt[iz][indexZ] * volume[offset2];
                    zSum[1] += wt[iz][indexZ] * volume[offset2 + 1];
                    zSum[2] += wt[iz][indexZ] * volume[offset2 + 2];
                    zSum[3] += wt[iz][indexZ] * volume[offset2 + 3];
                } // for (iz = 0, j2 = zbase - 3; j2 <= zbase + 4;iz++, j2++)

                ySum[0] += wt[iy][indexY] * zSum[0];
                ySum[1] += wt[iy][indexY] * zSum[1];
                ySum[2] += wt[iy][indexY] * zSum[2];
                ySum[3] += wt[iy][indexY] * zSum[3];
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++

            sum[0] += wt[ix][indexX] * ySum[0];
            sum[1] += wt[ix][indexX] * ySum[1];
            sum[2] += wt[ix][indexX] * ySum[2];
            sum[3] += wt[ix][indexX] * ySum[3];
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        // Since color is usually stored as ARGB with values limited to ranges from 0 to
        // 255, clamp the values between 0 and 255 if clip is false or restrict further
        // if clip is true
        sum[0] = Math.max(Math.min(sum[0], alphaMax), alphaMin);
        sum[1] = Math.max(Math.min(sum[1], redMax), redMin);
        sum[2] = Math.max(Math.min(sum[2], greenMax), greenMin);
        sum[3] = Math.max(Math.min(sum[3], blueMax), blueMin);

        return sum;
    }
    
    /**
     * 3D heptic Lagrangian function for color (3 channel images).
     *
     * @param   x  double point index
     * @param   y  double point index
     * @param   z  double point index
     *
     * @return  the hepticLagrangian3D interpolated data point
     */
    public double[] hepticLagrangian3DC(double x, double y, double z) {

        int xbase, ybase, zbase;
        int j0, j1, j2;
        int l0, l1, l2;
        int ix, iy, iz;
        double diffX, diffY, diffZ;
        double[] sum = new double[4];
        int offset, offset2;
        int indexX, indexY, indexZ;
        double[] ySum = new double[4];
        double[] zSum = new double[4];

        xbase = (int) x;
        ybase = (int) y;
        zbase = (int) z;
        diffX = x - xbase;
        diffY = y - ybase;
        diffZ = z - zbase;
        indexX = (int) (999.0 * diffX);
        indexY = (int) (999.0 * diffY);
        indexZ = (int) (999.0 * diffZ);

        sum[0] = 0.0;
        sum[1] = 0.0;
        sum[2] = 0.f;
        sum[3] = 0.0;

        for (ix = 0, j0 = xbase - 3; j0 <= (xbase + 4); ix++, j0++) {
            l0 = xD; // xdim - 1

            if (j0 < l0) {
                l0 = j0;
            }

            if (l0 < 0) {
                l0 = 0;
            }

            ySum[0] = 0.0;
            ySum[1] = 0.0;
            ySum[2] = 0.0;
            ySum[3] = 0.0;

            for (iy = 0, j1 = ybase - 3; j1 <= (ybase + 4); iy++, j1++) {
                l1 = yD; // ydim-1;

                if (j1 < l1) {
                    l1 = j1;
                }

                if (l1 < 0) {
                    l1 = 0;
                }

                zSum[0] = 0.0;
                zSum[1] = 0.0;
                zSum[2] = 0.0;
                zSum[3] = 0.0;
                offset = (l1 * xdim) + l0;

                for (iz = 0, j2 = zbase - 3; j2 <= (zbase + 4); iz++, j2++) {
                    l2 = zD; // zdim-1;

                    if (j2 < l2) {
                        l2 = j2;
                    }

                    if (l2 < 0) {
                        l2 = 0;
                    }

                    offset2 = 4 * ((sliceSize * l2) + offset);
                    zSum[0] += wt[iz][indexZ] * volume[offset2];
                    zSum[1] += wt[iz][indexZ] * volume[offset2 + 1];
                    zSum[2] += wt[iz][indexZ] * volume[offset2 + 2];
                    zSum[3] += wt[iz][indexZ] * volume[offset2 + 3];
                } // for (iz = 0, j2 = zbase - 3; j2 <= zbase + 4;iz++, j2++)

                ySum[0] += wt[iy][indexY] * zSum[0];
                ySum[1] += wt[iy][indexY] * zSum[1];
                ySum[2] += wt[iy][indexY] * zSum[2];
                ySum[3] += wt[iy][indexY] * zSum[3];
            } // for (iy = 0,j1 = ybase - 3; j1 <= ybase + 4;iy++, j1++

            sum[0] += wt[ix][indexX] * ySum[0];
            sum[1] += wt[ix][indexX] * ySum[1];
            sum[2] += wt[ix][indexX] * ySum[2];
            sum[3] += wt[ix][indexX] * ySum[3];
        } // for (ix = 0,j0 = xbase - 3; j0 <= xbase + 4;ix++, j0++)

        // Since color is usually stored as ARGB with values limited to ranges from 0 to
        // 255, clamp the values between 0 and 255 if clip is false or restrict further
        // if clip is true
        sum[0] = Math.max(Math.min(sum[0], alphaMax), alphaMin);
        sum[1] = Math.max(Math.min(sum[1], redMax), redMin);
        sum[2] = Math.max(Math.min(sum[2], greenMax), greenMin);
        sum[3] = Math.max(Math.min(sum[3], blueMax), blueMin);

        return sum;
    }

    /**
     * run - default method that is not really appropiate for this class but must be defined because this class extends
     * AlgorithmBase.
     */
    public void runAlgorithm() { }

    /**
     * Setup 2D hepticLagrangian.
     *
     * @param  vol      volume comprising control points for the heptic Lagrangian
     * @param  extents  vol extents (xdim, ydim)
     * @param  clip     if true clip output to range of input image
     */
    public void setup2DHepticLagrangian(double[] vol, int[] extents, boolean clip) {
        int i;
        double arg;

        volume = vol;
        xdim = extents[0];
        ydim = extents[1];
        this.clip = clip;
        xD = xdim - 1;
        yD = ydim - 1;
        wt = new double[8][1000];

        for (i = 0; i < 1000; i++) {
            arg = i / 999.0;
            wt[0][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg - 3.0) * (4.0 - arg)) / 5040.0);
            wt[1][i] = ((arg * ((arg * arg) - 1.0) * (arg - 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 720.0);
            wt[2][i] = ((arg * (arg - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 240.0);
            wt[3][i] = ((((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (arg - 4.0)) /
                                    144.0);
            wt[4][i] = ((arg * (arg + 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 144.0);
            wt[5][i] = ((arg * ((arg * arg) - 1.0) * (arg + 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 240.0);
            wt[6][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg + 3.0) * (4.0 - arg)) / 720.0);
            wt[7][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0)) / 5040.0);
        }

        if (clip) {
            inputMin = volume[0];
            inputMax = volume[0];

            for (i = 1; i < volume.length; i++) {

                if (volume[i] < inputMin) {
                    inputMin = volume[i];
                }

                if (volume[i] > inputMax) {
                    inputMax = volume[i];
                }
            }
        } // if (clip)
    }

    /**
     * Setup 2D hepticLagrangian for color.
     *
     * @param  vol      volume comprising control points for the heptic Lagrangian
     * @param  extents  vol extents (xdim, ydim)
     * @param  argbMax  maximum possible color value
     * @param  clip     if true clip output to range of input image
     */
    public void setup2DHepticLagrangianC(double[] vol, int[] extents, float argbMax, boolean clip) {
        int i;
        double arg;

        volume = vol;
        xdim = extents[0];
        ydim = extents[1];
        alphaMax = argbMax;
        redMax = argbMax;
        greenMax = argbMax;
        blueMax = argbMax;
        this.clip = clip;
        xD = xdim - 1;
        yD = ydim - 1;
        wt = new double[8][1000];

        for (i = 0; i < 1000; i++) {
            arg = i / 999.0;
            wt[0][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg - 3.0) * (4.0 - arg)) / 5040.0);
            wt[1][i] = ((arg * ((arg * arg) - 1.0) * (arg - 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 720.0);
            wt[2][i] = ((arg * (arg - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 240.0);
            wt[3][i] = ((((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (arg - 4.0)) /
                                    144.0);
            wt[4][i] = ((arg * (arg + 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 144.0);
            wt[5][i] = ((arg * ((arg * arg) - 1.0) * (arg + 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 240.0);
            wt[6][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg + 3.0) * (4.0 - arg)) / 720.0);
            wt[7][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0)) / 5040.0);
        }

        if (clip) {
            alphaMin = volume[0];
            alphaMax = volume[0];
            redMin = volume[1];
            redMax = volume[1];
            greenMin = volume[2];
            greenMax = volume[2];
            blueMin = volume[3];
            blueMax = volume[3];

            for (i = 1; i < (volume.length / 4); i++) {

                if (volume[4 * i] < alphaMin) {
                    alphaMin = volume[4 * i];
                }

                if (volume[4 * i] > alphaMax) {
                    alphaMax = volume[4 * i];
                }

                if (volume[(4 * i) + 1] < redMin) {
                    redMin = volume[(4 * i) + 1];
                }

                if (volume[(4 * i) + 1] > redMax) {
                    redMax = volume[(4 * i) + 1];
                }

                if (volume[(4 * i) + 2] < greenMin) {
                    greenMin = volume[(4 * i) + 2];
                }

                if (volume[(4 * i) + 2] > greenMax) {
                    greenMax = volume[(4 * i) + 2];
                }

                if (volume[(4 * i) + 3] < blueMin) {
                    blueMin = volume[(4 * i) + 3];
                }

                if (volume[(4 * i) + 3] > blueMax) {
                    blueMax = volume[(4 * i) + 3];
                }
            } // for (i = 1; i < (volume.length/4); i++)
        } // if (clip)
    }

    /**
     * Setup 3D heptic Lagrangian.
     *
     * @param  vol      volume comprising control points for the heptic Lagrangian
     * @param  extents  vol extents (xdim, ydim, zdim)
     * @param  clip     if true clip output to range of input image
     */
    public void setup3DHepticLagrangian(double[] vol, int[] extents, boolean clip) {
        int i;
        double arg;

        volume = vol;
        xdim = extents[0];
        ydim = extents[1];
        zdim = extents[2];
        this.clip = clip;
        xD = xdim - 1;
        yD = ydim - 1;
        zD = zdim - 1;
        sliceSize = xdim * ydim;
        wt = new double[8][1000];

        for (i = 0; i < 1000; i++) {
            arg = i / 999.0;
            wt[0][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg - 3.0) * (4.0 - arg)) / 5040.0);
            wt[1][i] = ((arg * ((arg * arg) - 1.0) * (arg - 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 720.0);
            wt[2][i] = ((arg * (arg - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 240.0);
            wt[3][i] = ((((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (arg - 4.0)) /
                                    144.0);
            wt[4][i] = ((arg * (arg + 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 144.0);
            wt[5][i] = ((arg * ((arg * arg) - 1.0) * (arg + 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 240.0);
            wt[6][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg + 3.0) * (4.0 - arg)) / 720.0);
            wt[7][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0)) / 5040.0);
        }

        if (clip) {
            inputMin = volume[0];
            inputMax = volume[0];

            for (i = 1; i < volume.length; i++) {

                if (volume[i] < inputMin) {
                    inputMin = volume[i];
                }

                if (volume[i] > inputMax) {
                    inputMax = volume[i];
                }
            }
        } // if (clip)
    }

    /**
     * Setup 3D heptic Lagrangian for color.
     *
     * @param  vol      volume comprising control points for the heptic Lagrangian
     * @param  extents  vol extents (xdim, ydim, zdim)
     * @param  argbMax  maximum possible color value
     * @param  clip     if true clip output to range of input image
     */
    public void setup3DHepticLagrangianC(double[] vol, int[] extents, float argbMax, boolean clip) {
        int i;
        double arg;

        volume = vol;
        xdim = extents[0];
        ydim = extents[1];
        zdim = extents[2];
        alphaMax = argbMax;
        redMax = argbMax;
        greenMax = argbMax;
        blueMax = argbMax;
        this.clip = clip;
        xD = xdim - 1;
        yD = ydim - 1;
        zD = zdim - 1;
        sliceSize = xdim * ydim;
        wt = new double[8][1000];

        for (i = 0; i < 1000; i++) {
            arg = i / 999.0;
            wt[0][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg - 3.0) * (4.0 - arg)) / 5040.0);
            wt[1][i] = ((arg * ((arg * arg) - 1.0) * (arg - 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 720.0);
            wt[2][i] = ((arg * (arg - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 240.0);
            wt[3][i] = ((((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (arg - 4.0)) /
                                    144.0);
            wt[4][i] = ((arg * (arg + 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0) * (4.0 - arg)) / 144.0);
            wt[5][i] = ((arg * ((arg * arg) - 1.0) * (arg + 2.0) * ((arg * arg) - 9.0) * (arg - 4.0)) / 240.0);
            wt[6][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * (arg + 3.0) * (4.0 - arg)) / 720.0);
            wt[7][i] = ((arg * ((arg * arg) - 1.0) * ((arg * arg) - 4.0) * ((arg * arg) - 9.0)) / 5040.0);
        }

        if (clip) {
            alphaMin = volume[0];
            alphaMax = volume[0];
            redMin = volume[1];
            redMax = volume[1];
            greenMin = volume[2];
            greenMax = volume[2];
            blueMin = volume[3];
            blueMax = volume[3];

            for (i = 1; i < (volume.length / 4); i++) {

                if (volume[4 * i] < alphaMin) {
                    alphaMin = volume[4 * i];
                }

                if (volume[4 * i] > alphaMax) {
                    alphaMax = volume[4 * i];
                }

                if (volume[(4 * i) + 1] < redMin) {
                    redMin = volume[(4 * i) + 1];
                }

                if (volume[(4 * i) + 1] > redMax) {
                    redMax = volume[(4 * i) + 1];
                }

                if (volume[(4 * i) + 2] < greenMin) {
                    greenMin = volume[(4 * i) + 2];
                }

                if (volume[(4 * i) + 2] > greenMax) {
                    greenMax = volume[(4 * i) + 2];
                }

                if (volume[(4 * i) + 3] < blueMin) {
                    blueMin = volume[(4 * i) + 3];
                }

                if (volume[(4 * i) + 3] > blueMax) {
                    blueMax = volume[(4 * i) + 3];
                }
            } // for (i = 1; i < (volume.length/4); i++)
        } // if (clip)
    }

}
