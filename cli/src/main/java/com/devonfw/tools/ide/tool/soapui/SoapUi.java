package com.devonfw.tools.ide.tool.soapui;

import java.util.Set;

import com.devonfw.tools.ide.common.Tag;
import com.devonfw.tools.ide.context.IdeContext;
import com.devonfw.tools.ide.process.ProcessMode;
import com.devonfw.tools.ide.tool.LocalToolCommandlet;

/**
 * {@link LocalToolCommandlet} for <a href="https://www.soapui.org/">SoapUI</a>, an open source API testing tool for SOAP and REST services.
 */
public class SoapUi extends LocalToolCommandlet {

    private static final String SOAPUI = "soapui";

    private static final String SOAPUI_BAT = SOAPUI + ".bat";

    private static final String SOAPUI_BASH_SCRIPT = SOAPUI + ".sh";

    /**
     * The constructor.
     *
     * @param ideContext {@link IdeContext}.
     */
    public SoapUi(IdeContext ideContext) {

        super(ideContext, SOAPUI, Set.of(Tag.TEST, Tag.REST));
    }

    @Override
    protected String getBinaryName() {

        if (this.context.getSystemInfo().isWindows()) {
            return SOAPUI_BAT;
        } else {
            return SOAPUI_BASH_SCRIPT;
        }
    }

    @Override
    protected void doRun() {

        runTool(ProcessMode.BACKGROUND_SILENT, null, this.arguments.asList());
    }

}
