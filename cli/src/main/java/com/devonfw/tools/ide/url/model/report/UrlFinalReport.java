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
}
