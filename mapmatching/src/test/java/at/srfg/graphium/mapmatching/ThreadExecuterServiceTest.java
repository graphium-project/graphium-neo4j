/**
 * Graphium Neo4j - Map Matching module of Graphium
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
/**
 * (C) 2016 Salzburg Research Forschungsgesellschaft m.b.H.
 *
 * All rights reserved.
 *
 */
package at.srfg.graphium.mapmatching;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

/**
 * @author mwimmer
 */
public class ThreadExecuterServiceTest {
	
	@Test
	public void testThreadExecuterService() {
		
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		Future<Integer> task = executorService.submit(new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				Thread.sleep(20000);
				return 5;
			}
		});

	    try {
	        Integer result = task.get(5, TimeUnit.SECONDS);
	        System.out.println("Result = " + result);
	    } catch (InterruptedException | ExecutionException | TimeoutException e) {
	    	System.out.println("No result");
	    }
    
	}

}
