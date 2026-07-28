package com.devonfw.tools.ide.tool;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test of {@link NativePackageManager}.
 */
class NativePackageManagerTest extends Assertions {

  /**
   * Test of {@link NativePackageManager#extractPackageManager(String)} for every supported package manager.
   */
  @Test
  void testExtractPackageManager() {

    assertThat(NativePackageManager.extractPackageManager("sudo apt install -y rancher-desktop")).isEqualTo(NativePackageManager.APT);
    assertThat(NativePackageManager.extractPackageManager("yum install rancher-desktop")).isEqualTo(NativePackageManager.YUM);
    assertThat(NativePackageManager.extractPackageManager("sudo zypper install rancher-desktop")).isEqualTo(NativePackageManager.ZYPPER);
    assertThat(NativePackageManager.extractPackageManager("dnf install rancher-desktop")).isEqualTo(NativePackageManager.DNF);
    assertThat(NativePackageManager.extractPackageManager("sudo pacman -Rs --noconfirm rancher-desktop")).isEqualTo(NativePackageManager.PACMAN);
  }

  /**
   * Test of {@link NativePackageManager#extractPackageManager(String)} for a command without a recognized package manager.
   */
  @Test
  void testExtractPackageManagerThrowsForUnknownCommand() {

    assertThatThrownBy(() -> NativePackageManager.extractPackageManager("brew install rancher-desktop")).isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Test of {@link NativePackageManager#getBinaryName()}.
   */
  @Test
  void testGetBinaryName() {

    assertThat(NativePackageManager.APT.getBinaryName()).isEqualTo("apt");
    assertThat(NativePackageManager.PACMAN.getBinaryName()).isEqualTo("pacman");
  }
}
