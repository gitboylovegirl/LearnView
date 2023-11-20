package com.trs.app.learnview.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.Nullable;
import androidx.core.graphics.PathParser;

import com.trs.app.learnview.R;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MapView extends View {

    private List<MapItem> list = new ArrayList<>();  // 地图元素列表
    private Paint paint;  // 画笔
    private int vectorWidth = -1;  // 矢量宽度
    private Matrix matrix = new Matrix();  // 矩阵
    private Matrix invertMatrix = new Matrix();  // 反转矩阵
    private float viewScale = -1f;  // 视图缩放比例
    private float userScale = 1.0f;  // 用户缩放比例
    private boolean initFinish = false;  // 初始化完成标志
    private int bgColor;  // 背景颜色
    private GestureDetector gestureDetector;  // 手势检测器
    private int offsetX, offsetY;  // 偏移量
    private Scroller scroller;  // 滚动器
    private float[] points;  // 点的坐标数组
    private float[] pointsFocusBefore;  // 缩放前的焦点坐标数组
    private float focusX, focusY;  // 缩放焦点的坐标
    private ScaleGestureDetector scaleGestureDetector;  // 缩放手势检测器
    private boolean showDebugInfo = false;  // 是否显示调试信息
    private static final int MAX_SCROLL = 10000;  // 最大滚动距离
    private static final int MIN_SCROLL = -10000;  // 最小滚动距离
    private int mapId = R.raw.ic_african;  // 地图资源ID
    public MapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgColor = Color.parseColor("#f5f5f5");  // 设置背景颜色
        paint = new Paint();  // 创建画笔对象
        paint.setAntiAlias(true);  // 开启抗锯齿
        paint.setColor(Color.GRAY);  // 设置画笔颜色为灰色
        scroller = new Scroller(getContext());  // 创建滚动器对象
        gestureDetector = new GestureDetector(getContext(), onGestureListener);  // 创建手势检测器对象，并设置手势监听器
        scaleGestureDetector = new ScaleGestureDetector(getContext(), scaleGestureListener);  // 创建缩放手势检测器对象，并设置缩放手势监听器
    }

    private ScaleGestureDetector.OnScaleGestureListener scaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        float lastScaleFactor;  // 上一次的缩放因子
        boolean mapPoint = false;  // 是否为地图点

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();  // 获取当前的缩放因子
            float[] points = new float[]{detector.getFocusX(), detector.getFocusY()};  // 获取缩放焦点的坐标数组
            pointsFocusBefore = new float[]{detector.getFocusX(), detector.getFocusY()};  // 缩放前的焦点坐标数组
            if (mapPoint) {
                mapPoint = false;
                invertMatrix.mapPoints(points);  // 将坐标数组映射到反转矩阵上
                focusX = points[0];  // 更新缩放焦点的X坐标
                focusY = points[1];  // 更新缩放焦点的Y坐标
            }
            float change = scaleFactor - lastScaleFactor;  // 计算缩放变化量
            lastScaleFactor = scaleFactor;  // 更新上一次的缩放因子
            userScale += change;  // 更新用户缩放比例
            postInvalidate();  // 刷新视图
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            lastScaleFactor = 1.0f;  // 初始化上一次的缩放因子为1.0
            mapPoint = true;  // 设置为地图点模式，用于在缩放开始时获取正确的焦点坐标
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // TODO: 2023/11/20 是否需要重置？
            mapPoint = false;  // 缩放结束后，取消地图点模式
        }
    };

    private GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // 当用户进行单击时的回调
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            boolean result = false;
            float x = event.getX();
            float y = event.getY();
            // 将触摸坐标存储在数组中
            points = new float[]{x, y};
            // 对触摸坐标应用逆转换矩阵
            invertMatrix.mapPoints(points);
            // 遍历地图项列表，并检查是否有项被触摸
            for (MapItem item : list) {
                if (item.onTouch(points[0], points[1])) {
                    result = true;  // 该项被触摸
                }
            }
            postInvalidate();  // 请求重新绘制视图
            return result;  // 表示触摸事件是否已处理
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // 根据滚动距离和用户缩放更新偏移量
            offsetX += -distanceX / userScale;
            offsetY += -distanceY / userScale;
            postInvalidate();  // 请求重新绘制视图
            return true;  // 表示滚动事件已处理
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // 当用户进行长按时的回调
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // 使用Scroller处理快滑手势
            scroller.fling(offsetX, offsetY, (int) ((int) velocityX / userScale), (int) ((int) velocityY / userScale), MIN_SCROLL,
                    MAX_SCROLL, MIN_SCROLL, MAX_SCROLL);
            postInvalidate(); // 请求重新绘制视图
            return true; // 表示快滑事件已处理
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 将触摸事件传递给gestureDetector和scaleGestureDetector
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        return true; // 表示触摸事件已处理
    }

    public void setMapId(int mapId) {
        this.mapId = mapId;
        userScale=1.0f;
        offsetY=0;
        offsetX=0;
        focusX=0;
        focusY=0;
        new Thread(new DecodeRunnable()).start(); // 启动新线程解码SVG文件
    }

    private class DecodeRunnable implements Runnable {
        @Override
        public void run() {
            // 使用DOM解析器解析SVG文件
            InputStream inputStream = getContext().getResources().openRawResource(mapId);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(inputStream);

                // 从SVG文档中提取信息
                Element rootElement = doc.getDocumentElement();
                String strWidth = rootElement.getAttribute("android:width");
                vectorWidth = Integer.parseInt(strWidth.replace("dp", ""));
                NodeList items = rootElement.getElementsByTagName("path");

                list.clear();  // 清空地图项列表
                for (int i = 1; i < items.getLength(); i++) {
                    Element element = (Element) items.item(i);
                    String pathData = element.getAttribute("android:pathData");
                    @SuppressLint("RestrictedApi")
                    Path path = PathParser.createPathFromPathData(pathData);
                    MapItem item = new MapItem(path, i);
                    list.add(item);  // 将地图项添加到列表
                }
                initFinish = true;  // 表示初始化完成
                postInvalidate();  // 请求重新绘制视图
            } catch (Exception e) {
                e.printStackTrace();  // 处理任何异常
            }
        }
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            offsetX = scroller.getCurrX();
            offsetY = scroller.getCurrY();
            invalidate();  // 请求重新绘制视图
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        if (vectorWidth != -1 && viewScale == -1) {
            int width = getWidth();
            viewScale = width * 1.0f / vectorWidth;  // 计算视图比例
        }
        if (viewScale != -1) {
            float scale = viewScale * userScale;
            matrix.reset();
            matrix.postTranslate(offsetX, offsetY);
            matrix.postScale(scale, scale, focusX, focusY);

            invertMatrix.reset();
            matrix.invert(invertMatrix);  // 计算逆转换矩阵
        }
        canvas.setMatrix(matrix);
        canvas.drawColor(bgColor);  // 绘制背景色
        if (initFinish) {
            for (MapItem item : list) {
                item.onDraw(canvas, paint);  // 绘制地图项
            }
        }

        showDebugInfo(canvas);  // 显示调试信息
    }

    private void showDebugInfo(Canvas canvas) {
        if (!showDebugInfo) {
            return;
        }
        if (points != null) {
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(points[0], points[1], 20, paint);  // 绘制触摸点
        }
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(focusX, focusY, 20, paint);  // 绘制焦点

        if (pointsFocusBefore != null) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(pointsFocusBefore[0], pointsFocusBefore[1], 20, paint);  // 绘制之前的聚焦点
        }
    }
}


