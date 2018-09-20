/**
 * Graphium Neo4j - Module of Graphserver for Neo4j extension
 * Copyright © 2017 Salzburg Research Forschungsgesellschaft (graphium@salzburgresearch.at)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package at.srfg.graphium.neo4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Salzburg Research ForschungsgesmbH (c) 2018
 *
 * Project: graphium
 * Created by mwimmer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:application-context-graphium-neo4j_testsuite.xml",
		"classpath:application-context-graphium-neo4j_test.xml"})
//@Transactional
public class TestSuite {
	
	private static Logger log = LoggerFactory.getLogger(TestSuite.class);
	
	@Resource(name="testclasses")
    List<ITestGraphiumNeo4j> testList; //all classes which implements ITestGraphiumNeo4j
//
	@Value("${graphium.neo4j.dbDirectory}")
	String dbDirectory = "C:\\development\\repository\\git\\graphium-neo4j\\neo4j-core\\db\\testdata\\importertest.db";

	@PostConstruct
//	@Before
	public void setup() {
//	public TestSuite() {
		// clean db directory if it exists
//        cleanDbDir();
	}
	
//    @Override
//	protected void finalize() throws Throwable {
//    	cleanDbDir();
//		super.finalize();
//	}

	@Test
    //@Transactional(readOnly = false)
    public void run(){
//        // clean db directory if it exists
//        cleanDbDir();

        for(ITestGraphiumNeo4j testclass : testList){
            testclass.run();
        }
        
        // clean db directory
//        cleanDbDir();
    }

//    @PreDestroy
//    @After
	public void cleanDbDir() {
//    	File dir = new File(dbDirectory);
//    	boolean deleted = FileSystemUtils.deleteRecursively(dir);
//    	log.info("directory " + dbDirectory + (deleted ? " deleted" : " not deleted"));
    	
    	try {
    		Path path = Paths.get(dbDirectory);
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    	
	}

}
