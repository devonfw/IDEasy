package com.devonfw.tools.ide.url.model.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to collect {@link UrlUpdaterReport} and finalize these after url updates have been completed.
 */
public class UrlFinalReport {

  private ArrayList<UrlUpdaterReport> urlUpdaterReports = new ArrayList<UrlUpdaterReport>();

  public List<UrlUpdaterReport> getUrlUpdaterReports() {
    return this.urlUpdaterReports;
  }

  public void addUrlUpdaterReport(UrlUpdaterReport urlUpdaterReport) {

    this.urlUpdaterReports.add(urlUpdaterReport);
  }

  /**
   * @return «tool»/«edition»: versions added: 5 failed, 7 succeeded, 13 total, 38,4% error - versions verified: 0 failed, 0 succeeded, 0 total, 0% error
   */
  @Override
  public String toString() {
    for (UrlUpdaterReport report : this.urlUpdaterReports) {
      return report.getTool() + "/" + report.getEdition() + " versions added: " + report.getAddVersionFailure() + " failed, "
          + report.getAddVersionSuccess() + " succeeded, " + report.getTotalAdditions() + " total, " + String.format("%.2f", report.getErrorRateAdditions())
          + "% error - versions verified: " + report.getVerificationFailure() + " failed, " + report.getVerificationSuccess() + " succeeded, "
          + report.getTotalVerificitations() + " total, " + String.format("%.2f", report.getErrorRateVerificiations()) + "% error";
    }
    return "";
  }

}
