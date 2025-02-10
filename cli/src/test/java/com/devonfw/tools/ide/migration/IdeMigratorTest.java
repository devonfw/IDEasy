package com.devonfw.tools.ide.migration;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devonfw.tools.ide.context.AbstractIdeContextTest;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.context.IdeTestContext;
import com.devonfw.tools.ide.version.VersionIdentifier;

/**
 * Test of {@link IdeMigrator}.
 */
public class IdeMigratorTest extends AbstractIdeContextTest {

  /** Test that no migration is executed when outside of project. */
  @Test
  public void testDoesNothingWithoutIdeHome() {

    // arrage
    IdeTestContext context = new IdeTestContext();
    IdeMigrator migrator = new IdeMigrator();
    // act
    migrator.run(context);
    // assert
    assertThat(context).logAtDebug().hasMessage("Skipping migration since IDE_HOME is undefined.");
  }

  /** Test that all versions are properly in order. */
  @Test
  public void testVerifyAllMigrations() {

    // arrange
    IdeTestContext context = newContext("migration");
    // ensure no migration is applied
    context.setProjectVersion(VersionIdentifier.of("2999.12.999"));
    IdeMigrator migrator = new IdeMigrator();
    // act
    migrator.run(context);
    // assert
  }

  /** Test that required migrations are properly applied and older migrations are not. */
  @Test
  public void testRunAllDummyMigrations() {

    // arrange
    IdeTestContext context = newContext("migration");
    IdeMigrator migrator = new IdeMigrator(List.of(new Dummy202401001(), new Dummy202501002(), new Dummy202502003()));
    // act
    migrator.run(context);
    // assert
    assertThat(context).logAtInfo().hasEntries("202501002 migration was done", "202502003 migration completed");
    assertThat(context.getIdeHome().resolve(".ide.software.version")).hasContent("2025.02.003");
  }

  private static class Dummy202401001 extends IdeVersionMigration {

    public Dummy202401001() {

      super("2024.01.001-alpha");
    }

    @Override
    public void run(IdeContext context) {
      throw new IllegalStateException("Should not be applied!");
    }
  }

  private static class Dummy202501002 extends IdeVersionMigration {

    public Dummy202501002() {

      super("2025.01.002-beta");
    }

    @Override
    public void run(IdeContext context) {
      context.info("202501002 migration was done");
    }
  }

  private static class Dummy202502003 extends IdeVersionMigration {

    public Dummy202502003() {

      super("2025.02.003");
    }

    @Override
    public void run(IdeContext context) {
      context.info("202502003 migration completed");
    }
  }

}
