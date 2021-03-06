package ij.plugin;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.gui.*;

/**
 *  Converts a 2 or 3 slice stack to RGB.
 *
 *@author     Thomas
 *@created    3 decembre 2007
 */
public class RGBStackConverter implements PlugIn {

	/**
	 *  Main processing method for the RGBStackConverter object
	 *
	 *@param  arg  Description of the Parameter
	 */
	public void run(String arg) {
	ImagePlus imp = IJ.getImage();
	CompositeImage cimg = imp instanceof CompositeImage ? (CompositeImage) imp : null;
	int size = imp.getStackSize();
		if ((size < 2 || size > 3) && cimg == null) {
			//EU_HOU Bundle
			IJ.error(IJ.getPluginBundle().getString("StackorColorStackReqErr"));
			return;
		}
	int type = imp.getType();
		if (!(type == ImagePlus.GRAY8 || type == ImagePlus.GRAY16)) {
			//EU_HOU Bundle
			IJ.error("8-bit or 16-bit grayscale stack required");
			return;
		}
		if (!imp.lock()) {
			return;
		}
		Undo.reset();
	String title = imp.getTitle() + " (RGB)";
		if (cimg != null) {
		ImagePlus imp2 = imp.createImagePlus();
			imp2.setProcessor(title, new ColorProcessor(imp.getImage()));
			imp2.show();
		} else if (type == ImagePlus.GRAY16) {
			sixteenBitsToRGB(imp);
		} else {
		ImagePlus imp2 = imp.createImagePlus();
			imp2.setStack(title, imp.getStack());
		ImageConverter ic = new ImageConverter(imp2);
			ic.convertRGBStackToRGB();
			imp2.show();
		}
		imp.unlock();
	}


	/**
	 *  Description of the Method
	 *
	 *@param  imp  Description of the Parameter
	 */
	void sixteenBitsToRGB(ImagePlus imp) {
	Roi roi = imp.getRoi();
	int width;
	int height;
	Rectangle r;
		if (roi != null) {
			r = roi.getBounds();
			width = r.width;
			height = r.height;
		} else {
			r = new Rectangle(0, 0, imp.getWidth(), imp.getHeight());
		}
	ImageProcessor ip;
	ImageStack stack1 = imp.getStack();
	ImageStack stack2 = new ImageStack(r.width, r.height);
		for (int i = 1; i <= stack1.getSize(); i++) {
			ip = stack1.getProcessor(i);
			ip.setRoi(r);
		ImageProcessor ip2 = ip.crop();
			ip2 = ip2.convertToByte(true);
			stack2.addSlice(null, ip2);
		}
	ImagePlus imp2 = imp.createImagePlus();
		imp2.setStack(imp.getTitle() + " (RGB)", stack2);
	ImageConverter ic = new ImageConverter(imp2);
		ic.convertRGBStackToRGB();
		imp2.show();
	}

}

