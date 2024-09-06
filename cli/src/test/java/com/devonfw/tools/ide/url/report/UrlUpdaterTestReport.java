package com.devonfw.tools.ide.url.report;

import com.devonfw.tools.ide.url.model.report.UrlUpdaterReport;

/**
 * Test class for {@link UrlUpdaterReport} to create an instance with specific attributes
 */
public class UrlUpdaterTestReport extends UrlUpdaterReport {
  
  public UrlUpdaterTestReport(String tool, String edition, int addSuccess, int addFailure, int verificationSuccess, int verificationFailure) {

    super(tool, edition);
    this.addVersionSuccess = addSuccess;
    this.addVersionFailure = addFailure;
    this.verificationSuccess = verificationSuccess;
    this.verificationFailure = verificationFailure;
  }


}
