package com.devonfw.tools.ide.url.updater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.url.model.folder.UrlVersion;

/**
 * Tests for the CPE registry support in {@link AbstractUrlUpdater}.
 */
class AbstractUrlUpdaterCpeRegistryTest {

  @Test
  void testDefaultRegistryUsesToolName() {

    DefaultCpeUpdater updater = new DefaultCpeUpdater();

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();

    assertThat(updater.getCpeVendor()).isEqualTo("sample");
    assertThat(updater.getCpeProduct()).isEqualTo("sample");
    assertThat(cpe.getPrimaryVendor()).isEqualTo("sample");
    assertThat(cpe.getPrimaryProduct()).isEqualTo("sample");
    assertThat(cpe.getVendors()).containsExactly("sample");
    assertThat(cpe.getProducts()).containsExactly("sample");
    assertThat(updater.matchesCpe("sample", "sample")).isTrue();
    assertThat(updater.matchesCpe("sample", "other")).isFalse();
  }

  @Test
  void testRegistrySupportsExactAndInfixAliases() {

    AliasedCpeUpdater updater = new AliasedCpeUpdater();

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();

    assertThat(cpe.getPrimaryVendor()).isEqualTo("astral");
    assertThat(cpe.getPrimaryProduct()).isEqualTo("uv");
    assertThat(cpe.getVendors()).containsExactly("astral", "astral-sh");
    assertThat(cpe.getProducts()).containsExactly("uv", "uv-cli");
    assertThat(updater.matchesCpe("astral", "uv")).isTrue();
    assertThat(updater.matchesCpe("astral-sh", "uv")).isTrue();
    assertThat(updater.matchesCpe("astral", "uv-cli-preview")).isTrue();
    assertThat(updater.matchesCpe("foo", "uv")).isFalse();
    assertThat(updater.matchesCpe("astral-sh", "other")).isFalse();
  }

  @Test
  void testLegacyRegistryUsesLegacyCpeVendorAndProduct() {

    LegacyCpeUpdater updater = new LegacyCpeUpdater();

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();

    assertThat(updater.getCpeVendor()).isEqualTo("bar");
    assertThat(updater.getCpeProduct()).isEqualTo("foo");
    assertThat(cpe.getPrimaryVendor()).isEqualTo("bar");
    assertThat(cpe.getPrimaryProduct()).isEqualTo("foo");
    assertThat(updater.matchesCpe("bar", "foo")).isTrue();
    assertThat(updater.matchesCpe("BAR", "FOO")).isTrue();
    assertThat(updater.matchesCpe("bar", "other")).isFalse();
  }


  @Test
  void testRegistryFailsWithoutVendorOrProduct() {

    BrokenCpeUpdater updater = new BrokenCpeUpdater();

    assertThatThrownBy(updater::getCpeRegistry).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CPE vendor");
  }

  private static class DefaultCpeUpdater extends AbstractUrlUpdater {

    @Override
    public String getTool() {

      return "sample";
    }

    @Override
    protected Set<String> getVersions() {

      return Set.of();
    }

    @Override
    protected void addVersion(UrlVersion urlVersion) {

      // not needed for these tests
    }

    @Override
    protected String getDownloadBaseUrl() {

      return "https://example.org";
    }

    @Override
    protected String getVersionBaseUrl() {

      return "https://example.org";
    }
  }
  
  private static final class AliasedCpeUpdater extends DefaultCpeUpdater {

    @Override
    protected void initCpe(CpeRegistry cpe) {

      cpe.addVendor("astral");
      cpe.addVendorInfix("astral-sh");
      cpe.addProduct("uv");
      cpe.addProductInfix("uv-cli");
    }
  }

  private static final class LegacyCpeUpdater extends DefaultCpeUpdater {

    @Override
    public String getCpeVendor() {

      return "bar";
    }

    @Override
    public String getCpeProduct() {

      return "foo";
    }
  }

  private static final class BrokenCpeUpdater extends DefaultCpeUpdater {

    @Override
    protected void initCpe(CpeRegistry cpe) {

      // intentionally left empty
    }
  }
}

