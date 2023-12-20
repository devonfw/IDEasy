package com.devonfw.tools.ide.url.model;

public final class UrlErrorState {

  private final String toolWithEdition;
  private int additionSuccesses;
  private int additionFailures;
  private int verificationSuccesses;
  private int verificationFailures;

  public UrlErrorState(String toolWithEdition) {

    this.toolWithEdition = toolWithEdition;
    this.additionSuccesses = 0;
    this.additionFailures = 0;
    this.verificationSuccesses = 0;
    this.verificationFailures = 0;
  }

  public String getToolWithEdition() {

    return toolWithEdition;
  }

  public int getAdditionSuccesses() {

    return additionSuccesses;
  }

  public void setAdditionSuccesses(int additionSuccesses) {

    this.additionSuccesses = additionSuccesses;
  }

  public int getAdditionFailures() {

    return additionFailures;
  }

  public void setAdditionFailures(int additionFailures) {

    this.additionFailures = additionFailures;
  }

  public int getVerificationSuccesses() {

    return verificationSuccesses;
  }

  public void setVerificationSuccesses(int verificationSuccesses) {

    this.verificationSuccesses = verificationSuccesses;
  }

  public int getVerificationFailures() {

    return verificationFailures;
  }

  public void setVerificationFailures(int verificationFailures) {

    this.verificationFailures = verificationFailures;
  }

  private int getTotalAdditions() {
    return this.additionFailures + this.additionSuccesses;
  }
  private int getTotalVerification() {
    return this.verificationFailures + this.verificationSuccesses;
  }

  private String getErrorRate(int failures, int totals) {
    if (failures == 0) {
      return "0.00";
    } else {
      double errorRate = (double) failures / totals * 100;
      return String.format("%.2f", errorRate);
    }
  }
  public void updateAdditions(boolean success) {
    if (success) {
      this.additionSuccesses += 1;
    } else {
      this.additionFailures += 1;
    }
  }

  public void updateVerifications(boolean success) {
    if (success) {
      this.verificationSuccesses += 1;
    } else {
      this.verificationFailures += 1;
    }
  }

  @Override
  public String toString() {

    String additionState = "versions added: " + getAdditionFailures() + " failed, " + getAdditionSuccesses() + " succeeded, "
        + getTotalAdditions() + " total, " + getErrorRate(getAdditionFailures(), getTotalAdditions()) + "% error";

    String verificationState = " - versions verified: " + getVerificationFailures() + " failed, " + getVerificationSuccesses() +
        " succeeded, " + getTotalVerification() + " total, " + getErrorRate(getVerificationFailures(), getTotalVerification()) + "% error";

    return getToolWithEdition() + ": " + additionState + verificationState;
  }
}
