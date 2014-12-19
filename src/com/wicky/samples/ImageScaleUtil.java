package com.wicky.samples;

import static org.imgscalr.Scalr.OP_BRIGHTER;
import static org.imgscalr.Scalr.resize;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;

public class ImageScaleUtil {
    /**
     * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step scaling technique that provides higher quality than the usual
     *            one-step technique (only useful in downscaling cases, where {@code targetWidth} or {@code targetHeight} is smaller than
     *            the original dimensions, and generally only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {
        double ratio = (double) img.getWidth() / img.getHeight();
        double ratioNew = (double) targetWidth/targetHeight;
        
        if(ratioNew < ratio){
            targetHeight = (int) (targetWidth / ratio + 0.4);
        }else{
            targetWidth = (int) (targetHeight * ratio + 0.4);
        }
        
        int type = (img.getTransparency() == Transparency.OPAQUE)?BufferedImage.TYPE_INT_RGB:BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    public static boolean resizeUsingJavaAlgo(String source, File dest, int width, int height) throws IOException {
        BufferedImage sourceImage = ImageIO.read(new FileInputStream(source));
        double ratio = (double) sourceImage.getWidth() / sourceImage.getHeight();
        double ratioNew = (double) width/height;
        
        if(ratioNew < ratio){
            height = (int) (width / ratio + 0.4);
        }else{
            width = (int) (height * ratio + 0.4);
        }
        
        if (width < 1) {
            width = (int) (height * ratio + 0.4);
        } else if (height < 1) {
            height = (int) (width / ratio + 0.4);
        }

        Image scaled = sourceImage.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        BufferedImage bufferedScaled = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedScaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(scaled, 0, 0, width, height, null);
        dest.createNewFile();
        writeJpeg(bufferedScaled, dest.getCanonicalPath(), 1.0f);
        return true;
    }

    /**
     * Write a JPEG file setting the compression quality.
     *
     * @param image a BufferedImage to be saved
     * @param destFile destination file (absolute or relative path)
     * @param quality a float between 0 and 1, where 1 means uncompressed.
     * @throws IOException in case of problems writing the file
     */
    public static void writeJpeg(BufferedImage image, String destFile, float quality) throws IOException {
        ImageWriter writer = null;
        FileImageOutputStream output = null;
        try {
            writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            output = new FileImageOutputStream(new File(destFile));
            writer.setOutput(output);
            IIOImage iioImage = new IIOImage(image, null, null);
            writer.write(null, iioImage, param);
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            if (output != null) {
                output.close();
            }
        }
    }

    /////////////////////////////////TEST CODES/////////////////////////////////////
    public static BufferedImage createThumbnail(BufferedImage img, int width, int height) {
        // Create quickly, then smooth and brighten it.
        return resize(img, Method.ULTRA_QUALITY, width, height/*, OP_BRIGHTER*/);
        
        // Let's add a little border before we return result.
//        return pad(img, 4);
      }
      
      public static BufferedImage createThumbnail2Steps(BufferedImage img, int width, int height) {
          int oWidth = img.getWidth();
          int oheight = img.getHeight();
          
          int width1 = (width + oWidth)/2;
          int height1 = (height + oheight)/2;
          BufferedImage img1 = resize(img, Method.ULTRA_QUALITY, width1, height1);
          
          // Create quickly, then smooth and brighten it.
          return resize(img1, Method.ULTRA_QUALITY, width, height/*, OP_BRIGHTER*/);
      }
      
      public static BufferedImage createThumbnail3Steps(BufferedImage img, int width, int height) {
          int oWidth = img.getWidth();
          int oheight = img.getHeight();
          
          int width1 = (width + oWidth)*2/3;
          int height1 = (height + oheight)*2/3;
          BufferedImage img1 = resize(img, Method.ULTRA_QUALITY, width1, height1);
          
          int width2 = (width + oWidth)/3;
          int height2 = (height + oheight)/3;
          BufferedImage img2 = resize(img1, Method.ULTRA_QUALITY, width2, height2);
          
          // Create quickly, then smooth and brighten it.
          return resize(img2, Method.ULTRA_QUALITY, width, height/*, OP_BRIGHTER*/);
          
      }
      
      public static BufferedImage cropPartThumbnail(BufferedImage img, int width, int height) {
          int oWidth = img.getWidth();
          int oheight = img.getHeight();
          int widthHalf = (width + oWidth)/2;
          int heightHalf = (height + oheight)/2;
          BufferedImage newImg = resize(img, Method.ULTRA_QUALITY, widthHalf, heightHalf);
          return cropThumbnail(newImg, width, height);
      }
      
      public static BufferedImage cropThumbnail(BufferedImage img, int width, int height) {
          try {
              int oWidth = img.getWidth();
              int oheight = img.getHeight();
              
              int x = 0;
              int y = 0;
              
              if(width < oWidth){
                  x = (oWidth - width)/2;
              }
              
              if(height < oheight){
                  y = (oheight - height)/2;
              }
              return Scalr.crop(img, x, y, width, height);
          } catch (Exception e) {
              BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
              Graphics g = image.getGraphics();
              g.setColor(null);
              g.drawLine(0, 0, 1, 1);
              return image;
          }

        }
      
      public static void main(String[] args) throws IOException {
          
          int width = 150;
          int height = 150;
          
          String fileName = "222.jpg";
          String filePath = "C:\\Users\\williamz\\Desktop\\";
          BufferedImage img = ImageIO.read(new File(filePath + fileName));
          
          String fileName1 = fileName.split("\\.")[0];
          String ext = "."+fileName.split("\\.")[1];
          int idx = 1;
          
          BufferedImage thumbnail = createThumbnail(img, width, height);
          ImageScaleUtil.writeJpeg(thumbnail, filePath + fileName1 + "_thumb"+idx+++"_"+width+"x"+height+ext, 1.0f);
          
          thumbnail = createThumbnail2Steps(img, width, height);
          ImageScaleUtil.writeJpeg(thumbnail, filePath + fileName1 + "_thumb"+idx+++"_"+width+"x"+height+ext, 1.0f);
          
          thumbnail = createThumbnail3Steps(img, width, height);
          ImageScaleUtil.writeJpeg(thumbnail, filePath + fileName1 + "_thumb"+idx+++"_"+width+"x"+height+ext, 1.0f);
          
          thumbnail = ImageScaleUtil.getScaledInstance(img, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
          ImageScaleUtil.writeJpeg(thumbnail, filePath + fileName1 + "_thumb"+idx+++"_"+width+"x"+height+ext, 1.0f);
          
          ImageScaleUtil.resizeUsingJavaAlgo(filePath + fileName1 + ext, new File(filePath + fileName1 + "_thumb5_"+width+"x"+height+ext), width, height);
          
          BufferedImage crop = cropThumbnail(img, width, height);
          ImageScaleUtil.writeJpeg(crop, filePath + fileName1 + "_crop1_"+width+"x"+height+ext, 1.0f);
          
          BufferedImage crop2 = cropPartThumbnail(img, width, height);
          ImageScaleUtil.writeJpeg(crop2, filePath + fileName1 + "_crop2_"+width+"x"+height+ext, 1.0f);
      }
}
