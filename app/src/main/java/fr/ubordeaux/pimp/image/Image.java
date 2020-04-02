package fr.ubordeaux.pimp.image;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.Queue;

import fr.ubordeaux.pimp.io.BitmapIO;
import fr.ubordeaux.pimp.util.Utils;

/**
 * This class is used to manipulate a Picture, this class mainly contain an Android {@link Bitmap} but offers in additions some methods and utilities to help to create and manage Images with effects applyed.
 */
public class Image {

    private int width;
    private int height;

    private Uri uri;
    private ImageInfo infos; //embeds all available information

    //Original version of the image at its creation
    private int[] imgBase;

    //Quick save of the image done when opening an effect, in order to discard its modifications later
    private int[] imgQuickSave;

    //Hitory of effects applyed.
    private Queue<ImageEffectRunnable> confirmedEffectsHistory;
    private Queue<ImageEffectRunnable> tempEffectsHistory;

    //Core of the Image, Bitmap representing its pixels.
    private Bitmap bitmap;


    /**
     * Load an image from resources, size is automatically limited depending the screen size.
     *
     * @param id      int value of the resource
     * @param context Execution context
     */
    public Image(int id, Activity context) {
        this(id, Utils.getScreenSize(context), context);
    }

    /**
     * See {@link #Image(int, int, int, Activity)}
     *
     * @param id      int value of the resource
     * @param size    Point where x is width and y is height.
     * @param context Execution context
     */
    public Image(int id, Point size, Activity context) {
        this(id, size.x, size.y, context);
    }

