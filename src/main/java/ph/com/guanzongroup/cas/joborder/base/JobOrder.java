package ph.com.guanzongroup.cas.joborder.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import org.guanzon.appdriver.base.CommonUtils;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.RecordStatus;
import ph.com.guanzongroup.cas.joborder.util.PITSelection;

/**
 * @author Valencia Maynard
 * @since 02-26-2026
 */
public class JobOrder {

    private final GRider p_oApp;
    private final boolean p_bWithParent;

    private final String MASTER_TABLE = "JobOrderBranch_Master";
    private final String DETAIL_TABLE = "JobOrderBranch_Detail";
    private String p_sBranchCd;

    private String p_sMessage;
    private boolean p_bWithUI = true;

    private CachedRowSet p_oMaster;
    private CachedRowSet p_oDetail;
    private CachedRowSet p_aJOList;
    private CachedRowSet p_oJOServiceStatus;

    public JobOrder(GRider foApp, String fsBranchCd, boolean fbWithParent) {
        p_oApp = foApp;
        p_sBranchCd = fsBranchCd;
        p_bWithParent = fbWithParent;

        if (p_sBranchCd.isEmpty()) {
            p_sBranchCd = p_oApp.getBranchCode();
        }

    }

    public void setWithUI(boolean fbValue) {
        p_bWithUI = fbValue;
    }
    
    public String getMessage() {
        return p_sMessage;
    }

    public int getItemCount() {
        return p_oDetail.size();
    }

    public int getJobOrderCount() {
        return p_aJOList.size();
    }

    public Object getJOServiceStatus() {
        return p_oJOServiceStatus;
    }

    public Object getDetail(int fnRow, int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }
        if (getItemCount() == 0 || fnRow > getItemCount()) {
            return null;
        }

        p_oDetail.absolute(fnRow);

