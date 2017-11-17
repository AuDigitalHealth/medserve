package online.medserve.server.indexbuilder;

import java.io.File;
import java.io.IOException;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import online.medserve.transform.amt.AmtMedicationResourceGenerator;

/**
 * Creates an index of Medication resources for the FHIR server
 */
@Mojo(name = "indexbuilder", defaultPhase = LifecyclePhase.INSTALL)
public class IndexBuilder
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/index", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/amt-snapshot.zip", property = "amtSnapshot", required = true)
    private File amtSnapshot;

    @Parameter(defaultValue = "${project.build.directory}/pbs-extract.zip", property = "pbsExtract", required = true)
    private File pbsExtract;

    @Override
    public void execute()
        throws MojoExecutionException
    {
        try {
            AmtMedicationResourceGenerator generator =
                    new AmtMedicationResourceGenerator(amtSnapshot.toPath(), pbsExtract.toPath());
            generator.process(new IndexBuildingResourceProcessor(outputDirectory));
        } catch (IOException e) {
            throw new MojoExecutionException("", e);
        }

    }
}
