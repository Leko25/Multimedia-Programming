
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

	BufferedImage imgOne;
	ImageDisplayUtility util = new ImageDisplayUtility();
	int width = 512;
	int height = 512;
	public static int FILTER_WINDOW_SIZE = 3;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	private BufferedImage scaleImageAnimate(BufferedImage prevImage, double scaleFactor) {
		int Width = prevImage.getWidth();
		int Height = prevImage.getHeight();
		double origin= Width/2.0;
		BufferedImage newImage = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < Height; y++) {
			for (int x = 0; x < Width; x++) {
				double x_origin = x - origin;
				double y_origin = y - origin;
				double x_transformed = x_origin/scaleFactor;
				double y_transformed = y_origin/scaleFactor;
				int x_new = (int) (x_transformed + origin);
				int y_new = (int) (y_transformed + origin);
				if (x_new < 0 || y_new < 0 || x_new >= Width|| y_new >= Height) {
					newImage.setRGB(x, y, util.hex2Decimal("000000"));
				}
				else {
					newImage.setRGB(x, y, prevImage.getRGB(x_new, y_new));
				}
			}
		}
		return newImage;
	}

	/**
	 *
	 * @param prevImage : Previous BufferedImage Object prior to this operation
	 * @param rotation : angle of rotation
	 * @return new BufferedImage that has been rotated according to args[2]
	 */
	private BufferedImage rotateImage(BufferedImage prevImage, double rotation){
		rotation = -Math.toRadians(rotation);
		if (rotation == 0.0){
			return prevImage;
		}

		//define image properties
		int Width = prevImage.getWidth();
		int Height = prevImage.getHeight();
		double origin = Width/2.0;

		BufferedImage rotatedImage = new BufferedImage(
				Width,
				Height,
				BufferedImage.TYPE_INT_RGB
		);

		for (int y = 0; y < Height; y++) {
			for (int x = 0; x < Width; x++) {
				double x_origin = x - origin;
				double y_origin = y - origin;
				double x_transformed = Math.cos(rotation) * x_origin - Math.sin(rotation) * y_origin;
				double y_transformed = Math.sin(rotation) * x_origin + Math.cos(rotation) * y_origin;
				int x_new = (int) (x_transformed + origin);
				int y_new = (int) (y_transformed + origin);
				if (x_new < 0 || y_new < 0 || x_new >= Width|| y_new >= Height) {
					rotatedImage.setRGB(x, y, util.hex2Decimal("000000"));
				}
				else {
					rotatedImage.setRGB(x, y, prevImage.getRGB(x_new, y_new));
				}
			}
		}
		return rotatedImage;
	}

	/**
	 * @param prevImage : Previous BufferedImage Object prior to this operation
	 * @param aliasFlag : alias
	 * @return Buffered Image with low pass filter
	 */
	private BufferedImage antiAlias(BufferedImage prevImage, int aliasFlag){
		if (aliasFlag == 1){
			int padding = (int) ((FILTER_WINDOW_SIZE - 1)/2);
			if (prevImage.getWidth() % FILTER_WINDOW_SIZE != 0) {
				prevImage = util.addPadding(prevImage); //pad Image
				padding = 0;
			}
			int prevWidth = prevImage.getWidth();
			int prevHeight = prevImage.getHeight();
			BufferedImage filteredImage = new BufferedImage(prevWidth, prevHeight, BufferedImage.TYPE_INT_RGB);
			int stop = util.getStop(prevWidth, prevHeight, padding);
			for (int y = 0; y < stop; y++) {
				for (int x = 0; x < stop; x++) {
					int r = 0, g = 0, b = 0;
					for (int i = 0; i < FILTER_WINDOW_SIZE; i++) {
						for (int j = 0; j < FILTER_WINDOW_SIZE; j++) {
							if (x + i < stop - 1  && y + j<= stop - 1){
								r += (prevImage.getRGB(x + i, y + j) >> 16) & 0x000000ff;
								g += (prevImage.getRGB(x + i, y + j) >> 8) & 0x000000ff;
								b += (prevImage.getRGB(x + i, y + j)) & 0x000000ff;
							}
						}
					}
					r /= (FILTER_WINDOW_SIZE * FILTER_WINDOW_SIZE);
					g /= (FILTER_WINDOW_SIZE * FILTER_WINDOW_SIZE);
					b /= (FILTER_WINDOW_SIZE * FILTER_WINDOW_SIZE);
					int pixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					filteredImage.setRGB(x, y, pixel);
				}
			}
			return filteredImage;
		}
		return prevImage;
	}

	/**
	 * Simultaneously Scale and Rotate the image according for frame[i] in numFrames
	 * @param prevImage : Previous BufferedImage Object prior to this operation
	 * @param numFrames : frames per second * transition (secs)
	 * @param rotation : Total angle of rotation
	 * @param scaleFactor : scaling factor
	 * @param aliasFlag : If true then low-pass filter is applied to the image
	 * @return an array of Buffered ImageFrames
	 */
	private BufferedImage[] animate(BufferedImage prevImage, int numFrames, double rotation, double scaleFactor, int aliasFlag) {
		BufferedImage[] imageFrames = new BufferedImage[numFrames];

		prevImage = aliasFlag == 1 ? antiAlias(prevImage, 1) : prevImage;

		for (int i = 0; i < numFrames; i++) {
			double scale_i = (double) (i * scaleFactor)/ ((double) (numFrames - 1));
			double angle_i = (double) (i * rotation)/ ((double) (numFrames-1));
			scale_i = scale_i == 0.0 ? (scale_i + 0.01) : scale_i;
			System.out.println("Generating Frame..." + i);
			imageFrames[i] = rotateImage(scaleImageAnimate(prevImage,scale_i), Math.round(angle_i));
		}
		System.out.println("Generation complete!");
		return imageFrames;
	}

	public void showIms(String[] args) {
		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

		// Command line arguments
		double scaleFactor = Double.parseDouble(args[1]);
		double rotation = Double.parseDouble(args[2]);
		int aliasFlag = Integer.parseInt(args[3]);
		int fps = Integer.parseInt(args[4]);
		int transition = Integer.parseInt(args[5]);

		if (fps == 0 || transition == 0) {
			imgOne = antiAlias(imgOne, aliasFlag);
			imgOne = scaleImageAnimate(imgOne, scaleFactor);
			imgOne = rotateImage(imgOne, rotation);
			util.showImsHelper(imgOne);
		} else {
			int numFrames = fps * transition;
			BufferedImage[] imageFrames = animate(imgOne, numFrames, rotation, scaleFactor, aliasFlag);

			//Display animation
			JFrame animationFrame = new JFrame();
			animationFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			JLabel lbIm2 = new JLabel();
			for (int i = 0; i < numFrames; i++) {
				util.showAnimationHelper(animationFrame, lbIm2, imageFrames[i]);
				try{
					Thread.sleep((int) (500.00/fps));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/////////////       MAIN        ///////////////
	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}
}
