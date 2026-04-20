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

    assertThat(cpe.getPrimaryVendor()).isEqualTo("sample");
    assertThat(cpe.getPrimaryProduct()).isEqualTo("sample");
    assertThat(updater.matchesCpe("sample", "sample")).isTrue();
    assertThat(updater.matchesCpe("sample", "other")).isFalse();
  }

  @Test
  void testRegistrySupportsExactAndInfixAliases() {

    AliasedCpeUpdater updater = new AliasedCpeUpdater();

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();

    assertThat(cpe.getPrimaryVendor()).isEqualTo("astral");
    assertThat(cpe.getPrimaryProduct()).isEqualTo("uv");
    assertThat(updater.matchesCpe("astral", "uv")).isTrue();
    assertThat(updater.matchesCpe("astral-sh", "uv")).isTrue();
    assertThat(updater.matchesCpe("foo", "uv")).isFalse();
    assertThat(updater.matchesCpe("astral-sh", "other")).isFalse();
  }

  @Test
  void testRegistryFailsWithoutVendorOrProduct() {

    BrokenCpeUpdater updater = new BrokenCpeUpdater();

    assertThatThrownBy(updater::getCpeRegistry).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CPE vendor");
  }

  @Test
  void testGetVendorsAndProducts() {

    AliasedCpeUpdater updater = new AliasedCpeUpdater();

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();

    assertThat(cpe.getVendors()).containsExactly("astral", "astral-sh");
    assertThat(cpe.getProducts()).containsExactly("uv");
  }

  @Test
  void testGetVendorsAndProductsWithMultipleProducts() {

    JavaLikeCpeUpdater updater = new JavaLikeCpeUpdater();

    AbstractUrlUpdater.CpeRegistry cpe = updater.getCpeRegistry();

    assertThat(cpe.getVendors()).containsExactly("oracle");
    assertThat(cpe.getProducts()).containsExactly("jdk", "java_se");
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
    }
   }

   private static final class BrokenCpeUpdater extends DefaultCpeUpdater {

     @Override
     protected void initCpe(CpeRegistry cpe) {

       // intentionally left empty
     }
   }

   private static final class JavaLikeCpeUpdater extends DefaultCpeUpdater {

     @Override
     protected void initCpe(CpeRegistry cpe) {

       cpe.addVendor("oracle")
           .addProduct("jdk")
           .addProduct("java_se");
     }
   }
}

