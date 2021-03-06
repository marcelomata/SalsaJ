//EU_HOU
package ij;

import java.awt.*;
import java.awt.image.*;
import java.net.URL;
import java.util.*;
import ij.process.*;
import ij.io.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.util.Tools;
import ij.macro.Interpreter;
import ij.plugin.ContrastEnhancer;
import ij.plugin.frame.ContrastAdjuster;
import ij.plugin.Converter;

/**
 *  This is an extended image class that supports 8-bit, 16-bit, 32-bit (real)
 *  and RGB images. It also provides support for 3D image stacks.
 *
 *@author     Thomas
 *@created    29 octobre 2007
 *@see        ij.process.ImageProcessor
 *@see        ij.ImageStack
 *@see        ij.gui.ImageWindow
 *@see        ij.gui.ImageCanvas
 */
public class ImagePlus implements ImageObserver, Measurements {

    /**
     *  8-bit grayscale (unsigned)
     */
    public final static int GRAY8 = 0;
    /**
     *  16-bit grayscale (unsigned)
     */
    public final static int GRAY16 = 1;
    /**
     *  32-bit floating-point grayscale
     */
    public final static int GRAY32 = 2;
    /**
     *  8-bit indexed color
     */
    public final static int COLOR_256 = 3;
    /**
     *  32-bit RGB color
     */
    public final static int COLOR_RGB = 4;
    /**
     *  True if any changes have been made to this image.
     */
    public boolean changes;
    /**
     *  Obsolete. Use GetCalibration().
     */
    public double pixelWidth = 1.0, pixelHeight = 1.0;
    /**
     *  Obsolete. Use GetCalibration().
     */
    public String unit = "pixel";
    /**
     *  Obsolete. Use GetCalibration().
     */
    public String units = unit;
    /**
     *  Obsolete. Use GetCalibration().
     */
    public boolean sCalibrated;
    /**
     *  Description of the Field
     */
    protected Image img;
    /**
     *  Description of the Field
     */
    protected ImageProcessor ip;
    /**
     *  Description of the Field
     */
    protected ImageWindow win;
    /**
     *  Description of the Field
     */
    protected Roi roi;
    /**
     *  Description of the Field
     */
    protected int currentSlice;
    /**
     *  Description of the Field
     */
    protected final static int OPENED = 0, CLOSED = 1, UPDATED = 2;
    /**
     *  Description of the Field
     */
    protected boolean compositeImage;
    /**
     *  Description of the Field
     */
    protected int width;
    /**
     *  Description of the Field
     */
    protected int height;
    /**
     *  Description of the Field
     */
    protected boolean locked = false;
    private ImageJ ij = IJ.getInstance();
    private String title;
    private String url;
    private FileInfo fileInfo;
    private int nSlices = 1;
    private int nChannels = 1;
    private int nFrames = 1;
    private int imageType = GRAY8;
    private ImageStack stack;
    private static int currentID = -1;
    private int ID;
    private static Component comp;
    private boolean imageLoaded;
    private int imageUpdateY, imageUpdateW;
    private Properties properties;
    private long startTime;
    private Calibration calibration;
    private static Calibration globalCalibration;
    private boolean activated;
    private boolean ignoreFlush;
    private boolean errorLoadingImage;
    private static ImagePlus clipboard;
    private static Vector listeners = new Vector();
    private boolean openAsHyperVolume;
    /*
     *  EU_HOU CHANGES
     */
    private String ComplementaryHeader = "";


    /*
     *  EU_HOU END
     */
    /**
     *  Constructs an uninitialized ImagePlus.
     */
    public ImagePlus() {
        ID = --currentID;
        title = "null";
    }

    /**
     *  Constructs an ImagePlus from an AWT Image. The first argument will be used
     *  as the title of the window that displays the image. Throws an
     *  IllegalStateException if an error occurs while loading the image.
     *
     *@param  title  Description of the Parameter
     *@param  img    Description of the Parameter
     */
    public ImagePlus(String title, Image img) {
        System.out.println("ImagePlus img=" + img + "	,title=" + title);
        this.title = title;
        ID = --currentID;
        /*
         *  EU_HOU CHANGES
         */
        if (img != null) {
            if (title != null) {
                setImage(img);
                System.out.println("ImagePlus img");
            } else {
                setImage2(img);
                System.out.println("ImagePlus img2");
            }

        }

        /*
         *  EU_HOU END
         */
    }


    /*
     *  EU_HOU CHANGES
     */
    /**
     *  Constructor for the ImagePlus object
     *
     *@param  imp  Description of the Parameter
     *@param  img  Description of the Parameter
     */
    public ImagePlus(ImagePlus imp, Image img) {
        this.title = null;
        ID = --currentID;
        if (img != null) {
            setImage(imp, img);
        }
    }


    /*
     *  EU_HOU END
     */
    /**
     *  Constructs an ImagePlus from an ImageProcessor.
     *
     *@param  title  Description of the Parameter
     *@param  ip     Description of the Parameter
     */
    public ImagePlus(String title, ImageProcessor ip) {
        setProcessor(title, ip);
        ID = --currentID;
    }

    /**
     *  Constructs an ImagePlus from a TIFF, BMP, DICOM, FITS, PGM, GIF or JPRG
     *  specified by a path or from a TIFF, DICOM, GIF or JPEG specified by a URL.
     *
     *@param  pathOrURL  Description of the Parameter
     */
    public ImagePlus(String pathOrURL) {
        Opener opener = new Opener();
        ImagePlus imp = null;
        boolean isURL = pathOrURL.indexOf("://") > 0;

        if (isURL) {
            imp = opener.openURL(pathOrURL);
        } else {
            imp = opener.openImage(pathOrURL);
        }
        if (imp != null) {

            if (imp.getStackSize() > 1) {
                setStack(imp.getTitle(), imp.getStack());
            } else {
                setProcessor(imp.getTitle(), imp.getProcessor());
            }
            setCalibration(imp.getCalibration());
            properties = imp.getProperties();
            setFileInfo(imp.getOriginalFileInfo());
            if (isURL) {
                this.url = pathOrURL;
            }
            ID = --currentID;
        }
    }

    /**
     *  Constructs an ImagePlus from a stack.
     *
     *@param  title  Description of the Parameter
     *@param  stack  Description of the Parameter
     */
    public ImagePlus(String title, ImageStack stack) {
        setStack(title, stack);
        ID = --currentID;
    }

    /**
     *  Locks the image so other threads can test to see if it is in use. Returns
     *  true if the image was successfully locked. Beeps, displays a message in the
     *  status bar, and returns false if the image is already locked.
     *
     *@return    Description of the Return Value
     */
    public synchronized boolean lock() {
        if (locked) {
            IJ.beep();
            IJ.showStatus("\"" + title + "\" is locked");
            if (IJ.macroRunning()) {
                //EU_HOU Bundle
                IJ.error("Image is locked");
                Macro.abort();
            }
            return false;
        } else {
            locked = true;
            if (IJ.debugMode) {
                IJ.log(title + ": lock");
            }
            return true;
        }
    }

    /**
     *  Similar to lock, but doesn't beep and display an error message if the
     *  attempt to lock the image fails.
     *
     *@return    Description of the Return Value
     */
    public synchronized boolean lockSilently() {
        if (locked) {
            return false;
        } else {
            locked = true;
            if (IJ.debugMode) {
                //EU_HOU Bundle
                IJ.log(title + ": lock silently");
            }
            return true;
        }
    }

    /**
     *  Unlocks the image.
     */
    public synchronized void unlock() {
        locked = false;
        if (IJ.debugMode) {
            //EU_HOU Bundle
            IJ.log(title + ": unlock");
        }
    }

    /**
     *  Description of the Method
     *
     *@param  img  Description of the Parameter
     */
    private void waitForImage(Image img) {
        if (comp == null) {
            comp = IJ.getInstance();
            if (comp == null) {
                comp = new Canvas();
            }
        }
        imageLoaded = false;
        if (!comp.prepareImage(img, this)) {
            double progress;

            waitStart = System.currentTimeMillis();
            while (!imageLoaded && !errorLoadingImage) {
                //IJ.showStatus(imageUpdateY+" "+imageUpdateW);
                IJ.wait(30);
                if (imageUpdateW > 1) {
                    progress = (double) imageUpdateY / imageUpdateW;
                    if (!(progress < 1.0)) {
                        progress = 1.0 - (progress - 1.0);
                        if (progress < 0.0) {
                            progress = 0.9;
                        }
                    }
                    showProgress(progress);
                }
            }
            showProgress(1.0);
        }
    }
    long waitStart;