        return p_oDetail.getObject(fnIndex);

    }

    public Object getJobOrder(int fnRow, int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }
        if (getJobOrderCount() == 0 || fnRow > getJobOrderCount()) {
            return null;
        }

        p_aJOList.absolute(fnRow);

        return p_aJOList.getObject(fnIndex);

    }

    public Object getJOServiceStatus(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oJOServiceStatus.first();
        return p_oJOServiceStatus.getObject(fnIndex);
    }

    public Object getJOServiceStatus(String fsIndex) throws SQLException {
        return getJOServiceStatus(getColumnIndex(p_oJOServiceStatus, fsIndex));
    }

    public Object getDetail(int fnRow, String fsIndex) throws SQLException {
        return getDetail(fnRow, getColumnIndex(p_oDetail, fsIndex));
    }

    public Object getJobOrder(int fnRow, String fsIndex) throws SQLException {
        return getJobOrder(fnRow, getColumnIndex(p_aJOList, fsIndex));
    }

    public void setJobOrder(int fnRow, int fnIndex, Object foValue) throws SQLException {
        if (getJobOrderCount() == 0 || fnRow > getJobOrderCount()) {
            return;
        }

        p_aJOList.absolute(fnRow);
        switch (fnIndex) {
            case 41://nRemainxx
                if (foValue instanceof Number) {
                    p_aJOList.updateObject(41, foValue);
                }

                break;

        }

        p_aJOList.updateRow();
    }

    public void setJobOrder(int fnRow, String fsIndex, Object foValue) throws SQLException {
        setJobOrder(fnRow, getColumnIndex(p_aJOList, fsIndex), foValue);
    }

    public Object getMaster(int fnIndex) throws SQLException {
        if (fnIndex == 0) {
            return null;
        }

        p_oMaster.first();
        return p_oMaster.getObject(fnIndex);
    }

    public Object getMaster(String fsIndex) throws SQLException {
        return getMaster(getColumnIndex(fsIndex));
    }

    public boolean initialize() throws SQLException {
        String lsSQL;
        RowSetFactory factory = RowSetProvider.newFactory();
        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        ResultSet loRS;

        //initialize rowset
        lsSQL = MiscUtil.addCondition(getSQ_Master(), "1=0");
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);

        lsSQL = getSQ_Detail();
        System.err.println("Retrieve Query = " + lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);

        if (getItemCount() == 0) {
            p_sMessage = "No incentive record to release.";
            return false;
        }
        return true;
    }

  
    public boolean OpenTransaction(String fsTransNox) throws SQLException {

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        if (!saveServiceBay()) {
            return false;
        }
        
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        //open master
        lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oMaster = factory.createCachedRowSet();
        p_oMaster.populate(loRS);
        MiscUtil.close(loRS);

        if (p_oMaster.size() == 0) {
            p_sMessage = "No transaction to open.";
            return false;
        }

        lsSQL = MiscUtil.addCondition(getSQ_Detail(), "sTransNox = " + SQLUtil.toSQL(fsTransNox));
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oDetail = factory.createCachedRowSet();
        p_oDetail.populate(loRS);
        MiscUtil.close(loRS);

        MiscUtil.close(loRS);

        return true;
    }

    private boolean isEntryOK() {
//        if (p_oDetail.size() == 0) {
//            return false;
//        }
        return true;
    }

    public boolean RetrieveJobOrderList() throws SQLException {

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";
        //save service bay if already existing
        if (!saveServiceBay()) {
            return false;
        }
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox LIKE " + SQLUtil.toSQL(p_oApp.getBranchCode() + "%")
                + " AND cTranStat NOT IN ('4','3') AND nEstTimex > 0 ORDER BY sTransNox");
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_aJOList = factory.createCachedRowSet();
        p_aJOList.populate(loRS);
        MiscUtil.close(loRS);

        MiscUtil.close(loRS);

        if (p_aJOList.size() == 0) {
            p_sMessage = "No transaction to open.";
            return false;
        }

        return true;
    }

    public boolean RetrieveJobOrderListRemaining() throws SQLException {

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";
        //save service bay if already existing
        if (!saveServiceBay()) {
            return false;
        }
        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        lsSQL = MiscUtil.addCondition(getSQ_Master(), "sTransNox LIKE " + SQLUtil.toSQL(p_oApp.getBranchCode() + "%")
                + " AND cTranStat NOT IN ('4','3') AND nEstTimex > 0 AND dJobEndxx IS NULL ORDER BY sTransNox");
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_aJOList = factory.createCachedRowSet();
        p_aJOList.populate(loRS);
        MiscUtil.close(loRS);

        MiscUtil.close(loRS);

        if (p_aJOList.size() == 0) {
            p_sMessage = "No transaction to open.";
            return false;
        }

        return true;
    }

    public boolean RetrieveServiceStatus() throws SQLException {

        if (p_oApp == null) {
            p_sMessage = "Application driver is not set.";
            return false;
        }

        p_sMessage = "";

        String lsSQL;
        ResultSet loRS;
        RowSetFactory factory = RowSetProvider.newFactory();

        lsSQL
                = "SELECT "
                + " fn.sFinished, "
                + " fn.nFnshTime, "
                + " sb.sOnGoingx, "
                + " sb.nOnGgTime, "
                + " sb.sTotalPitx, "
                + " q.sQueCount, "
                + " q.nQueTimex "
                + "FROM "
                /* FINISHED */
                + " (SELECT "
                + "     COUNT(*) AS sFinished, "
                + "     IFNULL(SUM(nEstTimex),0) AS nFnshTime "
                + "  FROM JobOrderBranch_Master "
                + "  WHERE  dJobEndxx IS NOT NULL "
                + "  AND cTranStat IN ('4','2','0') "
                + " AND sTransNox LIKE " + SQLUtil.toSQL(p_oApp.getBranchCode() + "%") + ") fn, "
                /* SERVICE BAY */
                + " (SELECT "
                + "     COUNT(*) AS sTotalPitx, "
                + "     SUM(CASE WHEN sReferNox <> '' THEN 1 ELSE 0 END) AS sOnGoingx, "
                + "     IFNULL(SUM(CASE WHEN sReferNox <> '' THEN nRemainxx ELSE 0 END),0) AS nOnGgTime "
                + "  FROM Service_Bay "
                + "  WHERE cRecdStat = '1') sb, "
                /* QUEUE */
                + " (SELECT "
                + "     COUNT(*) AS sQueCount, "
                + "     IFNULL(SUM(nEstTimex),0) AS nQueTimex "
                + "  FROM JobOrderBranch_Master "
                + "  WHERE cTranStat NOT IN ('4','3') "
                + "  AND nEstTimex > 0 "
                + " AND sTransNox LIKE " + SQLUtil.toSQL(p_oApp.getBranchCode() + "%") + ")  q";
        System.out.println(lsSQL);
        loRS = p_oApp.executeQuery(lsSQL);
        p_oJOServiceStatus = factory.createCachedRowSet();
        p_oJOServiceStatus.populate(loRS);
        MiscUtil.close(loRS);

        MiscUtil.close(loRS);

        if (p_oJOServiceStatus.size() == 0) {
            p_sMessage = "No transaction to open.";
            return false;
        }

        return true;
    }

    private String getSQ_Master() {
        return "SELECT"
                + "  a.sTransNox"
                + ", a.dTransact"
                + ", a.nKmReadng"
                + ", a.sDealerxx"
                + ", a.sMechanic"
                + ", a.cBusiness"
                + ", a.sJobDescr"
                + ", a.sCtrlNoxx"
                + ", a.cCardType"
                + ", a.cCouponTp"
                + ", a.sCouponNo"
                + ", a.nNoCoupon"
                + ", a.nTranTotl"
                + ", a.sClientID"
                + ", a.sSerialID"
                + ", a.nEntryNox"
                + ", a.sComboCde"
                + ", a.sCardIDxx"
                + ", a.sGCardNox"
                + ", a.cPrintedx"
                + ", a.cTranStat"
                + ", a.sSalesInv"
                + ", a.sReleased"
                + ", a.dReleased"
                + ", a.sPaymRecv"
                + ", a.dPaymRecv"
                + ", a.nGiftCpnx"
                + ", a.nAmtPaidx"
                + ", a.nEstTimex"
                + ", a.dJobStart"
                + ", IFNULL(a.dJobEndxx,'') dJobEndxx"
                + ", a.cPostedxx"
                + ", a.sModified"
                + ", a.dModified"
                + ", b.sCompnyNm xMechncNm"
                + ", c.sCompnyNm xClientNm"
                + ", e.sModelNme  "
                + ", IFNULL(f.sReferNox,'') sReferNox"
                + ", IFNULL(f.sPITNmbrx,'0') sPITNmbrx"
                + ", IFNULL(f.cPausedxx,'0') cPausedxx"
                + ", IFNULL(f.nRemainxx, 0.0) nRemainxx"
                + ", IFNULL(f.dResumedx,'') dResumedx"
                + ", IFNULL(f.sPITIDxxx,'') sPITIDxxx"
                + " FROM " + MASTER_TABLE + " a "
                + " LEFT JOIN Client_Master b ON a.sClientID = b.sClientID"
                + " LEFT JOIN Client_Master c ON a.sMechanic = c.sClientID"
                + " LEFT JOIN MC_Serial d ON a.sSerialID = d.sSerialID"
                + " LEFT JOIN MC_Model e ON d.sModelIDx = e.sModelIDx"
                + " LEFT JOIN Service_Bay f ON a.sTransNox = f.sReferNox";
    }

    private String getSQ_Detail() throws SQLException {
        String lsSQL = "SELECT "
                + " a.sTransNox"
                + ", a.nEntryNox"
                + ", a.sReferNox"
                + ", a.sDescript"
                + ", a.nQuantity"
                + ", a.nUnitPrce"
                + ", a.nEstTimex"
                + ", a.nDiscount"
                + ", a.nAddDiscx"
                + ", a.cLaborxxx"
                + ", a.cLaborTyp"
                + ", a.dModified"
                + " FROM " + DETAIL_TABLE + " a ";

        return lsSQL;
    }

    private int getColumnIndex(String fsValue) throws SQLException {
        int lnIndex = 0;
        int lnRow = p_oMaster.getMetaData().getColumnCount();

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            if (fsValue.equals(p_oMaster.getMetaData().getColumnLabel(lnCtr))) {
                lnIndex = lnCtr;
                break;
            }
        }

        return lnIndex;
    }

    public void displayMasFields() throws SQLException {

        int lnRow = p_oMaster.getMetaData().getColumnCount();

        System.out.println("----------------------------------------");
        System.out.println("MASTER TABLE INFO");
        System.out.println("----------------------------------------");
        System.out.println("Total number of columns: " + lnRow);
        System.out.println("----------------------------------------");

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            System.out.println("Column index: " + (lnCtr) + " --> Label: " + p_oMaster.getMetaData().getColumnLabel(lnCtr));
            if (p_oMaster.getMetaData().getColumnType(lnCtr) == Types.CHAR
                    || p_oMaster.getMetaData().getColumnType(lnCtr) == Types.VARCHAR) {

                System.out.println("Column index: " + (lnCtr) + " --> Size: " + p_oMaster.getMetaData().getColumnDisplaySize(lnCtr));
            }
        }

        System.out.println("----------------------------------------");
        System.out.println("END: MASTER TABLE INFO");
        System.out.println("----------------------------------------");
    }

    private int getColumnIndex(CachedRowSet loRS, String fsValue) throws SQLException {
        int lnIndex = 0;
        int lnRow = loRS.getMetaData().getColumnCount();

        for (int lnCtr = 1; lnCtr <= lnRow; lnCtr++) {
            if (fsValue.equals(loRS.getMetaData().getColumnLabel(lnCtr))) {
                lnIndex = lnCtr;
                break;
            }
        }

        return lnIndex;
    }

    public boolean showPitSelection() throws SQLException {
        if (p_oMaster == null) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        p_oMaster.first();
        if (p_oMaster.getString("sTransNox") == null
                || p_oMaster.getString("sTransNox").isEmpty()) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        //avoid starting twice 
        if (!p_oMaster.getString("sReferNox").isEmpty()) {
            p_sMessage = "Record Already Started!";
            return false;
        }

        if (!saveServiceBay()) {
            return false;
        }
        String lsSQL = "SELECT"
                + " sPITIDxxx"
                + ", sPITNmbrx"
                + ", sReferNox"
                + ", cPausedxx"
                + ", dPausedxx"
                + ", dResumedx"
                + ", nRemainxx"
                + ", cRecdStat"
                + " FROM Service_Bay WHERE (sReferNox ='' OR sReferNox IS NULL) "
                + " AND cRecdStat = " + SQLUtil.toSQL(RecordStatus.ACTIVE);
        try {
            System.err.println(lsSQL);
            ResultSet loRS = p_oApp.executeQuery(lsSQL);

            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }
            if (MiscUtil.RecordCount(loRS) == 1) {
                loRS.first();
                lsSQL = "UPDATE Service_Bay "
                        + " SET sReferNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"))
                        + ", nRemainxx = " + SQLUtil.toSQL(p_oMaster.getDouble("nEstTimex"))
                        + " WHERE sPITIDxxx=  " + SQLUtil.toSQL(loRS.getString("sPITIDxxx"));

                System.err.println(lsSQL);
                if (p_oApp.executeUpdate(lsSQL) <= 0) {
                    if (!p_bWithParent) {
                        p_oApp.rollbackTrans();
                    }
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }

                lsSQL = "UPDATE JobOrderBranch_Master "
                        + " SET dJobStart = " + SQLUtil.toSQL(p_oApp.getServerDate())
                        + "WHERE  sTransNox =  " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

                System.err.println(lsSQL);
                if (p_oApp.executeUpdate(lsSQL) <= 0) {
                    if (!p_bWithParent) {
                        p_oApp.rollbackTrans();
                    }
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
                if (!p_bWithParent) {
                    p_oApp.commitTrans();
                }
                return true;
            }

            if (MiscUtil.RecordCount(loRS) > 1) {
                loRS.first();

                PITSelection loSearch = new PITSelection();
                loSearch.setGRider(p_oApp);
                loSearch.setResultSet(loRS);
                loSearch.setSQLSource(lsSQL);

                CommonUtils.showModal(loSearch);
                String lsResult = loSearch.getResult();

                //check if not empty
                if (lsResult != null) {
                    if (!lsResult.isEmpty()) {
                        lsSQL = "UPDATE Service_Bay "
                                + " SET sReferNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"))
                                + ", nRemainxx = " + SQLUtil.toSQL(p_oMaster.getDouble("nEstTimex"))
                                + " WHERE sPITIDxxx=  " + SQLUtil.toSQL(lsResult);

                        System.err.println(lsSQL);
                        if (p_oApp.executeUpdate(lsSQL) <= 0) {
                            if (!p_bWithParent) {
                                p_oApp.rollbackTrans();
                            }
                            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                            return false;
                        }

                        lsSQL = "UPDATE JobOrderBranch_Master"
                                + " SET dJobStart = " + SQLUtil.toSQL(p_oApp.getServerDate())
                                + "WHERE  sTransNox =  " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

                        System.err.println(lsSQL);
                        if (p_oApp.executeUpdate(lsSQL) <= 0) {
                            if (!p_bWithParent) {
                                p_oApp.rollbackTrans();
                            }
                            p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                            return false;
                        }

                        if (!p_bWithParent) {
                            p_oApp.commitTrans();
                        }
                        return true;
                    } else {

                        p_sMessage = "No Service PIT Selected!";
                        return false;
                    }
                }
                p_sMessage = "Unable to Retrieve PIT Selected!";
                return false;
            }
        } catch (Exception ex) {
            Logger.getLogger(JobOrder.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        p_sMessage = "No Service Bay Available!";
        return false;
    }

    public boolean PauseService() throws SQLException {
        if (p_oMaster == null) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        p_oMaster.first();
        if (p_oMaster.getString("sTransNox") == null
                || p_oMaster.getString("sTransNox").isEmpty()) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        //avoid pausing twice 
        if (!p_oMaster.getString("sReferNox").isEmpty() && p_oMaster.getString("cPausedxx").equals("1")) {
            p_sMessage = "Record Already Paused!";
            return false;
        }
        if (!saveServiceBay()) {
            return false;
        }
        String lsSQL;
        try {

            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }
            lsSQL = "UPDATE Service_Bay "
                    + " SET cPausedxx = " + SQLUtil.toSQL(RecordStatus.ACTIVE)
                    + ", dPausedxx = " + SQLUtil.toSQL(p_oApp.getServerDate())
                    + ", nRemainxx = " + SQLUtil.toSQL(p_oMaster.getDouble("nRemainxx"))
                    + " WHERE sPITIDxxx=  " + SQLUtil.toSQL(p_oMaster.getString("sPITIDxxx"))
                    + " AND sReferNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

            System.err.println(lsSQL);
            if (p_oApp.executeUpdate(lsSQL) <= 0) {
                if (!p_bWithParent) {
                    p_oApp.rollbackTrans();
                }
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }

            if (!p_bWithParent) {
                p_oApp.commitTrans();
            }
            return true;

        } catch (Exception ex) {
            Logger.getLogger(JobOrder.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    public boolean ResumeService() throws SQLException {
        if (p_oMaster == null) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        p_oMaster.first();
        if (p_oMaster.getString("sTransNox") == null
                || p_oMaster.getString("sTransNox").isEmpty()) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        //avoid resume twice 
        if (!p_oMaster.getString("sReferNox").isEmpty() && p_oMaster.getString("cPausedxx").equals("0")) {
            p_sMessage = "Record Already Paused!";
            return false;
        }

        if (!saveServiceBay()) {
            return false;
        }

        String lsSQL;
        try {

            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }
            lsSQL = "UPDATE Service_Bay "
                    + " SET cPausedxx = " + SQLUtil.toSQL(RecordStatus.INACTIVE)
                    + ", dResumedx = " + SQLUtil.toSQL(p_oApp.getServerDate())
                    + ", nRemainxx = " + SQLUtil.toSQL(p_oMaster.getDouble("nRemainxx"))
                    + " WHERE sPITIDxxx=  " + SQLUtil.toSQL(p_oMaster.getString("sPITIDxxx"))
                    + " AND sReferNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

            System.err.println(lsSQL);
            if (p_oApp.executeUpdate(lsSQL) <= 0) {
                if (!p_bWithParent) {
                    p_oApp.rollbackTrans();
                }
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }

            if (!p_bWithParent) {
                p_oApp.commitTrans();
            }
            return true;

        } catch (Exception ex) {
            Logger.getLogger(JobOrder.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    public boolean FinishService() throws SQLException {
        if (p_oMaster == null) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        p_oMaster.first();
        if (p_oMaster.getString("sTransNox") == null
                || p_oMaster.getString("sTransNox").isEmpty()) {
            p_sMessage = "No Record is Selected";
            return false;
        }
        //avoid pausing twice 
        if (p_oMaster.getString("sReferNox").isEmpty()) {
            p_sMessage = "Record is not IN - SERVICE!";
            return false;
        }

        String lsSQL;
        try {

            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }
            lsSQL = "UPDATE Service_Bay "
                    + " SET cPausedxx = " + SQLUtil.toSQL(RecordStatus.INACTIVE)
                    + ", dPausedxx =  NULL "
                    + ", dResumedx =  NULL "
                    + ", nRemainxx =  0.0 "
                    + ", sReferNox =  '' "
                    + " WHERE sPITIDxxx=  " + SQLUtil.toSQL(p_oMaster.getString("sPITIDxxx"))
                    + " AND sReferNox = " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

            System.err.println(lsSQL);
            if (p_oApp.executeUpdate(lsSQL) <= 0) {
                if (!p_bWithParent) {
                    p_oApp.rollbackTrans();
                }
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }

            lsSQL = "UPDATE JobOrderBranch_Master"
                    + " SET dJobEndxx = " + SQLUtil.toSQL(p_oApp.getServerDate())
                    + "WHERE  sTransNox =  " + SQLUtil.toSQL(p_oMaster.getString("sTransNox"));

            System.err.println(lsSQL);
            if (p_oApp.executeUpdate(lsSQL) <= 0) {
                if (!p_bWithParent) {
                    p_oApp.rollbackTrans();
                }
                p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                return false;
            }

            if (!p_bWithParent) {
                p_oApp.commitTrans();
            }
            return true;

        } catch (Exception ex) {
            Logger.getLogger(JobOrder.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }

    public boolean saveServiceBay() {
        if (p_aJOList == null) {
            return true;
        }
        if (getJobOrderCount() <= 0) {
            return true;
        }

        String lsSQL;
        try {
            if (!p_bWithParent) {
                p_oApp.beginTrans();
            }
            for (int lnRow = 1; lnRow <= getJobOrderCount(); lnRow++) {

                p_aJOList.absolute(lnRow);
                if (p_aJOList.getString("sReferNox") == null
                        || p_aJOList.getString("sReferNox").isEmpty()) {
                    continue;
                }
                if (p_aJOList.getString("cPausedxx").equalsIgnoreCase("1")) {
                    continue;
                }
                lsSQL = "UPDATE Service_Bay "
                        + " SET nRemainxx = " + SQLUtil.toSQL(p_aJOList.getObject("nRemainxx"))
                        + " WHERE sPITIDxxx=  " + SQLUtil.toSQL(p_aJOList.getString("sPITIDxxx"))
                        + " AND sReferNox = " + SQLUtil.toSQL(p_aJOList.getString("sTransNox"));

                System.err.println(lsSQL);
                if (p_oApp.executeUpdate(lsSQL) <= 0) {
                    if (!p_bWithParent) {
                        p_oApp.rollbackTrans();
                    }
                    p_sMessage = p_oApp.getMessage() + ";" + p_oApp.getErrMsg();
                    return false;
                }
            }
            if (!p_bWithParent) {
                p_oApp.commitTrans();
            }
            return true;

        } catch (Exception ex) {
            Logger.getLogger(JobOrder.class.getName()).log(Level.SEVERE, null, ex);
            p_sMessage = ex.getMessage();
            return false;
        }
    }
}
