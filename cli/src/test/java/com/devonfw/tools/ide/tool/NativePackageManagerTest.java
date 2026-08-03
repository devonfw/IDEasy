package com.devonfw.tools.ide.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test of {@link NativePackageManager}.
 */
class NativePackageManagerTest {

  @Test
  void testAptInstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.APT,
        List.of("pkg1"),
        "1.0.0",
        List.of("--allow-downgrades"),
        List.of(
            "curl -s https://example.com/key.gpg | gpg --dearmor",
            "echo 'deb https://example.com/repository stable main'",
            "sudo apt update"),
        List.of(
            "sudo rm -f /etc/apt/sources.list.d/example.list",
            "sudo rm -f /usr/share/keyrings/example.gpg"));

    var cmd = NativePackageManager.APT.install(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(cmd.commands()).containsExactly(
        "curl -s https://example.com/key.gpg | gpg --dearmor",
        "echo 'deb https://example.com/repository stable main'",
        "sudo apt update",
        "sudo apt --allow-downgrades install -y pkg1=1.0.0");
  }

  @Test
  void testAptUninstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.APT,
        List.of("pkg1"),
        "1.0.0",
        List.of("--allow-downgrades"),
        List.of(
            "curl -s https://example.com/key.gpg | gpg --dearmor",
            "echo 'deb https://example.com/repository stable main'",
            "sudo apt update"),
        List.of(
            "sudo rm -f /etc/apt/sources.list.d/example.list",
            "sudo rm -f /usr/share/keyrings/example.gpg"));

    var cmd = NativePackageManager.APT.uninstall(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.APT);
    assertThat(cmd.commands()).containsExactly(
        "sudo apt autoremove -y pkg1",
        "sudo rm -f /etc/apt/sources.list.d/example.list",
        "sudo rm -f /usr/share/keyrings/example.gpg");
  }

  @Test
  void testZypperInstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.ZYPPER,
        List.of("pkg1"),
        "1.0.0",
        List.of("--no-gpg-checks"),
        List.of(
            "sudo zypper addrepo https://example.com/repo.repo",
            "sudo zypper refresh"),
        List.of(
            "sudo zypper removerepo example-repo"));

    var cmd = NativePackageManager.ZYPPER.install(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.ZYPPER);
    assertThat(cmd.commands()).containsExactly(
        "sudo zypper addrepo https://example.com/repo.repo",
        "sudo zypper refresh",
        "sudo zypper --no-gpg-checks --non-interactive install pkg1=1.0.0");
  }

  @Test
  void testZypperUninstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.ZYPPER,
        List.of("pkg1"),
        "1.0.0",
        List.of("--no-gpg-checks"),
        List.of(
            "sudo zypper addrepo https://example.com/repo.repo",
            "sudo zypper refresh"),
        List.of(
            "sudo zypper removerepo example-repo"));

    var cmd = NativePackageManager.ZYPPER.uninstall(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.ZYPPER);
    assertThat(cmd.commands()).containsExactly(
        "sudo zypper --non-interactive remove --clean-deps pkg1",
        "sudo zypper removerepo example-repo");
  }

  @Test
  void testYumInstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.YUM,
        List.of("pkg1"),
        "1.0.0",
        List.of("--skip-broken"),
        List.of(
            "sudo yum-config-manager --add-repo https://example.com/repo.repo",
            "sudo yum makecache"),
        List.of(
            "sudo rm -f /etc/yum.repos.d/example.repo"));

    var cmd = NativePackageManager.YUM.install(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.YUM);
    assertThat(cmd.commands()).containsExactly(
        "sudo yum-config-manager --add-repo https://example.com/repo.repo",
        "sudo yum makecache",
        "sudo yum --skip-broken install -y pkg1-1.0.0");
  }

  @Test
  void testYumUninstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.YUM,
        List.of("pkg1"),
        "1.0.0",
        List.of("--skip-broken"),
        List.of("sudo yum-config-manager --add-repo https://example.com/repo.repo", "sudo yum makecache"),
        List.of("sudo rm -f /etc/yum.repos.d/example.repo"));

    var cmd = NativePackageManager.YUM.uninstall(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.YUM);
    assertThat(cmd.commands()).containsExactly("sudo yum remove -y pkg1", "sudo rm -f /etc/yum.repos.d/example.repo");
  }


  @Test
  void testDnfInstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.DNF,
        List.of("pkg1"),
        "1.0.0",
        List.of("--refresh"),
        List.of("sudo dnf config-manager addrepo --from-repofile=https://example.com/repo.repo", "sudo dnf makecache"),
        List.of("sudo rm -f /etc/yum.repos.d/example.repo"));

    var cmd = NativePackageManager.DNF.install(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.DNF);
    assertThat(cmd.commands()).containsExactly("sudo dnf config-manager addrepo --from-repofile=https://example.com/repo.repo", "sudo dnf makecache",
        "sudo dnf --refresh install -y pkg1-1.0.0");
  }

  @Test
  void testDnfUninstallCommand() {
    NativePackage np = new NativePackage(
        NativePackageManager.DNF,
        List.of("pkg1"),
        "1.0.0",
        List.of("--refresh"),
        List.of("sudo dnf config-manager addrepo --from-repofile=https://example.com/repo.repo", "sudo dnf makecache"),
        List.of("sudo rm -f /etc/yum.repos.d/example.repo"));

    var cmd = NativePackageManager.DNF.uninstall(np);

    assertThat(cmd.packageManager()).isEqualTo(NativePackageManager.DNF);
    assertThat(cmd.commands()).containsExactly("sudo dnf remove -y pkg1", "sudo rm -f /etc/yum.repos.d/example.repo");
  }
}
