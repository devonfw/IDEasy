package com.devonfw.tools.ide.url.model.report;

import java.util.ArrayList;
import java.util.List;

/**
 * An instance of this class represent the result of updating a tool with specific url updater. It counts the number of successful and failed versions and
 * verifications.
 */
public class UrlUpdaterReport {

  private String tool;
  private String edition;
  private int addVersionSuccess = 0;
  private int addVersionFailure = 0;
  private int verificationSuccess = 0;
  private int verificationFailure = 0;
  private ArrayList<UrlError> urlErrors = new ArrayList<>();

  /**
   * The constructor.
   *
   * @param tool the name of the tool {@link #getTool() tool name}
   * @param edition the name of edition of the tool {@link #getEdition()} can be the same as the tool name if no editions exist.
   */
  public UrlUpdaterReport(String tool, String edition) {

    this.tool = tool;
    this.edition = edition;
  }

  public String getTool() {

    return tool;
  }

  public void setTool(String tool) {

    this.tool = tool;
  }

  public String getEdition() {

    return edition;
  }

  public void setEdition(String edition) {

    this.edition = edition;
  }

  public int getAddVersionSuccess() {

    return addVersionSuccess;
  }

  public void incrementAddVersionSuccess() {

    this.addVersionSuccess++;
  }

  public int getAddVersionFailure() {

    return addVersionFailure;
  }

  public void incrementAddVersionFailure() {

    this.addVersionFailure++;
  }

  public int getVerificationSuccess() {

    return verificationSuccess;
  }

  public void incrementVerificationSuccess() {

    this.verificationSuccess++;
  }

  public int getVerificationFailure() {

    return verificationFailure;
  }

  public void incrementVerificationFailure() {

    this.verificationFailure++;
  }

  public List<UrlError> getFailedUrls() {

    return urlErrors;
  }

  public void addUrlError(UrlError urlError) {

    this.urlErrors.add(urlError);
  }

  public int getTotalAdditions() {

    return this.addVersionFailure + this.addVersionSuccess;
  }

  public double getErrorRateAdditions() {

    if (this.addVersionFailure > 0 && this.addVersionSuccess > 0) {

      return ((double) this.addVersionFailure / getTotalAdditions()) * 100;
    } else {

      return 0;
    }
  }

  public int getTotalVerificitations() {

    return this.verificationFailure + this.verificationSuccess;
  }

  public double getErrorRateVerificiations() {

    System.out.println(this.verificationFailure + " " + this.verificationSuccess);

    if (this.verificationSuccess > 0 && this.verificationFailure > 0) {
      return ((double) this.verificationFailure / getTotalVerificitations()) * 100;
    } else {

      return 0;
    }
  }


}
