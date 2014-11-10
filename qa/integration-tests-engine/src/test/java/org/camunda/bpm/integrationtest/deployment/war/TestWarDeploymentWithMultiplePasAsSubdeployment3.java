/**
 * Copyright (C) 2011, 2012 camunda services GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.integrationtest.deployment.war;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;

import org.camunda.bpm.engine.repository.DeploymentQuery;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.camunda.bpm.integrationtest.util.DeploymentHelper;
import org.camunda.bpm.integrationtest.util.TestContainer;
import org.camunda.bpm.integrationtest.util.TestHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * 
 * <pre>
 *   |-- My-Application.war
 *       |-- WEB-INF
 *           |-- classes
 *                   |-- process0.bpmn    
 *                   |-- directory/process1.bpmn
 *                   |-- alternateDirectory/process2.bpmn 
 *           |-- lib/
 *               |-- pa2.jar 
 *                   |-- META-INF/processes.xml uses classpath:directory/
 * </pre> 
 * 
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class TestWarDeploymentWithMultiplePasAsSubdeployment3 extends AbstractFoxPlatformIntegrationTest {
  
  public final static String PROCESSES_XML =       
    "<process-application xmlns=\"http://www.camunda.org/schema/1.0/ProcessApplication\">" +
          
      "<process-archive name=\"PA_NAME\">" +
        "<properties>" +        
          "<property name=\"isDeleteUponUndeploy\">true</property>" +
          "<property name=\"resourceRootPath\">classpath:directory/</property>" +
        "</properties>" +  
      "</process-archive>" +
  
    "</process-application>";  
   
  
  @Deployment
  public static Archive<?> processArchive() {

    Asset pa2ProcessesXml = TestHelper.getStringAsAssetWithReplacements(
            PROCESSES_XML, 
            new String[][]{new String[]{"PA_NAME","PA2"}});
    
    
    Asset[] processAssets = TestHelper.generateProcessAssets(9);
        
    JavaArchive pa2 = ShrinkWrap.create(JavaArchive.class, "pa2.jar")
            .addAsResource(pa2ProcessesXml, "META-INF/processes.xml");
       
    WebArchive deployment = ShrinkWrap.create(WebArchive.class, "test.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsLibraries(DeploymentHelper.getEngineCdi())
            
            .addAsLibraries(pa2)
            
            .addAsResource(processAssets[0], "process0.bpmn")
            .addAsResource(processAssets[1], "directory/process1.bpmn")
            .addAsResource(processAssets[2], "alternateDirectory/process2.bpmn")
            
            .addClass(AbstractFoxPlatformIntegrationTest.class);
    
    TestContainer.addContainerSpecificResources(deployment);
    
    return processArchiveDeployment(deployment);
    
  }
  
  @Test
  public void testDeployProcessArchive() {
    
    assertProcessNotDeployed("process-0");
    assertProcessDeployed   ("process-1", "PA2");
    assertProcessNotDeployed("process-2");
      
  }

 protected void assertProcessNotDeployed(String processKey) {
    
    long count = repositoryService
        .createProcessDefinitionQuery()
        .latestVersion()
        .processDefinitionKey(processKey)
        .count();
    
    Assert.assertEquals("Process with key "+processKey+ " should not be deployed", 0, count);
  }

  protected void assertProcessDeployed(String processKey, String expectedDeploymentName) {
    
    ProcessDefinition processDefinition = repositoryService
        .createProcessDefinitionQuery()
        .latestVersion()
        .processDefinitionKey(processKey)
        .singleResult();    
    
    DeploymentQuery deploymentQuery = repositoryService
        .createDeploymentQuery()
        .deploymentId(processDefinition.getDeploymentId());
    
    Assert.assertEquals(expectedDeploymentName, deploymentQuery.singleResult().getName());
    
  }
  
}
