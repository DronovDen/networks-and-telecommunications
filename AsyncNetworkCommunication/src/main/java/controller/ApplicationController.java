package controller;
import view.AppView;

public class ApplicationController implements Runnable {
    @Override
    public void run() {
        AppView mainView = new AppView();
        mainView.start();
    }
}