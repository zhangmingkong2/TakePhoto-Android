package com.marlinl.code.takephoto.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by MarlinL on 2015/6/8.
 */
public class AutoFitTextureView extends TextureView {

    private int width = 0;
    private int height = 0;


    public AutoFitTextureView(Context context) {
        super(context, null);
    }

    public AutoFitTextureView(Context context ,AttributeSet attr){
        this(context,attr, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attributeSet, int defStyle){
        super(context, attributeSet, defStyle);
    }

    public void setAspectRatio(int aspectRatioWidth, int aspectRatioHeigh){
        if (aspectRatioHeigh < 0 || aspectRatioWidth < 0){
            throw new IllegalArgumentException("size connt be negative");
        }
        this.width = aspectRatioWidth;
        this.height = aspectRatioHeigh;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == width || 0 == height) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * width / height) {
                setMeasuredDimension(width, width * height / width);
            } else {
                setMeasuredDimension(height * width / height, height);
            }
        }
    }
}