    /**
     *  Description of the Method
     *
     *@param  percent  Description of the Parameter
     */
    private void showProgress(double percent) {
        if ((System.currentTimeMillis() - waitStart) > 500L) {
            IJ.showProgress(percent);
        }
    }

    /**
     *  Draws the image. If there is an ROI, its outline is also displayed. Does
     *  nothing if there is no window associated with this image (i.e. show() has
     *  not been called).
     */
    public void draw() {
        if (win != null) {
            win.getCanvas().repaint();
        }
    }

    /**
     *  Draws image and roi outline using a clip rect.
     *
     *@param  x       Description of the Parameter
     *@param  y       Description of the Parameter
     *@param  width   Description of the Parameter
     *@param  height  Description of the Parameter
     */
    public void draw(int x, int y, int width, int height) {
        if (win != null) {
            ImageCanvas ic = win.getCanvas();
            double mag = ic.getMagnification();

            x = ic.screenX(x);
            y = ic.screenY(y);
            width = (int) (width * mag);
            height = (int) (height * mag);
            ic.repaint(x, y, width, height);
            if (listeners.size() > 0 && roi != null && roi.getPasteMode() != Roi.NOT_PASTING) {
                notifyListeners(UPDATED);
            }
        }
    }

    /**
     *  Updates this image from the pixel data in its associated ImageProcessor,
     *  then displays it. Does nothing if there is no window associated with this
     *  image (i.e. show() has not been called).
     */
    public void updateAndDraw() {
        if (ip != null) {
            if (win != null) {
                win.getCanvas().setImageUpdated();
            }
            draw();
            if (listeners.size() > 0) {
                notifyListeners(UPDATED);
            }
        }
    }

    /**
     *  Updates this image from the pixel data in its associated ImageProcessor,
     *  then displays it. The CompositeImage class overrides this method to only
     *  update the current channel.
     */
    public void updateChannelAndDraw() {
        updateAndDraw();
    }

    /**
     *  Returns a reference to the current ImageProcessor. The CompositeImage class
     *  overrides this method so it returns the processor associated with the
     *  current channel.
     *
     *@return    The channelProcessor value
     */
    public ImageProcessor getChannelProcessor() {
        return getProcessor();
    }

    /**
     *  Calls draw to draw the image and also repaints the image window to force
     *  the information displayed above the image (dimension, type, size) to be
     *  updated.
     */
    public void repaintWindow() {
        if (win != null) {
            draw();
            win.repaint();
        }
    }

    /**
     *  Calls updateAndDraw to update from the pixel data and draw the image, and
     *  also repaints the image window to force the information displayed above the
     *  image (dimension, type, size) to be updated.
     */
    public void updateAndRepaintWindow() {
        if (win != null) {
            updateAndDraw();
            win.repaint();
        }
    }

    /**
     *  ImageCanvas.paint() calls this method when the ImageProcessor has generated
     *  new image.
     */
    public void updateImage() {
        if (ip != null) {
            img = ip.createImage();
        }
    }

    /**
     *  Closes the window, if any, that is displaying this image.
     */
    public void hide() {
        if (win == null) {
            Interpreter.removeBatchModeImage(this);
            return;
        }
        boolean unlocked = lockSilently();

        changes = false;
        win.close();
        win = null;
        if (unlocked) {
            unlock();
        }
    }

    /**
     *  Closes this image and sets the pixel arrays to null. To avoid the "Save
     *  changes?" dialog, first set the public 'changes' variable to false.
     */
    public void close() {
        ImageWindow win = getWindow();

        if (win != null) {
            //if (IJ.isWindows() && IJ.isJava14())
            //	changes = false; // avoid 'save changes?' dialog and potential Java 1.5 deadlocks
            win.close();
        } else {
            if (WindowManager.getCurrentImage() == this) {
                WindowManager.setTempCurrentImage(null);
            }
            killRoi();//save any ROI so it can be restored later
            Interpreter.removeBatchModeImage(this);
        }
    }

    /**
     *  Opens a window to display this image and clears the status bar.
     */
    public void show() {
        /*
         *  EU_HOU CHANGES
         */
        show("", true);


        /*
         *  EU_HOU END
         */
    }


    /*
     *  EU_HOU CHANGES
     */
    /**
     *  Description of the Method
     *
     *@param  accessibleImage  Description of the Parameter
     */
    public void show(boolean accessibleImage) {
        show("", accessibleImage);
    }


    /*
     *  EU_HOU END
     */
    /**
     *  Opens a window to display this image and displays 'statusMessage' in the
     *  status bar.
     *
     *@param  statusMessage    Description of the Parameter
     *@param  accessibleImage  Description of the Parameter
     */
    //EU_HOU Changes
    public void show(String statusMessage, boolean accessibleImage) {
        if (win != null) {
            return;
        }
        if ((IJ.macroRunning() && ij == null) || Interpreter.isBatchMode()) {
            ImagePlus img = WindowManager.getCurrentImage();

            if (img != null) {
                img.saveRoi();
            }
            WindowManager.setTempCurrentImage(this);
            Interpreter.addBatchModeImage(this);
            return;
        }
        if (Prefs.useInvertingLut && getBitDepth() == 8 && ip != null && !ip.isInvertedLut() && !ip.isColorLut()) {
            invertLookupTable();
        }
        img = getImage();
        if ((img != null) && (width >= 0) && (height >= 0)) {
            activated = false;

            int stackSize = getStackSize();
            //if (compositeImage) stackSize /= nChannels;
            if (stackSize > 1) {
                /*
                 *  EU_HOU CHANGES
                 */
                win = new StackWindow(this, accessibleImage);
            } else {
                win = new ImageWindow(this, accessibleImage);
            }
            /*
             *  EU_HOU END
             */
            if (roi != null) {
                roi.setImage(this);
            }
            draw();
            // enhance contrast
            if (!activated) {
                ContrastEnhancer ce = new ContrastEnhancer();
                ce.stretchHistogram(this, 0.1);
                this.updateAndDraw();
            }

            IJ.showStatus(statusMessage);
            if (IJ.macroRunning()) {// wait for image to become activated
                //IJ.log("Waiting for image to be activated");
                long start = System.currentTimeMillis();

                while (!activated) {
                    IJ.wait(5);
                    if ((System.currentTimeMillis() - start) > 2000) {
                        WindowManager.setTempCurrentImage(this);
                        break;// 2 second timeout
                    }
                }
                //IJ.log(""+(System.currentTimeMillis()-start));
            }
            notifyListeners(OPENED);
        }
    }

    /**
     *  Description of the Method
     */
    void invertLookupTable() {
        int nImages = getStackSize();

        ip.invertLut();
        if (nImages == 1) {
            ip.invert();
        } else {
            ImageStack stack2 = getStack();

            for (int i = 1; i <= nImages; i++) {
                stack2.getProcessor(i).invert();
            }
            stack2.setColorModel(ip.getColorModel());
        }
    }

    /**
     *  Called by ImageWindow.windowActivated().
     */
    public void setActivated() {
        activated = true;
    }

    /**
     *  Returns the current AWT image.
     *
     *@return    The image value
     */
    public Image getImage() {
        if (img == null && ip != null) {
            img = ip.createImage();
        }
        return img;
    }

    /**
     *  Returns this image's unique numeric ID.
     *
     *@return    The iD value
     */
    public int getID() {
        return ID;
    }

    /**
     *  Replaces the image, if any, with the one specified. Throws an
     *  IllegalStateException if an error occurs while loading the image.
     *
     *@param  img  The new image value
     */
    public void setImage(Image img) {
        if (img instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) img;

            if (bi.getType() == BufferedImage.TYPE_USHORT_GRAY) {
                setProcessor(null, new ShortProcessor(bi));
                return;
            }
        }
        roi = null;
        errorLoadingImage = false;
        waitForImage(img);
        if (errorLoadingImage) {
            //EU_HOU Bundle
            throw new IllegalStateException("Error loading image");
        }
        this.img = img;

        int newWidth = img.getWidth(ij);
        int newHeight = img.getHeight(ij);
        boolean dimensionsChanged = newWidth != width || newHeight != height;

        width = newWidth;
        height = newHeight;
        ip = null;
        stack = null;

        LookUpTable lut = new LookUpTable(img);
        int type;