class MapItem {
    Path path; // 路径对象
    private final Region region; // 区域对象
    private boolean isSelected = false; // 是否被选中的标志
    private final RectF rectF; // 矩形区域对象
    private final int index; // 索引值

    public boolean onTouch(float x, float y) {
        if (region.contains((int) x, (int) y)) { // 判断触摸点是否在区域内
            isSelected = true; // 设置为选中状态
            return true;
        }
        isSelected = false; // 设置为未选中状态
        return false;
    }

    public MapItem(Path path, int index) {
        this.path = path;
        rectF = new RectF();
        path.computeBounds(rectF, true); // 计算路径的边界矩形
        region = new Region();
        region.setPath(path, new Region(new Rect((int) rectF.left
                , (int) rectF.top, (int) rectF.right, (int) rectF.bottom))); // 根据路径和边界矩形创建区域对象
        this.index = index; // 设置索引值
    }


    protected void onDraw(Canvas canvas, Paint paint) {
        paint.reset();
        paint.setColor(isSelected ? Color.YELLOW : Color.GRAY); // 根据选中状态设置颜色
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPath(path, paint); // 绘制填充路径
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        canvas.drawPath(path, paint); // 绘制路径轮廓
        paint.setColor(Color.GRAY);
        paint.setColor(Color.BLUE);
        //canvas.drawText(index+"",rectF.centerX(),rectF.centerY(),paint);
    }
}
