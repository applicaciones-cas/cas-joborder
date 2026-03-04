package ph.com.guanzongroup.cas.joborder.util;

import java.awt.Color;
import java.sql.ResultSet;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import org.guanzon.appdriver.base.GRider;

public class PITSelection extends Application {

    public static String pxeQuickSearch = "PITSelectionFX";
    public static String pxeQuickSearchScreen = "/ph/com/guanzongroup/cas/joborder/view/child/PITSelection.fxml";

    public void setGRider(GRider foGRider) {
        this.poGRider = foGRider;
    }

    public void setResultSet(ResultSet foRecSource) {
        this.poSource = foRecSource;
    }

    public void setSQLSource(String fsSQLSource) {
        this.psSQLSource = fsSQLSource;
    }

    private double xOffset = 0;
    private double yOffset = 0;

    private static GRider poGRider;
    private static ResultSet poSource;
    private static String psSQLSource;
    private static String psReturnVal;


    public String getResult() {
        return psReturnVal;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource(pxeQuickSearchScreen));

        /*SET PARAMETERS TO CLASS*/
        PITSelectionController oSearch = new PITSelectionController();
        oSearch.setGRider(poGRider);
        oSearch.setDataSource(poSource);
        oSearch.setSQLSource(psSQLSource);

        fxmlLoader.setController(oSearch);
        Parent parent = fxmlLoader.load();
        
        oSearch.loadPIT();

        parent.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        parent.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            }
        });

        Scene scene = new Scene(parent);
        scene.setFill(null);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.initModality(Modality.APPLICATION_MODAL);
        primaryStage.showAndWait();

        if (!oSearch.isCancelled()) {
            psReturnVal = oSearch.getPITID();
        } else {
            psReturnVal = "";
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
