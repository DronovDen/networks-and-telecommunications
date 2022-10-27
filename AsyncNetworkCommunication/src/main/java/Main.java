import controller.ApplicationController;
import view.AppView;

public class Main {
    public static void main(String[] args) {
        ApplicationController mainController = new ApplicationController();
        mainController.run();
        //AppView app = new AppView();
        //app.start();
    }
}
