package ph.com.guanzongroup.cas.joborder.util;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.guanzon.appdriver.base.GRider;

/**
 * FXML Controller class
 *
 * @author Maynard
 */
public class PITSelectionController implements Initializable {

    @FXML
    private ComboBox<String> cbPitSelection;
    @FXML
    private Button cancelButton;
    @FXML
    private Button okButton;

    @FXML
    public void onCancelClicked(ActionEvent event) {
        p_sPITID = "";
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void onOkClicked(ActionEvent event) {

        try {
            int lnRow = cbPitSelection.getSelectionModel().getSelectedIndex() + 1;
            p_oResultSet.absolute(lnRow);
            p_sPITID = p_oResultSet.getString("sPITIDxxx");

            Stage stage = (Stage) okButton.getScene().getWindow();
            stage.close();

        } catch (SQLException ex) {
            Logger.getLogger(PITSelectionController.class.getName()).log(Level.SEVERE, null, ex);
            p_sPITID = "";
            Stage stage = (Stage) okButton.getScene().getWindow();
            stage.close();
        }
    }

    public void loadPIT() {

        if (p_oResultSet == null) {
            return;
        }

        try {
            p_aPitNumber.clear();
            cbPitSelection.getItems().clear();
            cbPitSelection.getSelectionModel().clearSelection();

            p_oResultSet.beforeFirst();

            while (p_oResultSet.next()) {
                p_aPitNumber.add(
                        p_oResultSet.getString("sPITNmbrx")
                );
            }

            cbPitSelection.setItems(p_aPitNumber);
            cbPitSelection.getSelectionModel().selectFirst();

        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void setGRider(GRider foValue) {
        p_oApp = foValue;
    }

    public void setDataSource(ResultSet foResultSet) {
        p_oResultSet = foResultSet;
    }

    public void setSQLSource(String foSQLSource) {
        p_sSQLSource = foSQLSource;
    }

    public boolean isCancelled() {
        return p_bCancelled;
    }

    public String getPITID() {
        return p_sPITID;
    }

    private GRider p_oApp;
    private ResultSet p_oResultSet;
    private String p_sSQLSource;
    private String p_sPITID;
    private boolean p_bCancelled;
    private ObservableList<String> p_aPitNumber = FXCollections.observableArrayList();

}
