/**
 * Copyright (c) 2012-2013 by European Organization for Nuclear Research (CERN)
 * Author: Justin Salmon <jsalmon@cern.ch>
 *
 * XRootD is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRootD is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with XRootD.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.cern.dss.teamcity.agent;

import ch.cern.dss.teamcity.common.IOUtil;
import ch.cern.dss.teamcity.agent.util.SimpleLogger;
import ch.cern.dss.teamcity.common.AbiCheckerConstants;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 */
public class MockModeCommandLine implements ProgramCommandLine {

    private final SimpleLogger logger;
    private final AbiCheckerContext context;
    private final MockEnvironmentBuilder mockEnvironmentBuilder;

    /**
     *
     * @param context
     * @param logger
     * @throws RunBuildException
     */
    public MockModeCommandLine(AbiCheckerContext context, SimpleLogger logger) throws RunBuildException {
        this.context = context;
        this.logger = logger;

        logger.message("Setting up mock context");

        File mockMetaDirectory = new File(context.getNewArtifactsDirectory(), AbiCheckerConstants.MOCK_META_DIRECTORY);
        if (!mockMetaDirectory.exists() || !mockMetaDirectory.isDirectory()) {
            throw new RunBuildException("Cannot setup mock context: directory not found: "
                    + mockMetaDirectory);
        }

        mockEnvironmentBuilder = new MockEnvironmentBuilder(mockMetaDirectory, logger);
        mockEnvironmentBuilder.setup();
    }

    /**
     *
     * @return
     * @throws RunBuildException
     */
    @NotNull
    @Override
    public String getExecutablePath() throws RunBuildException {
        return "/bin/bash";
    }

    /**
     *
     * @return
     * @throws RunBuildException
     */
    @NotNull
    @Override
    public String getWorkingDirectory() throws RunBuildException {
        return context.getWorkingDirectory().getAbsolutePath();
    }

    /**
     *
     * @return
     * @throws RunBuildException
     */
    @NotNull
    @Override
    public List<String> getArguments() throws RunBuildException {
        List<String> arguments = new Vector<String>();
        StringBuilder command = new StringBuilder();

        try {
            FileUtils.copyFile(new File(AbiCheckerConstants.MOCK_CONFIG_DIRECTORY, "site-defaults.cfg"),
                    new File(context.getWorkingDirectory().getAbsolutePath(), "site-defaults.cfg"));
            FileUtils.copyFile(new File(AbiCheckerConstants.MOCK_CONFIG_DIRECTORY, "logging.ini"),
                    new File(context.getWorkingDirectory().getAbsolutePath(), "logging.ini"));
        } catch (IOException e) {
            throw new RunBuildException("Error reading mock site-defaults.cfg", e);
        }

        for (String chroot : mockEnvironmentBuilder.getChroots()) {
            String mockConfig;

            try {
                mockConfig = IOUtil.readFile(
                        new File(AbiCheckerConstants.MOCK_CONFIG_DIRECTORY, chroot + ".cfg").getAbsolutePath());
            } catch (IOException e) {
                throw new RunBuildException("Error reading mock config: " + chroot, e);
            }

            mockConfig += "\nconfig_opts['plugin_conf']['bind_mount_enable'] = True\n";
            mockConfig += "config_opts['plugin_conf']['root_cache_opts']['age_check'] = False\n";
            mockConfig += "config_opts['plugin_conf']['bind_mount_opts']['create_dirs'] = True\n";
            mockConfig += "config_opts['plugin_conf']['bind_mount_opts']['dirs'].append(('"
                    + context.getBuildTempDirectory() + "', '" + context.getBuildTempDirectory() + "' ))\n";

            try {
                IOUtil.writeFile(new File(context.getWorkingDirectory().getAbsolutePath(), chroot + ".cfg")
                        .getAbsolutePath(), mockConfig);
            } catch (IOException e) {
                throw new RunBuildException("Error writing mock config: " + chroot, e);
            }

            command.append(AbiCheckerConstants.MOCK_EXECUTABLE)
                    .append(" --configdir=").append(context.getWorkingDirectory().getAbsolutePath())
                    .append(" -r ").append(chroot)
                    .append(" --install abi-compliance-checker ctags\n");

            command.append(AbiCheckerConstants.MOCK_EXECUTABLE)
                    .append(" --configdir=").append(context.getWorkingDirectory().getAbsolutePath())
                    .append(" -r ").append(chroot)
                    .append(" --chroot '")
                    .append(context.getAbiCheckerExecutablePath())
                    .append(" -show-retval")
                    .append(" -lib ").append(StringUtil.join(context.getLibNames(), ", "))
                    .append(" -component ").append(context.getLibNames().size() > 1 ? "libraries" : "library")
                    .append(" -old ").append(context.getReferenceXmlFilename())
                    .append(" -new ").append(context.getNewXmlFilename())
                    .append(" -binary -bin-report-path ").append(context.getNewArtifactsDirectory())
                    .append("/").append(chroot).append(AbiCheckerConstants.REPORT_DIRECTORY)
                    .append(AbiCheckerConstants.ABI_REPORT)
                    .append(" -source -src-report-path ").append(context.getNewArtifactsDirectory())
                    .append("/").append(chroot).append(AbiCheckerConstants.REPORT_DIRECTORY)
                    .append(AbiCheckerConstants.SRC_REPORT)
                    .append(" -log-path ").append(context.getNewArtifactsDirectory())
                    .append("/").append(chroot).append(AbiCheckerConstants.REPORT_DIRECTORY)
                    .append("log.txt")
                    .append(" -report-format html")
                    .append(" -logging-mode w'\n");
        }

        File mockScriptFile = new File(context.getWorkingDirectory(), "mock-install.sh");
        try {
            IOUtil.writeFile(mockScriptFile.getAbsolutePath(), command.toString());
        } catch (IOException e) {
            throw new RunBuildException("Error writing mock script", e);
        }

        arguments.add(mockScriptFile.getAbsolutePath());
        logger.message("Arguments: " + StringUtil.join(arguments, " "));
        return arguments;
    }

    /**
     *
     * @return
     * @throws RunBuildException
     */
    @NotNull
    @Override
    public Map<String, String> getEnvironment() throws RunBuildException {
        return context.getEnvironment();
    }
}
