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
    assertThat(np.getExtraInstallOptions()).isEmpty();
    assertThat(np.getSetupCommands()).isEmpty();
    assertThat(np.getCleanupCommands()).isEmpty();
  }

  @Test
  void testConstructorWithAllFields() {
    NativePackage np = new NativePackage(NativePackageManager.APT, List.of("pkg1"), List.of("--opt"), List.of("setup"), List.of("cleanup"));

    assertThat(np.getExtraInstallOptions()).containsExactly("--opt");
    assertThat(np.getSetupCommands()).containsExactly("setup");
    assertThat(np.getCleanupCommands()).containsExactly("cleanup");
  }

  @Test
  void testGetPackages() {
    NativePackage np = new NativePackage(NativePackageManager.APT, List.of("pkg1"), null, null, null);

    assertThat(np.getPackages()).containsExactly("pkg1");
  }

  @Test
  void testNullSafeGetters() {
    NativePackage np = new NativePackage(NativePackageManager.APT, List.of("pkg1"), null, null, null);

    assertThat(np.getExtraInstallOptions()).isEmpty();
    assertThat(np.getSetupCommands()).isEmpty();
    assertThat(np.getCleanupCommands()).isEmpty();
  }

  @Test
  void testInstallDelegatesToPackageManager() {
    NativePackage np = NativePackage.of(NativePackageManager.APT, "pkg1");

    var cmd = np.install(null);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(cmd.commands()).containsExactly("sudo apt install -y pkg1");
  }

  @Test
  void testUninstallDelegatesToPackageManager() {
    NativePackage np = NativePackage.of(NativePackageManager.APT, "pkg1");

    var cmd = np.uninstall();

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(cmd.commands()).containsExactly("sudo apt -y autoremove --purge pkg1");
  }
}
