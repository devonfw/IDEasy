import org.apache.tools.ant.ExitStatusException
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.jobs.IJobManager
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs
import org.eclipse.ui.IWorkingSet
import org.eclipse.ui.IWorkingSetManager
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.internal.wizards.datatransfer.RecursiveImportListener
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob
import org.eclipse.ui.wizards.datatransfer.ProjectConfigurator

class SysoutProgressMonitor extends org.eclipse.core.runtime.NullProgressMonitor {
  private String myName;

  public void setTaskName(String name) {
    this.myName = name;
  }

  public void beginTask(String name, int totalWork) {
    System.out.println(myName + " :Begin Task: " + name + "(" + totalWork + ")");
  }
}

class SysoutListener implements RecursiveImportListener {
  public void projectCreated(IProject project) {
    System.out.println("Created " + project);
  }

  public void projectConfigured(IProject project, ProjectConfigurator configurator) {
    System.out.println("Configured " + project + " using " + configurator);
  }

  public void errorHappened(IPath location, Exception ex) {
    System.out.println("Error: " + location);
    ex.printStackTrace();
  }
}

class MyWorkbenchAdvisor extends org.eclipse.ui.application.WorkbenchAdvisor {
  private Map antProperties;

  public MyWorkbenchAdvisor(Map properties) {
    this.antProperties = properties;
  }

  public String getInitialWindowPerspectiveId() {
    return null;
  }

  private Set getOrCreateWorkingSets(Collection workingSetNames) {
    IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
    Set workingSets = new HashSet();
    for (String workingSetName : workingSetNames) {
      IWorkingSet workingSet = workingSetManager.getWorkingSet(workingSetName);
      if (workingSet == null) {
        workingSet = workingSetManager.createWorkingSet(workingSetName, new IProject[0]);
        workingSet.setId(IWorkingSetIDs.RESOURCE);
        workingSetManager.addWorkingSet(workingSet);
      }
      workingSets.add(workingSet);
    }
    return workingSets;
  }

  private void doImport(String projectDirectoryName, Collection workingSetNames) {
    System.out.println("Importing " + projectDirectoryName + " for working sets " + workingSetNames);
    File projectDirectory = new File(projectDirectoryName);
    if (!projectDirectory.canRead()) {
      throw new IllegalStateException("Cannot open project directory " + projectDirectoryName);
    }
    SmartImportJob importJob = new SmartImportJob(projectDirectory, getOrCreateWorkingSets(workingSetNames), true, true);
    Map proposals = importJob.getImportProposals(null);
    importJob.setDirectoriesToImport(proposals.keySet());
    importJob.setListener(new SysoutListener());
    importJob.run(null);

    waitForJobs();
  }

  private void waitForJobs() {
    // Class names of jobs to wait for
    List importantJobClassNames = Arrays.asList(
        "org.eclipse.m2e.importer.internal.MavenProjectConfigurator\$UpdateMavenConfigurationJob",
        "org.eclipse.m2e.core.internal.project.registry.ProjectRegistryRefreshJob",
        "org.eclipse.core.internal.events.AutoBuildJob",
    );
    System.out.println("Wating for jobs to finish...");
    IJobManager jobMan = Job.getJobManager();
    boolean importantJobsRunning;
    do {
      importantJobsRunning = false;
      System.out.println("Still running:");
      Job[] jobs = jobMan.find(null);
      for (Job job : jobs) {
        importantJobsRunning |= (importantJobClassNames.contains(job.getClass().getName()));
        System.out.println(job.toString() + ": " + job.getClass().getName());
      }
      Thread.sleep(3000);
    } while (importantJobsRunning);
  }

  public void preStartup() {
    try {
      // Get path from ant properties
      String path = antProperties.get("devonImportPath");
      if (path == null || path.equals("")) {
        throw new IllegalStateException("Parameter repositoryImportPath must be set.");
      }

      // Get workingsets from ant properties
      Collection workingSetNames = Collections.EMPTY_LIST;
      String workingSetParam = antProperties.get("repositoryImportWorkingSet");
      if (workingSetParam != null && !workingSetParam.equals("")) {
        workingSetNames = Arrays.asList(workingSetParam.split(","));
      }

      // Actually start import
      doImport(path, workingSetNames);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public void postStartup() {
    PlatformUI.getWorkbench().close();
  }
}
// Groovy Script startes here...
System.out.println("Preparing eclipse instance for import...");

if (Platform.getInstanceLocation().isLocked()) {
  throw new ExitStatusException("Workspace is locked", PlatformUI.RETURN_UNSTARTABLE);
}

// We are running in the context of antrunner. Import, Workingset etc. need some
// parts of the normal IDE infratructure running. So we init it here.

// Register Adapters, e.g. to allow persistence of workingsets work (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=513188)
org.eclipse.ui.ide.IDE.registerAdapters();

// Create Workbench, this make the eclispe windows appear, but it is necessary for the importer job.
display = PlatformUI.createDisplay();

// 'properties' will be automatically populated by groovy-ant-task, pass it to the importer
int rc = PlatformUI.createAndRunWorkbench(display, new MyWorkbenchAdvisor(properties));
if (rc != PlatformUI.RETURN_OK) {
  throw new ExitStatusException("Import failed", rc);
}
System.out.println("End.");
