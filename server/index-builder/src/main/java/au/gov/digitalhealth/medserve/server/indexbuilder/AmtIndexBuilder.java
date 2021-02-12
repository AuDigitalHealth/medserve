package au.gov.digitalhealth.medserve.server.indexbuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.dionmcm.ncts.syndication.client.SyndicationClient;

import au.gov.digitalhealth.medserve.transform.amt.AmtMedicationResourceGenerator;

/**
 * Creates an index of Medication resources for the FHIR server
 */
@Mojo(name = "amt-index-builder", defaultPhase = LifecyclePhase.INSTALL)
public class AmtIndexBuilder
        extends AbstractMojo {
    /**
     * Location of the index output.
     */
    @Parameter(defaultValue = "${project.build.directory}/index", property = "indexLocation")
    private File indexLocation;

    /**
     * Syndication cache directory
     */
    @Parameter(defaultValue = "/tmp", property = "syndCacheDirectory")
    private File syndCacheDirectory;

    /**
     * Syndication cache directory
     */
    @Parameter(property = "clientId", required = true)
    private String clientId;

    /**
     * Syndication cache directory
     */
    @Parameter(property = "clientSecret", required = true)
    private String clientSecret;

    Calendar cal = Calendar.getInstance();

    @Override
    public void execute() throws MojoExecutionException {
        File amtSnapshot = getLatestAmtSnapshot();
        File pbsExtract = getLatestPbsExtract();

        try {
            AmtMedicationResourceGenerator generator =
                    new AmtMedicationResourceGenerator(amtSnapshot.toPath(), pbsExtract.toPath());
            generator.process(new IndexBuildingResourceProcessor(indexLocation));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed building index", e);
        }

    }

    private File getLatestAmtSnapshot() throws MojoExecutionException {
        SyndicationClient syndClient;
        File amtSnapshot;
        try {
            syndClient = new SyndicationClient(syndCacheDirectory, clientId, clientSecret);
            // response will contain one category with one DownloadResult in a List, if the download didn't fail
            amtSnapshot = syndClient.downloadLatest("SCT_RF2_SNAPSHOT").getFile();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to get latest AMT file", e);
        }
        return amtSnapshot;
    }

    private File getLatestPbsExtract() throws MojoExecutionException {
        try {
            File pbsExtract = new File(syndCacheDirectory, getLatestPbsFileName());
            URL url = new URL("https://www.pbs.gov.au/downloads/" + getYear() + "/" + getMonth() + "/" + getLatestPbsFileName());
            if (!pbsExtract.exists()) {
                getLog().info("Downloading latest PBS file from " + url + " to " + pbsExtract.getAbsolutePath());
                FileUtils.copyURLToFile(url, pbsExtract);
            } else {
                getLog().info("Latest PBS file already downloaded at " + pbsExtract.getAbsolutePath());
            }
            return pbsExtract;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed getting latest PBS file", e);
        }

    }

    private String getLatestPbsFileName() {
        return getYear() + "-" + getMonth() + "-01-v3extracts.zip";
    }

    private String getMonth() {
        return String.format("%02d", cal.get(Calendar.MONTH) + 1);
    }

    private String getYear() {
        return String.valueOf(cal.get(Calendar.YEAR));
    }
}
