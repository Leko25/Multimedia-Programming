
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 512;
	int height = 512;
	private static double INTERPOLATION_DIFF = 0.005;
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


	/**
	 * @param s : hexadecimal string
	 * @return Returns the corresponding pixel value
	 */
	private static int hex2Decimal(String s) {
		String digits = "0123456789ABCDEF";
		s = s.toUpperCase();
		int val = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int d = digits.indexOf(c);
			val = 16*val + d;
		}
		return val;
	}

//	private int interpolation(BufferedImage prevImage,
//							  double diagWidth, double diagHeight) {
//
//	}

	/**
	 *
	 * @param prevImg : Previous BufferedImage Object prior to this operation
	 * @param scaleFactor : scaling argument
	 * @return new BufferedImage that has been scale according to args[1]
	 */
	private BufferedImage scaleImage(BufferedImage prevImg, double scaleFactor) {
		int scaledWidth = (int) (prevImg.getWidth() * scaleFactor);
		int scaledHeight = (int) (prevImg.getHeight() * scaleFactor);
		if (scaleFactor == 1.0){
			return prevImg;
		}
		BufferedImage nextImg = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < scaledHeight; y++) {
			for (int x = 0; x < scaledWidth; x++) {
				int scaledPixel = prevImg.getRGB((int) (x/scaleFactor), (int) (y/scaleFactor));
				nextImg.setRGB(x, y, scaledPixel);
			}
		}
		return nextImg;
	}

	/**
	 *
	 * @param prevImage : Previous BufferedImage Object prior to this operation
	 * @param rotation : angle of rotation
	 * @return new BufferedImage that has been rotated according to args[2]
	 */
	private BufferedImage rotateImage(BufferedImage prevImage, double rotation){
		rotation = -Math.toRadians(rotation);
		if (rotation == 0){
			return prevImage;
		}

		//define image properties
		int imgWidth = prevImage.getWidth();
		int imgHeight = prevImage.getHeight();
//		int diagWidth = (int) (Math.sqrt(2) * imgWidth);
//		int diagHeight = (int) (Math.sqrt(2) * imgHeight);
		int diagWidth = 724; //TODO -- rotation is off fix
		int diagHeight = 724;
		double origin = imgWidth/2.0;
		double diagOrigin = diagWidth/2.0;

		BufferedImage rotatedImg = new BufferedImage(
				diagWidth,
				diagHeight,
				BufferedImage.TYPE_INT_RGB
		);
		for (int y = 0; y < diagHeight; y++) {
			for (int x = 0; x < diagWidth; x++) {
				double x_origin = x - diagOrigin;
				double y_origin = y - diagOrigin;
				double x_transformed = Math.cos(rotation) * x_origin - Math.sin(rotation) * y_origin;
				double y_transformed = Math.sin(rotation) * x_origin + Math.cos(rotation) * y_origin;
				int x_new = (int) (x_transformed + origin);
				int y_new = (int) (y_transformed + origin);
				if (x_new < 0 || y_new < 0 || x_new >= imgWidth || y_new >= imgHeight) {
					rotatedImg.setRGB(x, y, hex2Decimal("D0D0D0"));
				}
				else {
					rotatedImg.setRGB(x, y, prevImage.getRGB(x_new, y_new));
				}
			}
		}
		return rotatedImg;
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
				prevImage = addPadding(prevImage); //pad Image
				padding = 0;
			}
			int prevWidth = prevImage.getWidth();
			int prevHeight = prevImage.getHeight();
			BufferedImage filteredImage = new BufferedImage(prevWidth, prevHeight, BufferedImage.TYPE_INT_RGB);
			int stop = getStop(prevWidth, prevHeight, padding);
			for (int y = 0; y < stop; y++) {
				for (int x = 0; x < stop; x++) {
					int r = 0, g = 0, b = 0;
					for (int i = 0; i < FILTER_WINDOW_SIZE; i++) {
						for (int j = 0; j < FILTER_WINDOW_SIZE; j++) {
							if (x + i < stop - 1  && y + j<= stop - 1){
//								System.out.println((x + i) + ", " + (y + j));
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
	 * @param width : image width
	 * @param height : image height
	 * @return stopping parameter for loop
	 */
	private int getStop(int width, int height, int padding){
		if (padding == 0) {
			return width;
		}
		int i = 0;
		for (i = width - padding; i < width; i++){
			if (i % FILTER_WINDOW_SIZE == 0){
				break;
			}
		}
		return i;
	}

	/**
	 * @param prevImage : Previous BufferedImage Object prior to this operation
	 *@return Padded Image
	 */
	private BufferedImage addPadding(BufferedImage prevImage) {
		int padding = (int) ((FILTER_WINDOW_SIZE - 1)/2);
		int padWidth = prevImage.getWidth() + (padding * 2);
		int padHeight = prevImage.getHeight() + (padding * 2);
		BufferedImage paddedImage = new BufferedImage(padWidth, padHeight, BufferedImage.TYPE_INT_RGB);

		//Fill Center of the Image
		for (int x = padding; x < padWidth - padding; x++) {
			for (int y = padding; y < padHeight - padding; y++) {
				paddedImage.setRGB(x, y, prevImage.getRGB(x - padding, y - padding));
			}
		}

		//Pad the left-Boundary
		for (int y = 0; y < padding; y++) {
			for (int x = padding; x < padWidth - padding; x++) {
				paddedImage.setRGB(x, y, prevImage.getRGB(x - padding, y));
			}
		}

		//Pad the right-Boundary
		for (int y = padHeight - 1; y >= padHeight - padding; y--) {
			for (int x = padding; x < padWidth - padding; x++) {
				paddedImage.setRGB(x, y, prevImage.getRGB(x - padding, y - padding - 1));
			}
		}

		//Pad corners
		int prevWidth = prevImage.getWidth();
		int prevHeight = prevImage.getHeight();
		for (int x = 0; x < padding; x++) {
			for (int y = 0; y < padding; y++) {
				paddedImage.setRGB(
						x,
						y,
						prevImage.getRGB(0, 0));
				paddedImage.setRGB(x,
						y + prevWidth + padding,
						prevImage.getRGB(0, prevWidth - 1));
				paddedImage.setRGB(
						x + prevHeight + padding,
						y,
						prevImage.getRGB(prevHeight - 1, 0));
				paddedImage.setRGB(
						x + prevHeight + padding,
						y + prevWidth + padding,
						prevImage.getRGB(prevHeight - 1, prevWidth - 1)
				);
			}
		}
		return paddedImage;
	}


	private BufferedImage[] animate(BufferedImage prevImage, int numFrames, double rotation, double scaleFactor, int aliasFlag) {
		BufferedImage[] imageFrames = new BufferedImage[numFrames];

		// Anitaliasing
		prevImage = aliasFlag == 1 ? antiAlias(prevImage, 1) : prevImage;
		scaleFactor = 1/scaleFactor;

		// TODO -- Animation is off fix
		if (scaleFactor == 1.0) {
			for (int i = 0; i < numFrames; i++) {
				double tmpRot = (i * rotation)/((double) (numFrames - 1));
				prevImage = rotateImage(prevImage, tmpRot);
				imageFrames[i] = prevImage;
			}
		}
		return imageFrames;
	}

	private void showImsHelper(BufferedImage prevImage) {
		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	private void showAnimationHelper(JFrame animationFrame, BufferedImage prevImage){
		JLabel lbIm2 = new JLabel(new ImageIcon(prevImage));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		animationFrame.getContentPane().removeAll();
		animationFrame.getContentPane().add(lbIm2, c);

		animationFrame.pack();
		animationFrame.setVisible(true);
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
			imgOne = scaleImage(imgOne, scaleFactor);
			imgOne = rotateImage(imgOne, rotation);
			showImsHelper(imgOne);
		} else {
			int numFrames = fps * transition;
			BufferedImage[] imageFrames = animate(imgOne, numFrames, rotation, scaleFactor, aliasFlag);

			//Display animation
			JFrame animationFrame = new JFrame();
			GridBagLayout gLayout = new GridBagLayout();
			animationFrame.getContentPane().setLayout(gLayout);
			for (int i = 0; i < numFrames; i++) {
				showAnimationHelper(animationFrame, imageFrames[i]);
				try{
					Thread.sleep((int) (1500.00/fps));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}
}
