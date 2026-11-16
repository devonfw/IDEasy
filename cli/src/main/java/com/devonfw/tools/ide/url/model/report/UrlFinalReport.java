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
    StringBuilder result = new StringBuilder("\nSTART OF FINAL URL UPDATER REPORT\n");
    for (UrlUpdaterReport report : this.urlUpdaterReports) {
      result.append(report.getTool()).append("/").append(report.getEdition()).append(" versions added: ").append(report.getAddVersionFailure())
          .append(" failed, ").append(report.getAddVersionSuccess()).append(" succeeded, ").append(report.getTotalAdditions()).append(" total, ")
          .append(String.format("%.2f", report.getErrorRateAdditions())).append("% error - versions verified: ").append(report.getVerificationFailure())
          .append(" failed, ").append(report.getVerificationSuccess()).append(" succeeded, ").append(report.getTotalVerificitations()).append(" total, ")
          .append(String.format("%.2f", report.getErrorRateVerificiations())).append("% error").append("\n");
    }
    result.append("END OF FINAL URL UPDATER REPORT\n");
    return result.toString();
  }
}
