package com.devonfw.tools.ide.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link NativePackage}.
 */
class NativePackageTest {

  @Test
  void testOfFactoryMethod() {
    NativePackage np = NativePackage.of(NativePackageManager.APT, "pkg1", "pkg2");

    assertThat(np.getPackageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(np.getPackages()).containsExactly("pkg1", "pkg2");
    assertThat(np.getVersion()).isNull();
    assertThat(np.getExtraInstallOptions()).isEmpty();
    assertThat(np.getSetupCommands()).isEmpty();
    assertThat(np.getCleanupCommands()).isEmpty();
  }

  @Test
  void testOfWithVersionFactoryMethod() {
    NativePackage np = NativePackage.ofVersion(NativePackageManager.APT, "1.0.0", "pkg1", "pkg2");

    assertThat(np.getPackageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(np.getPackages()).containsExactly("pkg1", "pkg2");
    assertThat(np.getVersion()).isEqualTo("1.0.0");
  }

  @Test
  void testConstructorWithAllFields() {
    NativePackage np = new NativePackage(NativePackageManager.APT, List.of("pkg1"), "1.0.0", List.of("--opt"), List.of("setup"), List.of("cleanup"));

    assertThat(np.getExtraInstallOptions()).containsExactly("--opt");
    assertThat(np.getSetupCommands()).containsExactly("setup");
    assertThat(np.getCleanupCommands()).containsExactly("cleanup");
  }

  @Test
  void testGetPackagesWithVersion() {
    NativePackage np = NativePackage.ofVersion(NativePackageManager.APT, "1.0.0", "pkg1", "pkg2");

    assertThat(np.getPackagesWithVersion()).containsExactly("pkg1=1.0.0", "pkg2=1.0.0");
  }

  @Test
  void testGetPackagesWithVersionYum() {
    NativePackage np = NativePackage.ofVersion(NativePackageManager.YUM, "1.0.0", "pkg1");

    assertThat(np.getPackagesWithVersion()).containsExactly("pkg1-1.0.0");
  }

  @Test
  void testGetPackagesWithVersionNoVersion() {
    NativePackage np = NativePackage.of(NativePackageManager.APT, "pkg1", "pkg2");

    assertThat(np.getPackagesWithVersion()).containsExactly("pkg1", "pkg2");
  }

  @Test
  void testGetPackagesWithVersionBlankVersion() {
    NativePackage np = new NativePackage(NativePackageManager.APT, List.of("pkg1"), "   ", null, null, null);

    assertThat(np.getPackagesWithVersion()).containsExactly("pkg1");
  }

  @Test
  void testNullSafeGetters() {
    NativePackage np = new NativePackage(NativePackageManager.APT, List.of("pkg1"), null, null, null, null);

    assertThat(np.getExtraInstallOptions()).isEmpty();
    assertThat(np.getSetupCommands()).isEmpty();
    assertThat(np.getCleanupCommands()).isEmpty();
  }

  @Test
  void testInstallDelegatesToPackageManager() {
    NativePackage np = NativePackage.of(NativePackageManager.APT, "pkg1");

    var cmd = np.install();

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(cmd.commands()).containsExactly("sudo apt install -y pkg1");
  }

  @Test
  void testUninstallDelegatesToPackageManager() {
    NativePackage np = NativePackage.of(NativePackageManager.APT, "pkg1");

    var cmd = np.uninstall();

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(cmd.commands()).containsExactly("sudo apt autoremove -y pkg1");
  }
}
