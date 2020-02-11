import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplayUtility {

    private JFrame frame;
    private JLabel lbIm1;
    public int FILTER_WINDOW_SIZE = 3;

    /**
     * @param prevImage : Previous BufferedImage Object prior to this operation
     *@return Padded Image
     */
    public BufferedImage addPadding(BufferedImage prevImage) {
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

    /**
     * convert Hexadecimal to pixel RGB
     * @param s : hexadecimal string
     * @return Returns the corresponding pixel value
     */
    public int hex2Decimal(String s) {
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

    /**
     * Helper function to find padded image border for low-pass filter
     * @param width : image width
     * @param height : image height
     * @return stopping parameter for loop
     */
    public int getStop(int width, int height, int padding){
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

    // Image Display Helper functions

    public void showImsHelper(BufferedImage prevImage) {
        // Use label to display the image
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        lbIm1 = new JLabel(new ImageIcon(prevImage));

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
        System.out.println(frame.getWidth());
    }

//    public void showAnimationHelper(JFrame animationFrame, JLabel lbIm2, BufferedImage prevImage){
//        lbIm2.setIcon(new ImageIcon(prevImage));
//        animationFrame.getContentPane().removeAll();
//        animationFrame.getContentPane().add(lbIm2, BorderLayout.CENTER);
//        animationFrame.setLocation(200, 0);
//        animationFrame.pack();
//        animationFrame.setVisible(true);
//    }

    public void showAnimationHelper(JFrame animationFrame, JLabel lbIm2, BufferedImage prevImage) {
        lbIm2.setIcon(new ImageIcon(prevImage));
        animationFrame.getContentPane().removeAll();
        GridBagLayout gLayout = new GridBagLayout();
        animationFrame.getContentPane().setLayout(gLayout);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        animationFrame.getContentPane().add(lbIm2, c);
        animationFrame.pack();
        animationFrame.setVisible(true);
    }
}