    /**
     * Load an image from resources with size limitation.
     * See {@link fr.ubordeaux.pimp.util.Utils#calculateInSampleSize(int, int, int, int)}
     * Note that information are not yet managed with this constructor.
     *
     * @param id             int value of the resource
     * @param requiredWidth  The desired width for the image.
     * @param requiredHeight The desired height for the image.
     * @param context        Execution context
     */
    public Image(int id, int requiredWidth, int requiredHeight, Activity context) {
        this(BitmapIO.decodeAndScaleBitmapFromResource(id, requiredWidth, requiredHeight, context));
        Resources resources = context.getResources();
        this.uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(id))
                .appendPath(resources.getResourceTypeName(id))
                .appendPath(resources.getResourceEntryName(id))
                .build(); //TO TEST !!!!!!
        infos = new ImageInfo(null, context); // todo ?
        infos.setLoadedHeight(height);//set values n info pack
        infos.setLoadedWidth(width);
    }

    /**
     * Load an image from folders, size is automatically limited depending the screen size.
     *
     * @param uri     Path of the picture to laod.
     * @param context Execution context
     */
    public Image(Uri uri, Activity context) {
        this(uri, Utils.getScreenSize(context), context);
    }

    /**
     * See {@link #Image(Uri, int, int, Activity)}
     *
     * @param uri     Path of the picture to load.
     * @param size    Point where x is width and y is height.
     * @param context Execution context
     */
    public Image(Uri uri, Point size, Activity context) {
        this(uri, size.x, size.y, context);
    }

    /**
     * Load an image from folders with size limitation.
     * See {@link fr.ubordeaux.pimp.util.Utils#calculateInSampleSize(int, int, int, int)}
     *
     * @param uri            Path of the picture to laod.
     * @param requiredWidth  The desired width for the image.
     * @param requiredHeight The desired height for the image.
     * @param context        Execution context
     */
    public Image(Uri uri, int requiredWidth, int requiredHeight, Activity context) {
        this(BitmapIO.decodeAndScaleBitmapFromUri(uri, requiredWidth, requiredHeight, context));
        this.uri = uri;
        infos = new ImageInfo(this.uri, context);
        infos.setLoadedHeight(height);//set values n info pack
        infos.setLoadedWidth(width);
    }


    /**
     * Special constructor to convert a Bitmap already created to an Image.
     * Note that the Bitmap instance is included in the Image and its not a copy of it.
     * A modification on the Image will modify the Bitmap and vice versa.
     *
     * @param bmp Bitmap to include in the Image.
     */
    public Image(Bitmap bmp) {
        bitmap = bmp;
        width = bmp.getWidth();
        height = bmp.getHeight();
        imgBase = new int[width * height];
        bitmap.getPixels(imgBase, 0, width, 0, 0, width, height);
        confirmedEffectsHistory = new LinkedList<>();
    }

    /**
     * See {@link #Image(Image, int, int)}
     * Differs from {@link #Image(Bitmap)} because this does not pack the bitmap in the Image, but create another Bitmap.
     *
     * @param bmp               Source Bitmap
     * @param newRequiredWidth  The desired width for the image. (must be less than or equal to the original)
     * @param newRequiredHeight The desired height for the image. (must be less than or equal to the original)
     */
    public Image(Bitmap bmp, int newRequiredWidth, int newRequiredHeight) {
        newRequiredWidth = newRequiredWidth == 0 || newRequiredWidth > bmp.getWidth() ? bmp.getWidth() : newRequiredWidth;
        newRequiredHeight = newRequiredHeight == 0 || newRequiredHeight > bmp.getHeight() ? bmp.getHeight() : newRequiredHeight;
        int ratio = Utils.calculateInSampleSize(bmp.getWidth(), bmp.getHeight(), newRequiredWidth, newRequiredHeight);
        Bitmap newBitmap = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / ratio,
                bmp.getHeight() / ratio, true); //true for bilinear filtering
        bitmap = newBitmap;
        width = newBitmap.getWidth();
        height = newBitmap.getHeight();
        imgBase = new int[width * height];
        bitmap.getPixels(imgBase, 0, width, 0, 0, width, height);
        infos = new ImageInfo(null, null);
        infos.setLoadedHeight(height);//set values n info pack
        infos.setLoadedWidth(width);
        confirmedEffectsHistory = new LinkedList<>();

    }

    /**
     * Use this constructor to duplicate an Image.
     *
     * @param source Image to copy.
     */
    public Image(Image source) {
        this(source, source.getWidth(), source.getHeight());
    }

    /**
     * Constructor to create an Image from a rescaled other Image.
     * See {@link fr.ubordeaux.pimp.util.Utils#calculateInSampleSize(int, int, int, int)}
     * Note that the rescaling is using bilinear filtering, see {@link android.graphics.Bitmap#createScaledBitmap(Bitmap, int, int, boolean)}.
     * Note that infos will be duplicated.
     *
     * @param source            Source Image
     * @param newRequiredWidth  The desired width for the image. (must be less than or equal to the original)
     * @param newRequiredHeight The desired height for the image. (must be less than or equal to the original)
     */
    public Image(Image source, int newRequiredWidth, int newRequiredHeight) {
        this(source.getBitmap(), newRequiredWidth, newRequiredHeight);
        this.uri = source.uri;
        infos = new ImageInfo(source.getInfo());
        infos.setLoadedHeight(height);//set values n info pack
        infos.setLoadedWidth(width);
    }

    /**
     * Reset all pixels of the Image to the original version (when the Image was created or loaded).
     */
    public void reset() {
        bitmap.setPixels(imgBase, 0, width, 0, 0, width, height);
        confirmedEffectsHistory.clear();
        tempEffectsHistory.clear();
    }


    /**
     * If used for the first time, initialise a new save of all pixels of the Image.
     * Then save the current Image. See also {@link #discard()}.
     */
    public void quickSave() {
        if (imgQuickSave == null) {
            imgQuickSave = new int[width * height];
            tempEffectsHistory = new LinkedList<>();
        }
        bitmap.getPixels(imgQuickSave, 0, width, 0, 0, width, height);

        // Confirm all unconfirmedEffects :
        while (tempEffectsHistory.peek() != null) {
            confirmedEffectsHistory.add(tempEffectsHistory.remove());
        }
    }

    /**
     * Restore the Image to the last quick save, do nothing if {@link #quickSave()} was never called before.
     * Will also remove from the effects history all effects applied since the quicksave.
     */
    public void discard() {
        if (imgQuickSave != null) {
            bitmap.setPixels(imgQuickSave, 0, width, 0, 0, width, height);
            tempEffectsHistory.clear(); // clear last unconfirmed effects.
        }
    }


    /**
     * Getter of the Bitmap included in the Image, use it to convert this Image to a Bitmap.
     *
     * @return Bitmap object of this Image
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * @return Number of pixels in the width of this Image
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return Number of pixels in the height of this Image
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return Get a pack of information about this image: see {@link ImageInfo}. Note that depending if the image is loaded or if it was created from a Bitmap, some fiels can be null.
     */
    public ImageInfo getInfo() {
        return infos;
    }


    /**
     * @return Get uri from image
     */
    public Uri getUri() {
        return uri;
    }

    /**
     * Export to the device gallery the original picture file with all effects applied on the current Image
     * This method needs permission to acces device storage, please allow it before call this method.
     *
     * @param context Execution context
     * @return True if export ends correclty
     */
    public boolean exportOriginalToGallery(Activity context) {
        return exportOriginalToGallery(context, false);
    }

    /**
     * Same as {@link  #exportOriginalToGallery(Activity)} but you can specified to using {@link Toast} to show effects advancement.
     *
     * @param context Execution context
     * @param toasted true if you want show Toasts
     * @return True if export ends correclty
     */
    public boolean exportOriginalToGallery(Activity context, boolean toasted) {
        if (context == null || context.isFinishing())
            return false;
        if (getUri() == null) return false;
        Bitmap result;
        try {
            result = BitmapIO.decodeAndScaleBitmapFromUri(getUri(), 5000, 5000, context); //TODO choose correct size, and warn user that exported image will be smaller if original size even too large
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        Queue<ImageEffectRunnable> effectQueue = new LinkedList<>(getEffectsHistory()); //Get copy of queue:

        applyQueueEffects(effectQueue, result, toasted, context);

        return BitmapIO.saveBitmap(result, "pimp_image", context);
    }

    /**
     * Export to the device gallery the current Image with the same size and all applied effects
     * This method needs permission to acces device storage, please allow it before call this method.
     *
     * @param context Execution context
     * @return True if export ends correclty
     */
    public boolean exportToGallery(Activity context) {
        return BitmapIO.saveBitmap(getBitmap(), "pimp_image", context);
    }


    /**
     * Will return the history of all effects applyed to the Image.
     * Note that the returned List is created when calling this method, please store as quick as possible the returned value.
     *
     * @return FIFO of effects applyed to the Image.
     */
    public Queue<ImageEffectRunnable> getEffectsHistory() {
        Queue<ImageEffectRunnable> effects = new LinkedList<>(confirmedEffectsHistory); //Merge confirmed effects and temp effects.

        if (tempEffectsHistory != null) {
            effects.addAll(tempEffectsHistory);
        }
        return effects;
    }

    /**
     * Apply an effect to the Image, it is still possible to apply an effect to the Bitmap of this Image, however using this method will assure that the hisotry of effects will be correct.
     *
     * @param effect The runnable of the effect function with correct args, see the class {@link ImageEffectRunnable} for more information.
     */
    public void applyEffect(ImageEffectRunnable effect) {
        effect.setBmp(this.getBitmap()); // to be sure that it will be applied on the Image
        effect.run(); //apply effect

        //add effect to history:
        if (tempEffectsHistory == null) //there is no quickSave
            confirmedEffectsHistory.add(effect);
        else
            tempEffectsHistory.add(effect);
    }

    /**
     * Apply severals effects on a Bitmap.
     *
     * @param queue   A FIFO of effects to apply
     * @param bitmap  the target Bitmap
     * @param toasted true if you want to show advancement, with {@link Toast}.
     * @param context Must be define if toasted is set to true
     */
    private static void applyQueueEffects(Queue<ImageEffectRunnable> queue, Bitmap bitmap, boolean toasted, Activity context) {
        ImageEffectRunnable effect;
        int totalEffects = queue.size();
        int currentEffect = 1;
        effect = queue.poll(); // Get first effect
        while (effect != null) {
            if (toasted) //todo, use handler ??
            {
                final int finalCurrentEffect = currentEffect;
                final int finalTotalEffects = totalEffects;
                final Activity finalContext = context;
                context.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(finalContext, "Applying effect " + finalCurrentEffect + " of " + finalTotalEffects, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            effect.setBmp(bitmap); //Apply the effect on the right bitmap
            effect.run();
            effect = queue.poll();
            currentEffect++;
        }
    }

}