        if (lut.getMapSize() > 0) {
            if (lut.isGrayscale()) {
                type = GRAY8;
            } else {
                type = COLOR_256;
            }
        } else {
            type = COLOR_RGB;
        }
        setType(type);
        setupProcessor();
        this.img = ip.createImage();
        if (win != null) {
            if (dimensionsChanged) {
                win = new ImageWindow(this);
            } else {
                repaintWindow();
            }
        }
    }


    /*
     *  EU_HOU CHANGES
     */
    /**
     *  Sets the image2 attribute of the ImagePlus object
     *
     *@param  img  The new image2 value
     */
    //EU_HOU ADD
    public void setImage2(Image img) {
        if (img instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) img;

            if (bi.getType() == BufferedImage.TYPE_USHORT_GRAY) {
                setProcessor(null, new ShortProcessor(bi));
                return;
            }
        }
        roi = null;
        errorLoadingImage = false;
        //waitForImage(img);
        if (errorLoadingImage) {
            //EU_HOU Bundle
            throw new IllegalStateException("Error loading image");
        }
        this.img = img;

        int newWidth = img.getWidth(ij);
        int newHeight = img.getHeight(ij);

        width = newWidth;
        height = newHeight;
        ip = null;
        stack = null;

        LookUpTable lut = new LookUpTable(img);
        int type;

        if (lut.getMapSize() > 0) {
            if (lut.isGrayscale()) {
                type = GRAY8;
            } else {
                type = COLOR_256;
            }
        } else {
            type = COLOR_RGB;
        }
        setType(type);
        setupProcessor();
        this.img = ip.createImage();

    }

    /**
     *  Sets the image attribute of the ImagePlus object
     *
     *@param  imp  The new image value
     *@param  img  The new image value
     */
    public void setImage(ImagePlus imp, Image img) {
        roi = null;
        this.img = img;

        int newWidth = img.getWidth(ij);
        int newHeight = img.getHeight(ij);

        width = newWidth;
        height = newHeight;
        ip = null;
        stack = null;

        LookUpTable lut = imp.createLut();
        int type;

        if (lut.getMapSize() > 0) {
            if (lut.isGrayscale()) {
                type = GRAY8;
            } else {
                type = COLOR_256;
            }
        } else {
            type = COLOR_RGB;
        }
        setType(type);
        setupProcessor();
        ip.setColorModel(imp.getProcessor().getColorModel());
        //	    this.img = img;

    }


    /*
     *  EU_HOU END
     */
    /**
     *  Replaces the ImageProcessor, if any, with the one specified. Set 'title' to
     *  null to leave the image title unchanged.
     *
     *@param  title  The new processor value
     *@param  ip     The new processor value
     */
    public void setProcessor(String title, ImageProcessor ip) {
        int stackSize = getStackSize();

        if (stackSize > 1 && (ip.getWidth() != width || ip.getHeight() != height)) {
            //EU_HOU Bundle
            throw new IllegalArgumentException("ip wrong size");
        }
        if (stackSize <= 1) {
            stack = null;
            currentSlice = 1;
        }
        setProcessor2(title, ip, null);
    }

    /**
     *  Sets the processor2 attribute of the ImagePlus object
     *
     *@param  title     The new processor2 value
     *@param  ip        The new processor2 value
     *@param  newStack  The new processor2 value
     */
    void setProcessor2(String title, ImageProcessor ip, ImageStack newStack) {
        if (title != null) {
            setTitle(title);
        }
        this.ip = ip;
        if (ij != null) {
            ip.setProgressBar(ij.getProgressBar());
        }
        int stackSize = 1;

        if (stack != null) {
            stackSize = stack.getSize();
            if (currentSlice > stackSize) {
                currentSlice = stackSize;
            }
        }
        img = null;

        boolean dimensionsChanged = width > 0 && height > 0 && (width != ip.getWidth() || height != ip.getHeight());

        if (dimensionsChanged) {
            roi = null;
        }
        int type;

        if (ip instanceof ByteProcessor) {
            type = GRAY8;
        } else if (ip instanceof ColorProcessor) {
            type = COLOR_RGB;
        } else if (ip instanceof ShortProcessor) {
            type = GRAY16;
        } else {
            type = GRAY32;
        }
        if (width == 0) {
            imageType = type;
        } else {
            setType(type);
        }
        width = ip.getWidth();
        height = ip.getHeight();
        if (win != null) {
            if (dimensionsChanged && stackSize == 1) {
                win.updateImage(this);
            } else if (newStack == null) {
                repaintWindow();
            }
        }
    }

    /**
     *  Replaces the stack, if any, with the one specified. Set 'title' to null to
     *  leave the title unchanged.
     *
     *@param  title  The new stack value
     *@param  stack  The new stack value
     */
    public void setStack(String title, ImageStack stack) {
        int stackSize = stack.getSize();

        if (stackSize == 0) {
            //EU_HOU Bundle
            throw new IllegalArgumentException("Stack is empty");
        }
        boolean stackSizeChanged = this.stack != null && stackSize != getStackSize();

        if (currentSlice < 1) {
            currentSlice = 1;
        }
        boolean resetCurrentSlice = currentSlice > stackSize;

        if (resetCurrentSlice) {
            currentSlice = stackSize;
        }
        //IJ.log("setStack: "+stack+"  "+stackSizeChanged+"  "+resetCurrentSlice);
        ImageProcessor ip = stack.getProcessor(currentSlice);
        boolean dimensionsChanged = width > 0 && height > 0 && (width != ip.getWidth() || height != ip.getHeight());

        this.stack = stack;
        setProcessor2(title, ip, stack);
        if (win == null) {
            return;
        }
        if (stackSize == 1 && win instanceof StackWindow) {
            win = new ImageWindow(this, getCanvas());
        } // replaces this window
        else if (dimensionsChanged && !stackSizeChanged) {
            win.updateImage(this);
        } else if (stackSize > 1 && !(win instanceof StackWindow)) {
            win = new StackWindow(this, getCanvas());
        } // replaces this window
        else if (stackSize > 1 && dimensionsChanged) {
            win = new StackWindow(this);
        } // replaces this window
        else {
            repaintWindow();
        }
        if (resetCurrentSlice) {
            setSlice(currentSlice);
        }
    }

    /**
     *  Saves this image's FileInfo so it can be later retieved using
     *  getOriginalFileInfo().
     *
     *@param  fi  The new fileInfo value
     */
    public void setFileInfo(FileInfo fi) {
        if (fi != null) {
            fi.pixels = null;
        }
        fileInfo = fi;
    }

    /**
     *  Returns the ImageWindow that is being used to display this image. Returns
     *  null if show() has not be called or the ImageWindow has been closed.
     *
     *@return    The window value
     */
    public ImageWindow getWindow() {
        return win;
    }

    /**
     *  This method should only be called from an ImageWindow.
     *
     *@param  win  The new window value
     */
    public void setWindow(ImageWindow win) {
        this.win = win;
        if (roi != null) {
            roi.setImage(this);
        }// update roi's 'ic' field
    }

    /**
     *  Returns the ImageCanvas being used to display this image, or null.
     *
     *@return    The canvas value
     */
    public ImageCanvas getCanvas() {
        return win != null ? win.getCanvas() : null;
    }

    /**
     *  Sets current foreground color.
     *
     *@param  c  The new color value
     */
    public void setColor(Color c) {
        if (ip != null) {
            ip.setColor(c);
        }
    }

    /**
     *  Description of the Method
     */
    void setupProcessor() {
        if (imageType == COLOR_RGB) {
            if (ip == null || ip instanceof ByteProcessor) {
                ip = new ColorProcessor(getImage());
                if (IJ.debugMode) {
                    //EU_HOU Bundle
                    IJ.log(title + ": new ColorProcessor");
                }
            }
        } else if (ip == null || (ip instanceof ColorProcessor)) {
            ip = new ByteProcessor(getImage());
            if (IJ.debugMode) {
                //EU_HOU Bundle
                IJ.log(title + ": new ByteProcessor");
            }
        }
        if (roi != null && roi.isArea()) {
            ip.setRoi(roi.getBounds());
        } else {
            ip.resetRoi();
        }
    }

    /**
     *  Gets the processor attribute of the ImagePlus object
     *
     *@return    The processor value
     */
    public boolean isProcessor() {
        return ip != null;
    }

    /**
     *  Returns a reference to the current ImageProcessor. If there is no
     *  ImageProcessor, it creates one. Returns null if this ImagePlus contains no
     *  ImageProcessor and no AWT Image.
     *
     *@return    The processor value
     */
    public ImageProcessor getProcessor() {
        if (ip == null && img == null) {
            return null;
        }
        setupProcessor();
        if (!compositeImage) {
            ip.setLineWidth(Line.getWidth());
        }
        if (ij != null) {
            //setColor(Toolbar.getForegroundColor());
            ip.setProgressBar(ij.getProgressBar());
        }
        return ip;
    }

    /**
     *  Frees RAM by setting the snapshot (undo) buffer in the current
     *  ImageProcessor to null.
     */
    public void trimProcessor() {
        ImageProcessor ip2 = ip;

        if (!locked && ip2 != null) {
            if (IJ.debugMode) {
                //EU_HOU Bundle
                IJ.log(title + ": trimProcessor");
            }
            ip2.setSnapshotPixels(null);
        }
    }

    /**
     *  Obsolete.
     */
    public void killProcessor() {
    }

    /**
     *  For images with irregular ROIs, returns a byte mask, otherwise, returns
     *  null. Mask pixels have a non-zero value.
     *
     *@return    The mask value
     */
    public ImageProcessor getMask() {
        if (roi == null) {
            if (ip != null) {
                ip.resetRoi();
            }
            return null;
        }
        ImageProcessor mask = roi.getMask();

        if (mask == null) {
            return null;
        }
        if (ip != null) {
            ip.setMask(mask);
            ip.setRoi(roi.getBounds());
        }
        return mask;
    }

    /**
     *  Returns an ImageStatistics object generated using the standard measurement
     *  options (area, mean, mode, min and max).
     *
     *@return    The statistics value
     */
    public ImageStatistics getStatistics() {
        return getStatistics(AREA + MEAN + MODE + MIN_MAX);
    }

    /**
     *  Returns an ImageStatistics object generated using the specified measurement
     *  options.
     *
     *@param  mOptions  Description of the Parameter
     *@return           The statistics value
     */
    public ImageStatistics getStatistics(int mOptions) {
        return getStatistics(mOptions, 256, 0.0, 0.0);
    }

    /**
     *  Returns an ImageStatistics object generated using the specified measurement
     *  options and histogram bin count. Note: except for float images, the number
     *  of bins is currently fixed at 256.
     *
     *@param  mOptions  Description of the Parameter
     *@param  nBins     Description of the Parameter
     *@return           The statistics value
     */
    public ImageStatistics getStatistics(int mOptions, int nBins) {
        return getStatistics(mOptions, nBins, 0.0, 0.0);
    }

    /**
     *  Returns an ImageStatistics object generated using the specified measurement
     *  options, histogram bin count and histogram range. Note: for 8-bit and RGB
     *  images, the number of bins is fixed at 256 and the histogram range is
     *  always 0-255.
     *
     *@param  mOptions  Description of the Parameter
     *@param  nBins     Description of the Parameter
     *@param  histMin   Description of the Parameter
     *@param  histMax   Description of the Parameter
     *@return           The statistics value
     */
    public ImageStatistics getStatistics(int mOptions, int nBins, double histMin, double histMax) {
        setupProcessor();
        if (roi != null && roi.isArea()) {
            ip.setRoi(roi);
        } else {
            ip.resetRoi();
        }
        ip.setHistogramSize(nBins);

        Calibration cal = getCalibration();

        if (getType() == GRAY16 && !(histMin == 0.0 && histMax == 0.0)) {
            histMin = cal.getRawValue(histMin);
            histMax = cal.getRawValue(histMax);
        }
        ip.setHistogramRange(histMin, histMax);

        ImageStatistics stats = ImageStatistics.getStatistics(ip, mOptions, cal);

        ip.setHistogramSize(256);
        ip.setHistogramRange(0.0, 0.0);
        return stats;
    }

    /**
     *  Returns the image name.
     *
     *@return    The title value
     */
    public String getTitle() {
        if (title == null) {
            return "";
        } else {
            return title;
        }
    }

    /**
     *  Returns a shortened version of image name that does not include spaces or a
     *  file name extension.
     *
     *@return    The shortTitle value
     */
    public String getShortTitle() {
        String title = getTitle();
        int index = title.indexOf(' ');

        if (index > -1) {
            title = title.substring(0, index);
        }
        index = title.lastIndexOf('.');
        if (index > 0) {
            title = title.substring(0, index);
        }
        return title;
    }

    /**
     *  Sets the image name.
     *
     *@param  title  The new title value
     */
    public void setTitle(String title) {
        if (title == null) {
            return;
        }
        if (win != null) {
            if (ij != null) {
                Menus.updateWindowMenuItem(this.title, title);
            }
            String scale = "";
            double magnification = win.getCanvas().getMagnification();

            if (magnification != 1.0) {
                double percent = magnification * 100.0;
                int digits = percent > 100.0 || percent == (int) percent ? 0 : 1;

                scale = " (" + IJ.d2s(percent, digits) + "%)";
            }
            win.setTitle(title + scale);
        }
        this.title = title;
    }

    /**
     *  Gets the width attribute of the ImagePlus object
     *
     *@return    The width value
     */
    public int getWidth() {
        return width;
    }

    /**
     *  Gets the height attribute of the ImagePlus object
     *
     *@return    The height value
     */
    public int getHeight() {
        return height;
    }

    /**
     *  If this is a stack, returns the number of slices, else returns 1.
     *
     *@return    The stackSize value
     */
    public int getStackSize() {
        if (stack == null) {
            return 1;
        } else {
            int slices = stack.getSize();
            //if (compositeImage) slices /= nChannels;
            if (slices <= 0) {
                slices = 1;
            }
            return slices;
        }
    }

    /**
     *  If this is a stack, returns the actual number of images in the stack, else
     *  returns 1.
     *
     *@return    The imageStackSize value
     */
    public int getImageStackSize() {
        if (stack == null) {
            return 1;
        } else {
            int slices = stack.getSize();

            if (slices == 0) {
                slices = 1;
            }
            return slices;
        }
    }

    /**
     *  Sets the 3rd, 4th and 5th dimensions, where <code>nChannels</code>*<code>nSlices</code>
     *  *<code>nFrames</code> must be equal to the stack size.
     *
     *@param  nChannels  The new dimensions value
     *@param  nSlices    The new dimensions value
     *@param  nFrames    The new dimensions value
     */
    public void setDimensions(int nChannels, int nSlices, int nFrames) {
        if (nChannels * nSlices * nFrames != getImageStackSize() && ip != null) {
            //throw new IllegalArgumentException("channels*slices*frames!=stackSize");
            nChannels = 1;
            nSlices = getImageStackSize();
            nFrames = 1;
            if (is5D()) {
                setOpenAsHyperVolume(false);
                new StackWindow(this);
            }
        }
        boolean updateWin = is5D() && (this.nChannels != nChannels || this.nSlices != nSlices || this.nFrames != nFrames);

        this.nChannels = nChannels;
        this.nSlices = nSlices;
        this.nFrames = nFrames;
        if (updateWin) {
            if (nSlices != getImageStackSize()) {
                setOpenAsHyperVolume(true);
            }
            new StackWindow(this);
        }
        //IJ.log("setDimensions: "+ nChannels+"  "+nSlices+"  "+nFrames);
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    boolean is5D() {
        return win != null && win instanceof StackWindow && ((StackWindow) win).is5D();
    }

    /**
     *  Returns the number of channels.
     *
     *@return    The nChannels value
     */
    public int getNChannels() {
        verifyDimensions();
        return nChannels;
    }

    /**
     *  Returns the image depth (number of z-slices).
     *
     *@return    The nSlices value
     */
    public int getNSlices() {
        //IJ.log("getNSlices: "+ nChannels+"  "+nSlices+"  "+nFrames);
        verifyDimensions();
        return nSlices;
    }

    /**
     *  Returns the number of frames (time-points).
     *
     *@return    The nFrames value
     */
    public int getNFrames() {
        verifyDimensions();
        return nFrames;
    }

    /**
     *  Returns the dimensions of this image (width, height, nChannels, nSlices,
     *  nFrames) as a 5 element int array.
     *
     *@return    The dimensions value
     */
    public int[] getDimensions() {
        verifyDimensions();

        int[] d = new int[5];

        d[0] = width;
        d[1] = height;
        d[2] = nChannels;
        d[3] = nSlices;
        d[4] = nFrames;
        return d;
    }

    /**
     *  Description of the Method
     */
    void verifyDimensions() {
        int stackSize = getImageStackSize();

        if (nSlices == 1) {
            if (nChannels > 1 && nFrames == 1) {
                nChannels = stackSize;
            } else if (nFrames > 1 && nChannels == 1) {
                nFrames = stackSize;
            }
        }
        if (nChannels * nSlices * nFrames != stackSize) {
            nSlices = stackSize;
            nChannels = 1;
            nFrames = 1;
        }
    }

    /**
     *  Returns the current image type (ImagePlus.GRAY8, ImagePlus.GRAY16,
     *  ImagePlus.GRAY32, ImagePlus.COLOR_256 or ImagePlus.COLOR_RGB).
     *
     *@return    The type value
     *@see       #getBitDepth
     */
    public int getType() {
        return imageType;
    }

    /**
     *  Returns the bit depth, 8, 16, 24 (RGB) or 32. RGB images actually use 32
     *  bits per pixel.
     *
     *@return    The bitDepth value
     */
    public int getBitDepth() {
        int bitDepth = 0;

        switch (imageType) {
            case GRAY8:
            case COLOR_256:
                bitDepth = 8;
                break;
            case GRAY16:
                bitDepth = 16;
                break;
            case GRAY32:
                bitDepth = 32;
                break;
            case COLOR_RGB:
                bitDepth = 24;
                break;
        }
        return bitDepth;
    }

    /**
     *  Sets the type attribute of the ImagePlus object
     *
     *@param  type  The new type value
     */
    protected void setType(int type) {
        if ((type < 0) || (type > COLOR_RGB)) {
            return;
        }
        int previousType = imageType;

        imageType = type;
        if (imageType != previousType) {
            if (win != null) {
                Menus.updateMenus();
            }
            /*
             *  EU_HOU CHANGES
             */
            if (calibration != null || globalCalibration != null) {
                getCalibration().setImage(this);
            }
            /*
             *  EU_HOU END
             */
        }
    }

    /**
     *  Adds a key-value pair to this image's properties.
     *
     *@param  key    The new property value
     *@param  value  The new property value
     */
    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
    }

    /**
     *  Returns the property associated with 'key'. May return null.
     *
     *@param  key  Description of the Parameter
     *@return      The property value
     */
    public Object getProperty(String key) {
        if (properties == null) {
            return null;
        } else {
            return properties.get(key);
        }
    }

    /**
     *  Returns this image's Properties. May return null.
     *
     *@return    The properties value
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     *  Creates a LookUpTable object corresponding to this image.
     *
     *@return    Description of the Return Value
     */
    public LookUpTable createLut() {
        return new LookUpTable(getProcessor().getColorModel());
    }

    /**
     *  Returns true is this image uses an inverting LUT that displays zero as
     *  white and 255 as black.
     *
     *@return    The invertedLut value
     */
    public boolean isInvertedLut() {
        if (ip == null) {
            if (img == null) {
                return false;
            }
            setupProcessor();
        }
        return ip.isInvertedLut();
    }
    private int[] pvalue = new int[4];

    /**
     *  Returns the pixel value at (x,y) as a 4 element array. Grayscale values are
     *  retuned in the first element. RGB values are returned in the first 3
     *  elements. For indexed color images, the RGB values are returned in the
     *  first 3 three elements and the index (0-255) is returned in the last.
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     *@return    The pixel value
     */
    public int[] getPixel(int x, int y) {
        pvalue[0] = pvalue[1] = pvalue[2] = pvalue[3] = 0;
        if (img == null) {
            return pvalue;
        }
        switch (imageType) {
            case GRAY8:
            case COLOR_256:
                int index;

                if (ip != null) {
                    index = ip.getPixel(x, y);
                } else {
                    byte[] pixels8;
                    PixelGrabber pg = new PixelGrabber(img, x, y, 1, 1, false);

                    try {
                        pg.grabPixels();
                    } catch (InterruptedException e) {
                        return pvalue;
                    }
                    ;
                    pixels8 = (byte[]) (pg.getPixels());
                    index = pixels8 != null ? pixels8[0] & 0xff : 0;
                }
                if (imageType != COLOR_256) {
                    pvalue[0] = index;
                    return pvalue;
                }
                pvalue[3] = index;
            // fall through to get rgb values
            case COLOR_RGB:
                int[] pixels32 = new int[1];

                if (win == null) {
                    break;
                }
                PixelGrabber pg = new PixelGrabber(img, x, y, 1, 1, pixels32, 0, width);

                try {
                    pg.grabPixels();
                } catch (InterruptedException e) {
                    return pvalue;
                }
                ;

                int c = pixels32[0];
                int r = (c & 0xff0000) >> 16;
                int g = (c & 0xff00) >> 8;
                int b = c & 0xff;

                pvalue[0] = r;
                pvalue[1] = g;
                pvalue[2] = b;
                break;
            case GRAY16:
            case GRAY32:
                if (ip != null) {
                    pvalue[0] = ip.getPixel(x, y);
                }
                break;
        }
        return pvalue;
    }

    /**
     *  Returns an empty image stack that has the same width, height and color
     *  table as this image.
     *
     *@return    Description of the Return Value
     */
    public ImageStack createEmptyStack() {
        ColorModel cm;

        if (ip != null) {
            cm = ip.getColorModel();
        } else {
            cm = createLut().getColorModel();
        }
        return new ImageStack(width, height, cm);
    }

    /**
     *  Returns the image stack. The stack may have only one slice. After adding or
     *  removing slices, call <code>setStack()</code> to update the image and the
     *  window that is displaying it.
     *
     *@return    The stack value
     *@see       #setStack
     */
    public ImageStack getStack() {
        ImageStack s;

        if (stack == null) {
            s = createEmptyStack();

            ImageProcessor ip2 = getProcessor();
            String info = (String) getProperty("Info");
            String label = info != null ? getTitle() + "\n" + info : null;

            s.addSlice(label, ip2);
            s.update(ip2);
        } else {
            s = stack;
            s.update(ip);
        }
        if (roi != null) {
            s.setRoi(roi.getBounds());
        }
        return s;
    }

    /**
     *  Returns the base image stack.
     *
     *@return    The imageStack value
     */
    public ImageStack getImageStack() {
        if (stack == null) {
            return getStack();
        } else {
            stack.update(ip);
            return stack;
        }
    }

    /**
     *  Returns the current stack slice number or 1 if this is a single image.
     *
     *@return    The currentSlice value
     */
    public int getCurrentSlice() {
        if (currentSlice < 1) {
            currentSlice = 1;
        }
        if (currentSlice > getStackSize()) {
            currentSlice = getStackSize();
        }
        return currentSlice;
    }

    /**
     *  Description of the Method
     */
    public void killStack() {
        stack = null;
        trimProcessor();
    }

    /**
     *  Activates the specified slice. The index must be >= 1 and <= N, where N in
     *  the number of slices in the stack. Does nothing if this ImagePlus does not
     *  use a stack.
     *
     *@param  index  The new slice value
     */
    public synchronized void setSlice(int index) {
        if (stack == null || index == currentSlice) {
            updateAndRepaintWindow();
            return;
        }
        if (index >= 1 && index <= stack.getSize()) {
            Roi roi = getRoi();

            if (roi != null) {
                roi.endPaste();
            }
            if (isProcessor()) {
                stack.setPixels(ip.getPixels(), currentSlice);
            }
            ip = getProcessor();
            currentSlice = index;

            Object pixels = stack.getPixels(currentSlice);

            if (pixels != null) {
                ip.setSnapshotPixels(null);
                ip.setPixels(pixels);
            }
            if (win != null && win instanceof StackWindow) {
                ((StackWindow) win).updateSliceSelector();
            }
            if (IJ.altKeyDown()) {
                if (imageType == GRAY16 || imageType == GRAY32) {
                    ip.resetMinAndMax();
                    IJ.showStatus(index + ": min=" + ip.getMin() + ", max=" + ip.getMax());
                }
                ContrastAdjuster.update();
            }
            if (!Interpreter.isBatchMode()) {
                updateAndRepaintWindow();
            }
        }
    }

    /**
     *  Obsolete
     */
    void undoFilter() {
        if (ip != null) {
            ip.reset();
            updateAndDraw();
        }
    }

    /**
     *  Gets the roi attribute of the ImagePlus object
     *
     *@return    The roi value
     */
    public Roi getRoi() {
        return roi;
    }

    /**
     *  Assigns the specified ROI to this image and displays it. Any existing ROI
     *  is deleted if <code>roi</code> is null or its width or height is zero.
     *
     *@param  newRoi  The new roi value
     */
    public void setRoi(Roi newRoi) {
        if (newRoi == null) {
            killRoi();
            return;
        }
        if (newRoi.isVisible()) {
            newRoi = (Roi) newRoi.clone();
            if (newRoi == null) {
                killRoi();
                return;
            }
        }
        Rectangle bounds = newRoi.getBounds();

        if (bounds.width == 0 && bounds.height == 0 && !(newRoi.getType() == Roi.POINT || newRoi.getType() == Roi.LINE)) {
            killRoi();
            return;
        }
        roi = newRoi;
        if (ip != null) {
            ip.setMask(null);
            if (roi.isArea()) {
                ip.setRoi(bounds);
            } else {
                ip.resetRoi();
            }
        }
        roi.setImage(this);
        draw();
    }


    /**
     *  Creates a rectangular selection.
     *
     *@param  x       The new roi value
     *@param  y       The new roi value
     *@param  width   The new roi value
     *@param  height  The new roi value
     */
    public void setRoi(int x, int y, int width, int height) {
        setRoi(new Rectangle(x, y, width, height));
    }

    /**
     *  Creates a rectangular selection.
     *
     *@param  r  The new roi value
     */
    public void setRoi(Rectangle r) {
        setRoi(new Roi(r.x, r.y, r.width, r.height));
    }

    /**
     *  Starts the process of creating a new selection, where sx and sy are the
     *  starting screen coordinates. The selection type is determined by which tool
     *  in the tool bar is active. The user interactively sets the selection size
     *  and shape.
     *
     *@param  sx  Description of the Parameter
     *@param  sy  Description of the Parameter
     */
    public void createNewRoi(int sx, int sy) {
        killRoi();
        switch (Toolbar.getToolId()) {
            case Toolbar.RECTANGLE:
                roi = new Roi(sx, sy, this);
                break;
            case Toolbar.OVAL:
                roi = new OvalRoi(sx, sy, this);
                break;
            case Toolbar.POLYGON:
            case Toolbar.POLYLINE:
                //EU_HOU Changes
                //case Toolbar.ANGLE:

                roi = new PolygonRoi(sx, sy, this);
                break;
            //EU_HOU Changes
            //case Toolbar.FREEROI:
            case Toolbar.FREELINE:
                roi = new FreehandRoi(sx, sy, this);
                break;
            case Toolbar.LINE:
                roi = new Line(sx, sy, this);
                break;
            //EU_HOU Changes
						/*
             *  case Toolbar.TEXT:
             *  roi = new TextRoi(sx, sy, this);
             *  break;
             *  case Toolbar.POINT:
             *  roi = new PointRoi(sx, sy, this);
             *  if (Prefs.pointAutoMeasure || Prefs.pointAutoNextSlice) {
             *  IJ.run("Measure");
             *  }
             *  if (Prefs.pointAutoNextSlice && getStackSize() > 1) {
             *  IJ.run("Next Slice [>]");
             *  killRoi();
             *  }
             *  break;
             */
        }
    }

    /**
     *  Deletes the current region of interest. Makes a copy of the current ROI so
     *  it can be recovered by the Edit/Restore Selection command.
     */
    public void killRoi() {
        if (roi != null) {
            saveRoi();
            roi = null;
            if (ip != null) {
                ip.resetRoi();
            }
            draw();
        }
    }

    /**
     *  Description of the Method
     */
    public void saveRoi() {
        if (roi != null) {
            roi.endPaste();

            Rectangle r = roi.getBounds();

            if (r.width > 0 && r.height > 0) {
                Roi.previousRoi = (Roi) roi.clone();
                if (IJ.debugMode) {
                    //EU_HOU Bundle
                    IJ.log("saveRoi: " + roi);
                }
            }
        }
    }

    /**
     *  Description of the Method
     */
    public void restoreRoi() {
        if (Roi.previousRoi != null) {
            Roi pRoi = Roi.previousRoi;
            Rectangle r = pRoi.getBounds();

            if (r.width <= width || r.height <= height) {// will it fit in this image?
                roi = (Roi) pRoi.clone();
                roi.setImage(this);
                if (r.x >= width || r.y >= height || (r.x + r.width) <= 0 || (r.y + r.height) <= 0) {// does it need to be moved?
                    roi.setLocation((width - r.width) / 2, (height - r.height) / 2);
                } else if (r.width == width && r.height == height) {// is it the same size as the image
                    roi.setLocation(0, 0);
                }
                draw();
            }
        }
    }

    /**
     *  Implements the File/Revert command.
     */
    public void revert() {
        if (getStackSize() > 1) {// can't revert stacks
            return;
        }
        FileInfo fi = getOriginalFileInfo();
        boolean isFileInfo = fi != null && fi.fileFormat != FileInfo.UNKNOWN;

        if (!(isFileInfo || url != null)) {
            return;
        }
        if (ij != null && changes && isFileInfo && !Interpreter.isBatchMode() && !IJ.macroRunning() && !IJ.altKeyDown()) {
            //EU_HOU Bundle
            if (!IJ.showMessageWithCancel("Revert?", "Revert to saved version of\n\"" + getTitle() + "\"?")) {
                return;
            }
        }
        if (roi != null) {
            roi.endPaste();
        }
        trimProcessor();
        if (isFileInfo && !(url != null && (fi.directory == null || fi.directory.equals("")))) {
            new FileOpener(fi).revertToSaved(this);
        } else if (url != null) {
            //EU_HOU Bundle
            IJ.showStatus("Loading: " + url);

            Opener opener = new Opener();

            try {
                ImagePlus imp = opener.openURL(url);

                if (imp != null) {
                    setProcessor(null, imp.getProcessor());
                }
            } catch (Exception e) {
            }
            if (getType() == COLOR_RGB && getTitle().endsWith(".jpg")) {
                Opener.convertGrayJpegTo8Bits(this);
            }
        }
        if (Prefs.useInvertingLut && getBitDepth() == 8 && ip != null && !ip.isInvertedLut() && !ip.isColorLut()) {
            invertLookupTable();
        }
        if (getProperty("FHT") != null) {
            properties.remove("FHT");
            if (getTitle().startsWith("FFT of ")) {
                setTitle(getTitle().substring(6));
            }
        }
        ContrastAdjuster.update();
        repaintWindow();
        IJ.showStatus("");
        changes = false;
    }

    /**
     *  Returns a FileInfo object containing information, including the pixel
     *  array, needed to save this image. Use getOriginalFileInfo() to get a copy
     *  of the FileInfo object used to open the image.
     *
     *@return    The fileInfo value
     *@see       ij.io.FileInfo
     *@see       #getOriginalFileInfo
     *@see       #setFileInfo
     */
    public FileInfo getFileInfo() {
        FileInfo fi = new FileInfo();

        fi.width = width;
        fi.height = height;
        fi.nImages = getStackSize();
        if (compositeImage) {
            fi.nImages = getImageStackSize();
        }
        fi.whiteIsZero = isInvertedLut();
        fi.intelByteOrder = false;
        setupProcessor();
        if (fi.nImages == 1) {
            fi.pixels = ip.getPixels();
        } else {
            fi.pixels = stack.getImageArray();
        }

        Calibration cal = getCalibration();

        if (cal.scaled()) {
            fi.pixelWidth = cal.pixelWidth;
            fi.pixelHeight = cal.pixelHeight;
            fi.unit = cal.getUnit();
        }
        if (fi.nImages > 1) {
            fi.pixelDepth = cal.pixelDepth;
        }
        fi.frameInterval = cal.frameInterval;
        if (cal.calibrated()) {
            fi.calibrationFunction = cal.getFunction();
            fi.coefficients = cal.getCoefficients();
            fi.valueUnit = cal.getValueUnit();
        }
        switch (imageType) {
            case GRAY8:
            case COLOR_256:
                LookUpTable lut = createLut();

                if (imageType == COLOR_256 || !lut.isGrayscale()) {
                    fi.fileType = FileInfo.COLOR8;
                } else {
                    fi.fileType = FileInfo.GRAY8;
                }
                fi.lutSize = lut.getMapSize();
                fi.reds = lut.getReds();
                fi.greens = lut.getGreens();
                fi.blues = lut.getBlues();
                break;
            case GRAY16:
                if (compositeImage && fi.nImages == 3) {
                    fi.fileType = fi.RGB48;
                } else {
                    fi.fileType = fi.GRAY16_UNSIGNED;
                }
                break;
            case GRAY32:
                fi.fileType = fi.GRAY32_FLOAT;
                break;
            case COLOR_RGB:
                fi.fileType = fi.RGB;
                break;
            default:
        }
        return fi;
    }


    /*
     *  EU_HOU ADD
     */
    /**
     *  Sets the headerComplementary attribute of the ImagePlus object
     *
     *@param  s  The new headerComplementary value
     */
    public void setHeaderComplementary(String s) {
        ComplementaryHeader = s;
    }

    /**
     *  Gets the headerComplementary attribute of the ImagePlus object
     *
     *@return    The headerComplementary value
     */
    public String getHeaderComplementary() {
        if (ComplementaryHeader == null) {
            return "";
        } else {
            return ComplementaryHeader;
        }
    }


    /*
     *  EU_HOU END
     */
    /**
     *  Returns the FileInfo object that was used to open this image. Returns null
     *  for images created using the File/New command.
     *
     *@return    The originalFileInfo value
     *@see       ij.io.FileInfo
     *@see       #getFileInfo
     */
    public FileInfo getOriginalFileInfo() {
        if (fileInfo == null & url != null) {
            fileInfo = new FileInfo();
            fileInfo.width = width;
            fileInfo.height = height;
            fileInfo.url = url;
            fileInfo.directory = null;
        }
        return fileInfo;
    }

    /**
     *  Used by ImagePlus to monitor loading of images.
     *
     *@param  img    Description of the Parameter
     *@param  flags  Description of the Parameter
     *@param  x      Description of the Parameter
     *@param  y      Description of the Parameter
     *@param  w      Description of the Parameter
     *@param  h      Description of the Parameter
     *@return        Description of the Return Value
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
        imageUpdateY = y;
        imageUpdateW = w;
        if ((flags & ERROR) != 0) {
            errorLoadingImage = true;
            return false;
        }
        imageLoaded = (flags & (ALLBITS | FRAMEBITS | ABORT)) != 0;
        return !imageLoaded;
    }

    /**
     *  Sets the image arrays to null to help the garbage collector do its job.
     *  Does nothing if the image is locked or a setIgnoreFlush(true) call has been
     *  made.
     */
    public synchronized void flush() {
        notifyListeners(CLOSED);
        if (locked || ignoreFlush) {
            return;
        }
        if (ip != null) {
            ip.setPixels(null);
            ip = null;
        }
        if (roi != null) {
            roi.setImage(null);
        }
        if (stack != null) {
            Object[] arrays = stack.getImageArray();

            if (arrays != null) {
                for (int i = 0; i < arrays.length; i++) {
                    arrays[i] = null;
                }
            }
        }
        img = null;
        System.gc();
    }

    /**
     *  Set <code>ignoreFlush true</code> to not have the pixel data set to null
     *  when the window is closed.
     *
     *@param  ignoreFlush  The new ignoreFlush value
     */
    public void setIgnoreFlush(boolean ignoreFlush) {
        this.ignoreFlush = ignoreFlush;
    }

    /**
     *  Returns a new ImagePlus with this ImagePlus' attributes (e.g. spatial
     *  scale), but no image.
     *
     *@return    Description of the Return Value
     */
    public ImagePlus createImagePlus() {
        ImagePlus imp2 = new ImagePlus();

        imp2.setType(getType());
        imp2.setCalibration(getCalibration());
        return imp2;
    }

    /**
     *  Copies the calibration of the specified image to this image.
     *
     *@param  imp  Description of the Parameter
     */
    public void copyScale(ImagePlus imp) {
        if (imp != null && globalCalibration == null) {
            setCalibration(imp.getCalibration());
        }
    }

    /**
     *  Calls System.currentTimeMillis() to save the current time so it can be
     *  retrieved later using getStartTime() to calculate the elapsed time of an
     *  operation.
     */
    public void startTiming() {
        startTime = System.currentTimeMillis();
    }

    /**
     *  Returns the time in milliseconds when startTiming() was last called.
     *
     *@return    The startTime value
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     *  Returns this image's calibration.
     *
     *@return    The calibration value
     */
    public Calibration getCalibration() {
        //IJ.log("getCalibration: "+globalCalibration+" "+calibration);
        if (globalCalibration != null) {
            Calibration gc = globalCalibration.copy();

            gc.setImage(this);
            return gc;
        } else {
            if (calibration == null) {
                calibration = new Calibration(this);
            }
            return calibration;
        }
    }

    /**
     *  Sets this image's calibration.
     *
     *@param  cal  The new calibration value
     */
    public void setCalibration(Calibration cal) {
        //IJ.write("setCalibration: "+cal);
        if (cal == null) {
            calibration = null;
        } else {
            calibration = cal.copy();
            calibration.setImage(this);
        }
    }

    /**
     *  Sets the system-wide calibration.
     *
     *@param  global  The new globalCalibration value
     */
    public void setGlobalCalibration(Calibration global) {
        //IJ.log("setGlobalCalibration ("+getTitle()+"): "+global);
        if (global == null) {
            globalCalibration = null;
        } else {
            globalCalibration = global.copy();
        }
    }

    /**
     *  Returns the system-wide calibration, or null.
     *
     *@return    The globalCalibration value
     */
    public Calibration getGlobalCalibration() {
        return globalCalibration;
    }

    /**
     *  Returns this image's local calibration, ignoring the "Global" calibration
     *  flag.
     *
     *@return    The localCalibration value
     */
    public Calibration getLocalCalibration() {
        if (calibration == null) {
            calibration = new Calibration(this);
        }
        return calibration;
    }

    /**
     *  Displays the cursor coordinates and pixel value in the status bar. Called
     *  by ImageCanvas when the mouse moves. Can be overridden by ImagePlus
     *  subclasses.
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     */
    public void mouseMoved(int x, int y) {
        if (ij != null) {
            ij.showStatus(getLocationAsString(x, y) + getValueAsString(x, y));
        }
        savex = x;
        savey = y;
    }


    /*
     *  EU_HOU Add
     */
    /**
     *  EU_HOU_rb 27/5/05 Called by ImageCanvas when the mouse is clicked . Can be
     *  overridden by ImagePlus subclasses.
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     */
    public void mouseClicked(int x, int y) {
        IJ.log(x+", "+y);
    }
    /*
     *  EU_HOU END
     */
    private int savex, savey;

    /**
     *  Redisplays the (x,y) coordinates and pixel value (which may have changed)
     *  in the status bar. Called by the Next Slice and Previous Slice commands to
     *  update the z-coordinate and pixel value.
     */
    public void updateStatusbarValue() {
        IJ.showStatus(getLocationAsString(savex, savey) + getValueAsString(savex, savey));
    }

    /**
     *  Gets the fFTLocation attribute of the ImagePlus object
     *
     *@param  x    Description of the Parameter
     *@param  y    Description of the Parameter
     *@param  cal  Description of the Parameter
     *@return      The fFTLocation value
     */
    String getFFTLocation(int x, int y, Calibration cal) {
        double center = width / 2.0;
        double r = Math.sqrt((x - center) * (x - center) + (y - center) * (y - center));

        if (r < 1.0) {
            r = 1.0;
        }
        double theta = Math.atan2(y - center, x - center);

        theta = theta * 180.0 / Math.PI;
        if (theta < 0) {
            theta = 360.0 + theta;
        }
        String s = "r=";

        /*
         *  EU_HOU CHANGES
         */
        y = height - 1 - y;
        /*
         *  EU_HOU END
         */
        if (cal.scaled()) {
            s += IJ.d2s((width / r) * cal.pixelWidth, 2) + " " + cal.getUnit() + "/c (" + IJ.d2s(r, 0) + ")";
        } else {
            s += IJ.d2s(width / r, 2) + " p/c (" + IJ.d2s(r, 0) + ")";
        }
        s += ", theta= " + IJ.d2s(theta, 2) + IJ.degreeSymbol;
        return s;
    }

    /**
     *  Converts the current cursor location to a string.
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     *@return    The locationAsString value
     */
    public String getLocationAsString(int x, int y) {
        Calibration cal = getCalibration();

        if (getProperty("FHT") != null) {
            /*
             *  EU_HOU CHANGES
             */
            return getFFTLocation(x, y, cal);
            /*
             *  EU_HOU END
             */
        }
        //y = Analyzer.updateY(y, height);
        if (!IJ.altKeyDown()) {
            String s = " x=" + d2s(cal.getX(x)) + ", y=" + d2s(cal.getY(y, height));

            if (getStackSize() > 1) {
                s += ", z=" + d2s(cal.getZ(getCurrentSlice() - 1));
            }
            return s;
        } else {
            String s = " x=" + x + ", y=" + y;

            if (getStackSize() > 1) {
                s += ", z=" + (getCurrentSlice() - 1);
            }
            return s;
        }
    }

    /**
     *  Description of the Method
     *
     *@param  n  Description of the Parameter
     *@return    Description of the Return Value
     */
    private String d2s(double n) {
        return n == (int) n ? Integer.toString((int) n) : IJ.d2s(n);
    }

    /**
     *  Gets the valueAsString attribute of the ImagePlus object
     *
     *@param  x  Description of the Parameter
     *@param  y  Description of the Parameter
     *@return    The valueAsString value
     */
    private String getValueAsString(int x, int y) {
        if (win != null && win instanceof PlotWindow) {
            return "";
        }
        Calibration cal = getCalibration();
        int[] v = getPixel(x, y);
        int type = getType();

        switch (type) {
            case GRAY8:
            case GRAY16:
            case COLOR_256:
                if (type == COLOR_256) {
                    if (cal.getCValue(v[3]) == v[3]) {// not calibrated
                        //EU_HOU Bundle
                        return (" " + IJ.getOpBundle().getString("Index") + "=" + v[3] + " " + IJ.getOpBundle().getString("Value") + "=" + v[0] + "," + v[1] + "," + v[2]);
                    } else {
                        v[0] = v[3];
                    }
                }
                double cValue = cal.getCValue(v[0]);

                if (cValue == v[0]) {
                    //EU_HOU Bundle
                    return (" " + IJ.getOpBundle().getString("Value") + "=" + v[0]);
                } else {
                    //EU_HOU Bundle
                    return (" " + IJ.getOpBundle().getString("Value") + "=" + IJ.d2s(cValue) + " (" + v[0] + ")");
                }
            case GRAY32:
                //EU_HOU Bundle
                return (" " + IJ.getOpBundle().getString("Value") + "=" + Float.intBitsToFloat(v[0]));
            case COLOR_RGB:
                //EU_HOU Bundle
                return (" " + IJ.getOpBundle().getString("Value") + "=" + v[0] + "," + v[1] + "," + v[2]);
            default:
                return ("");
        }
    }

    /**
     *  Copies the contents of the current selection to the internal clipboard.
     *  Copies the entire image if there is no selection. Also clears the selection
     *  if <code>cut</code> is true.
     *
     *@param  cut  Description of the Parameter
     */
    public void copy(boolean cut) {
        Roi roi = getRoi();

        if (roi != null && !roi.isArea()) {
            //EU_HOU Bundle
            IJ.error("Cut/Copy", "The Cut and Copy commands require\n"
                    + "an area selection, or no selection.");
            return;
        }
        String msg = (cut) ? "Cut" : "Copy";

        IJ.showStatus(msg + "ing...");

        ImageProcessor ip = getProcessor();
        ImageProcessor ip2;
        Roi roi2 = null;

        ip2 = ip.crop();
        /*
         *  EU_HOU CHANGES
         */
        //if (roi != null && roi.getType() != Roi.RECTANGLE) {
		/*
         *  EU_HOU END
         */
        if (roi != null && roi.getType() != roi.LINE) {
            roi2 = (Roi) roi.clone();

            Rectangle r = roi.getBounds();

            if (r.x < 0 || r.y < 0) {
                roi2.setLocation(Math.min(r.x, 0), Math.min(r.y, 0));
            }
        }
        //EU_HOU Bundle
        clipboard = new ImagePlus("Clipboard", ip2);
        if (roi2 != null) {
            clipboard.setRoi(roi2);
        }
        if (cut) {
            ip.snapshot();
            ip.setColor(Toolbar.getBackgroundColor());
            ip.fill();
            /*
             *  EU_HOU CHANGES
             */
            //if (roi != null && roi.getType() != Roi.RECTANGLE) {
			/*
             *  EU_HOU END
             */
            if (roi != null && roi.getType() != roi.LINE) {
                getMask();
                ip.reset(ip.getMask());
            }
            setColor(Toolbar.getForegroundColor());
            Undo.setup(Undo.FILTER, this);
            updateAndDraw();
        }
        int bytesPerPixel = 1;

        switch (clipboard.getType()) {
            case ImagePlus.GRAY16:
                bytesPerPixel = 2;
                break;
            case ImagePlus.GRAY32:
            case ImagePlus.COLOR_RGB:
                bytesPerPixel = 4;
        }
        //Roi roi3 = clipboard.getRoi();
        //IJ.log("copy: "+clipboard +" "+ "roi3="+(roi3!=null?""+roi3:""));
        IJ.showStatus(msg + ": " + (clipboard.getWidth() * clipboard.getHeight() * bytesPerPixel) / 1024 + "k");
    }

    /**
     *  Inserts the contents of the internal clipboard into the active image. If
     *  there is a selection the same size as the image on the clipboard, the image
     *  is inserted into that selection, otherwise the selection is inserted into
     *  the center of the image.
     */
    public void paste() {
        if (clipboard == null) {
            return;
        }
        int cType = clipboard.getType();
        int iType = getType();

        boolean sameType = false;

        if ((cType == ImagePlus.GRAY8 || cType == ImagePlus.COLOR_256) && (iType == ImagePlus.GRAY8 || iType == ImagePlus.COLOR_256)) {
            sameType = true;
        } else if ((cType == ImagePlus.COLOR_RGB || cType == ImagePlus.GRAY8 || cType == ImagePlus.COLOR_256) && iType == ImagePlus.COLOR_RGB) {
            sameType = true;
        } else if (cType == ImagePlus.GRAY16 && iType == ImagePlus.GRAY16) {
            sameType = true;
        } else if (cType == ImagePlus.GRAY32 && iType == ImagePlus.GRAY32) {
            sameType = true;
        }
        if (!sameType) {
            //EU_HOU Bundle
            IJ.error("Images must be the same type to paste.");
            return;
        }
        int w = clipboard.getWidth();
        int h = clipboard.getHeight();
        Roi cRoi = clipboard.getRoi();
        Rectangle r = null;
        Roi roi = getRoi();

        if (roi != null) {
            r = roi.getBounds();
        }
        if (w == width && h == height && (r == null || w != r.width || h != r.height)) {
            setRoi(0, 0, width, height);
            roi = getRoi();
            r = roi.getBounds();
        }
        if (r == null || (r != null && (w != r.width || h != r.height))) {
            // create a new roi centered on visible part of image
            ImageCanvas ic = null;

            if (win != null) {
                ic = win.getCanvas();
            }
            Rectangle srcRect = ic != null ? ic.getSrcRect() : new Rectangle(0, 0, width, height);
            int xCenter = srcRect.x + srcRect.width / 2;
            int yCenter = srcRect.y + srcRect.height / 2;

            /*
             *  EU_HOU CHANGES
             */
            //if (cRoi != null && cRoi.getType() != Roi.RECTANGLE) {
			/*
             *  EU_HOU END
             */
            if (cRoi != null && cRoi.getType() != Roi.LINE) {
                cRoi.setImage(this);
                cRoi.setLocation(xCenter - w / 2, yCenter - h / 2);
                setRoi(cRoi);
            } else {
                setRoi(xCenter - w / 2, yCenter - h / 2, w, h);
            }
            roi = getRoi();
        }
        if (IJ.macroRunning()) {
            //non-interactive paste
            int pasteMode = Roi.getCurrentPasteMode();
            //boolean nonRect = roi.getType() != Roi.RECTANGLE;
            boolean nonRect = roi.getType() != Roi.LINE;
            ImageProcessor ip = getProcessor();

            if (nonRect) {
                ip.snapshot();
            }
            r = roi.getBounds();
            ip.copyBits(clipboard.getProcessor(), r.x, r.y, pasteMode);
            if (nonRect) {
                ip.reset(getMask());
            }
            updateAndDraw();
            //killRoi();
        } else if (roi != null) {
            roi.startPaste(clipboard);
            Undo.setup(Undo.PASTE, this);
        }
        changes = true;
    }

    /**
     *  Returns the internal clipboard or null if the internal clipboard is empty.
     *
     *@return    The clipboard value
     */
    public static ImagePlus getClipboard() {
        return clipboard;
    }

    /**
     *  Clears the internal clipboard.
     */
    public static void resetClipboard() {
        clipboard = null;
    }

    /**
     *  Description of the Method
     *
     *@param  id  Description of the Parameter
     */
    protected void notifyListeners(int id) {
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
                ImageListener listener = (ImageListener) listeners.elementAt(i);

                switch (id) {
                    case OPENED:
                        listener.imageOpened(this);
                        break;
                    case CLOSED:
                        listener.imageClosed(this);
                        break;
                    case UPDATED:
                        listener.imageUpdated(this);
                        break;
                }
            }
        }
    }

    /**
     *  Adds a feature to the ImageListener attribute of the ImagePlus class
     *
     *@param  listener  The feature to be added to the ImageListener attribute
     */
    public static void addImageListener(ImageListener listener) {
        listeners.addElement(listener);
    }

    /**
     *  Description of the Method
     *
     *@param  listener  Description of the Parameter
     */
    public static void removeImageListener(ImageListener listener) {
        listeners.removeElement(listener);
    }

    /**
     *  Returns 'true' if the image is locked.
     *
     *@return    The locked value
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     *  Sets the openAsHyperVolume attribute of the ImagePlus object
     *
     *@param  openAsHV  The new openAsHyperVolume value
     */
    public void setOpenAsHyperVolume(boolean openAsHV) {
        openAsHyperVolume = openAsHV;
    }

    /**
     *  Gets the openAsHyperVolume attribute of the ImagePlus object
     *
     *@return    The openAsHyperVolume value
     */
    public boolean getOpenAsHyperVolume() {
        return openAsHyperVolume;
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     *  Description of the Method
     *
     *@return    Description of the Return Value
     */
    public String toString() {
        return "imp[" + getTitle() + " " + width + "x" + height + "x" + getStackSize() + "]";
    }

}

