package ch.cern.dss.teamcity.server;

import ch.cern.dss.teamcity.common.AbiCheckerConstants;
import org.jetbrains.annotations.NotNull;

public class AbiCheckerBean {
    @NotNull
    public String getProjectNameKey() {
        return AbiCheckerConstants.UI_PROJECT_NAME;
    }

    @NotNull
    public String getBuildTypeKey() {
        return AbiCheckerConstants.UI_BUILD_TYPE;
    }

    @NotNull
    public String getReferenceTagKey() {
        return AbiCheckerConstants.UI_REFERENCE_TAG;
    }

    @NotNull
    public String getAbiCheckerExecutablePathKey() {
        return AbiCheckerConstants.UI_ABI_CHECKER_EXECUTABLE_PATH;
    }

    @NotNull
    public String getCustomArtifactPathKey() {
        return AbiCheckerConstants.UI_ARTIFACT_PATH;
    }
}
