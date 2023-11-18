package com.example.demo002.test;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Random;

public class TestMain {

    public static void main(String[] args) throws AWTException {
        Robot robot = new Robot();
        Random random = new Random();
        int x = 0;
        int y = 0;
        long count =1;
        while(true) {
            try {
                count++;
                if(count%2==0){
                    x = 500;
                }else{
                    x = -500;
                }
                robot.delay(5000);

                //y = Math.abs(random.nextInt(100)) % 100 + 50;
                System.out.println("x----"+x+"   y-----"+y);
                change(1, x, y);

            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public static void change(int type, int x, int y){
        Point p = MouseInfo.getPointerInfo().getLocation();
        int width = (int) p.getX() + x;
        int heigh = (int) p.getY() + y;
        if(type == 0) {
            width = x;
            heigh = y;
        }
        Robot robot;
        try {
            robot = new Robot();
            robot.mouseMove(width,heigh);
            pressMouse(robot, InputEvent.BUTTON1_MASK,1000);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    //鼠标移动到指定坐标，然后单击
    public  static  void Danji(Robot rt,int x, int y,int times){
        rt.mouseMove(-1,-1);//初始化
        rt.delay(1000);
        rt.mouseMove(x,y);//制动到指定位置
        rt.delay(3000);
        pressMouse(rt, InputEvent.BUTTON1_MASK,times);
    }

    //鼠标点击事件
    public  static  void pressMouse(Robot rt,int m,int times){
        rt.mousePress(m);
        rt.delay(3000);
        rt.mouseRelease(m);
        rt.delay(times);
        System.out.printf("鼠标点击完成"+m);
    }


}