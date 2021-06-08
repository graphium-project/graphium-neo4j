/**
 * Graphium Neo4j - Module of Graphserver for Map Matching using Neo4j
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
package at.srfg.graphium.mapmatching.neo4j.matcher.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionTerminatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;

import at.srfg.graphium.mapmatching.matcher.IMapMatcher;
import at.srfg.graphium.mapmatching.matcher.IMapMatcherTask;
import at.srfg.graphium.mapmatching.matcher.impl.SegmentMatcher;
import at.srfg.graphium.mapmatching.model.Direction;
import at.srfg.graphium.mapmatching.model.IMatchedBranch;
import at.srfg.graphium.mapmatching.model.IMatchedWaySegment;
import at.srfg.graphium.mapmatching.model.ITrack;
import at.srfg.graphium.mapmatching.model.IWeightingStrategy;
import at.srfg.graphium.mapmatching.model.utils.TrackUtils;
import at.srfg.graphium.mapmatching.properties.IMapMatchingProperties;
import at.srfg.graphium.mapmatching.properties.impl.MapMatchingProperties;
import at.srfg.graphium.mapmatching.statistics.MapMatcherGlobalStatistics;
import at.srfg.graphium.mapmatching.statistics.MapMatcherStatistics;
import at.srfg.graphium.mapmatching.util.MapMatchingUtil;
import at.srfg.graphium.mapmatching.weighting.IWeightingStrategyFactory;
import at.srfg.graphium.mapmatching.weighting.impl.RouteDistanceWeightingStrategyFactory;
import at.srfg.graphium.model.IWayGraphVersionMetadata;
import at.srfg.graphium.neo4j.persistence.INeo4jWayGraphReadDao;
import at.srfg.graphium.neo4j.persistence.Neo4jUtil;
import at.srfg.graphium.routing.exception.RoutingParameterException;

public class MapMatchingTask implements IMapMatcherTask {

	private static Logger log = LoggerFactory.getLogger(MapMatchingTask.class);

	private IMapMatchingProperties properties;
	private Neo4jMapMatcher mapMatcher;			// TODO: => IMapMatcher
	private IWayGraphVersionMetadata graphMetadata;
	private Neo4jUtil neo4jUtil;

	// cancel request flag in case of processes taking too much time
//	private volatile boolean cancelRequested = false;
	private MutableBoolean cancellationObject;
	
	// collect some statistical data
	MapMatcherStatistics statistics = new MapMatcherStatistics();
	private MapMatcherGlobalStatistics globalStatistics = new MapMatcherGlobalStatistics();
	
	private ITrack track;
	
	private TrackSanitizer trackSanitizer;
	private InitialMatcher initialMatcher;
	private SegmentMatcher segmentMatcher;
	private PathExpanderMatcher pathExpanderMatcher;
	private RoutingMatcher routingMatcher;
	private AlternativePathMatcher alternativePathMatcher;
	private MatchesFilter matchesFilter;
	private IWeightingStrategyFactory weightingStrategyFactory;
	private IWeightingStrategy weightingStrategy;
	private static Logger csvLogger = null;

	public MapMatchingTask(Neo4jMapMatcher mapMatcher, MapMatchingProperties properties, IWayGraphVersionMetadata graphMetadata, Neo4jUtil neo4jUtil, 
			ITrack origTrack, String csvLoggerName, MapMatcherGlobalStatistics globalStatistics) throws RoutingParameterException {
		this(mapMatcher, properties, graphMetadata, neo4jUtil, origTrack, new RouteDistanceWeightingStrategyFactory(), 
				csvLoggerName, globalStatistics);
	}

	public MapMatchingTask(Neo4jMapMatcher mapMatcher, MapMatchingProperties properties, IWayGraphVersionMetadata graphMetadata, 
			Neo4jUtil neo4jUtil, ITrack origTrack, IWeightingStrategyFactory weightingStrategyFactory, String csvLoggerName,
			MapMatcherGlobalStatistics globalStatistics) throws RoutingParameterException {
		cancellationObject = new MutableBoolean(false);
		
		this.mapMatcher = mapMatcher;
		this.graphMetadata = graphMetadata;
		this.track = origTrack;
		this.properties = properties.clone();
		this.neo4jUtil = neo4jUtil;
		
		this.trackSanitizer = new TrackSanitizer();
		this.initialMatcher = new InitialMatcher(this, this.properties, neo4jUtil);
		this.segmentMatcher = new SegmentMatcher(this.properties);
		this.pathExpanderMatcher = new PathExpanderMatcher(this, this.properties, neo4jUtil);
		this.routingMatcher = new RoutingMatcher(this, mapMatcher.getRoutingService(), this.properties, this.trackSanitizer, cancellationObject);
		this.alternativePathMatcher = new AlternativePathMatcher(this);
		this.matchesFilter = new MatchesFilter(this, alternativePathMatcher, this.properties);
		this.weightingStrategyFactory = weightingStrategyFactory;
		this.globalStatistics = globalStatistics;

		if (csvLoggerName != null && csvLoggerName.length() > 0) {
			csvLogger = LoggerFactory.getLogger(csvLoggerName);
		}
		
		log.info("matching properties: " + properties.toString());

	}
	
	@Override
	@Transactional(readOnly=true)
	public List<IMatchedBranch> matchTrack() {
		return matchTrack(null, null);
	}
	
	@Override
	@Transactional(readOnly=true)
	public List<IMatchedBranch> matchTrack(Long startSegmentId) {
		return matchTrack(startSegmentId, null);
	}
	
	@Override
	@Transactional(readOnly=true)
	public List<IMatchedBranch> matchTrack(List<IMatchedBranch> branches) {
		return matchTrack(null, branches);
	}
	
	private List<IMatchedBranch> matchTrack(Long startSegmentId, List<IMatchedBranch> branches) {
		log.info("matching track " + track.getId());
		log.info("graph name: " + graphMetadata.getGraphName() + " in version: " + graphMetadata.getVersion());
		
		track.calculateMetaData(true);
		
		if (track.getTrackPoints().size() < properties.getMinNrOfPoints()
				|| track.getMetadata().getLength() < properties.getMinLength()) {
			log.error("matching failed, track " + track.getId() + " is too short");
			return Collections.emptyList();
		}

		statistics.reset();
		Date startTimestamp = new Date();
		statistics.setValue(MapMatcherStatistics.START_TIMESTAMP, startTimestamp);
		statistics.setValue(MapMatcherStatistics.NUMBER_OF_TRACK_POINTS, track.getTrackPoints().size());
		statistics.setValue(MapMatcherStatistics.TRACK_ID, track.getId());
		statistics.setValue(MapMatcherStatistics.TRACK_LENGTH, (int)track.getMetadata().getLength());
		statistics.setValue(MapMatcherStatistics.AVG_SAMPLING_RATE, TrackUtils.getMeanSamplingRate(track));
		
		List<IMatchedBranch> result = null;
		result = doMatchTrack(graphMetadata.getGraphName(), this.track, startSegmentId, branches);
		
		Date endTimestamp = new Date();
		statistics.setValue(MapMatcherStatistics.END_TIMESTAMP, endTimestamp);
		statistics.setValue(MapMatcherStatistics.EXECUTION_TIME, endTimestamp.getTime() - startTimestamp.getTime());
		if (result != null && !result.isEmpty()) {
			List<String> matchedFactors = new ArrayList<String>();
			for (IMatchedBranch branch : result) {
				matchedFactors.add(Double.toString(branch.getMatchedFactor()));
			}
			statistics.setValue(MapMatcherStatistics.MATCHED_FACTORS, matchedFactors);
		}
		
//		globalStatistics.incrementSuccess(this.track.getMetadata().getNumberOfPoints(), track.getLineString().getLength());
		
		// log statistics	
		log.info("Thread " + Thread.currentThread().getName() + " \n" + statistics.toString());
		logCsv();
		
		return result;
	}
	
	private List<IMatchedBranch> doMatchTrack(String graphName, ITrack origTrack, Long startSegmentId, List<IMatchedBranch> oldPaths) {
		Transaction tx = getGraphDao().getGraphDatabaseProvider().getGraphDatabase().beginTx();
		try {
			boolean iterativeMode = (oldPaths != null && !oldPaths.isEmpty());
			int maxPrevIndex = 0;
			int countLoopsWithoutPathExtension = 0;
			
			if (!prepareMatching(graphName, origTrack)) {
				return Collections.emptyList();
			}
			
			////
			long totalTimeFindingPaths = 0;
			long totalTimeFilteringPaths = 0;
			////

			int pointIndex = 0;
			List<IMatchedBranch> detectedPaths = new ArrayList<IMatchedBranch>();
			List<IMatchedWaySegment> certainPath = new ArrayList<>();
		
			// search for possible paths
			// if one iteration returns no paths => try to find start segments with next track point...
			do {
				List<IMatchedBranch> paths;
				
				if (startSegmentId != null && pointIndex == 0) {
					// find path(s) starting with segment matched in previous iteration
					paths = initialMatcher.getStartPathForStartSegment(0, track, startSegmentId);
					if (paths == null || paths.isEmpty()) {
						log.warn("No paths for given start segment " + startSegmentId + " found for track " + origTrack.getId());
						startSegmentId = null;
					} else {
						pointIndex = getPreviousEndPointIndex(paths);
					}
					// here Online Map Matching is proposed => ignore short sections
					properties.setMinSegmentsPerSection(1);
	
				} else if (iterativeMode && pointIndex == 0) {
					// start with paths from the previous iteration
					paths = prepareBranchesFromPreviousIteration(oldPaths, track);
					pointIndex = getPreviousEndPointIndex(oldPaths); // + properties.getNrOfPointsToSkip();
					// here Online Map Matching is proposed => ignore short sections
					properties.setMinSegmentsPerSection(1);

				} else {
					paths = initialMatcher.getStartPaths(pointIndex, track, properties.getNrOfPointsForInitialMatch());
					
					if (log.isDebugEnabled()) {
						if (paths != null) {
							int i = 1;
							for (IMatchedBranch path : paths) {
								for (IMatchedWaySegment seg : path.getMatchedWaySegments()) {
									log.debug("Path " + i++ + " Segment: " + seg.getId() + " (StartPointIndex=" + seg.getStartPointIndex() + ", " +
											" (EndPointIndex=" + seg.getEndPointIndex() + ")");
								}
							}
						}
					}				
					
					int pointsToSkip = trackSanitizer.determineNextValidPointForSegmentSearch(track, pointIndex, properties.getEnvelopeSideLength());;
					pointIndex += pointsToSkip;
	
				}
						
				List<IMatchedBranch> possiblePathsForStartSegments = new ArrayList<IMatchedBranch>();
				
				// incremental search for possible paths
				while (paths != null && !paths.isEmpty()) {
					checkCancelStatus();
					
					////
//					long timeFindingPaths = System.nanoTime();
					////
					
					paths = pathExpanderMatcher.findPaths(
							paths, 
							track);
					
					////
//					totalTimeFindingPaths += System.nanoTime() - timeFindingPaths;
					////
	
					checkCancelStatus();

					if (log.isDebugEnabled()) {
						log.debug("Paths before filtering");
						debugPrintPaths(paths, possiblePathsForStartSegments);
					}
					
					////
//					long timeFilteringPaths = 0;
//					if (log.isDebugEnabled()) {
//						timeFilteringPaths = System.nanoTime();
//					}
					
					paths = matchesFilter.filterMatches(
							paths, 
							track, 
							possiblePathsForStartSegments,
							certainPath.size() > 0,
							properties);
					
					////
//					if (log.isDebugEnabled()) {
//						totalTimeFilteringPaths += System.nanoTime() - timeFilteringPaths;
//					}
					////

					if (log.isDebugEnabled()) {
						log.debug("Paths after filtering");
						debugPrintPaths(paths, possiblePathsForStartSegments);
					}
	
					// extract safe parts of paths and merge them with already cached safe path;
					// safe path are all those segments starting at the beginning of the track which are identical to all paths	
					if (possiblePathsForStartSegments.isEmpty()) {
						extractCertainPath(certainPath, paths);
					}
					
					// check if paths have been extended; if 5 times not => break that loop 
					int maxEndPointIndex = getMaxEndPointIndex(paths);
					if (maxEndPointIndex > maxPrevIndex) {
						countLoopsWithoutPathExtension = 0;
						maxPrevIndex = maxEndPointIndex;
					} else {
						countLoopsWithoutPathExtension++;
						if (countLoopsWithoutPathExtension > properties.getMaxCountLoopsWithoutPathExtension()) {
							// TODO: do we need to filter?
							if (!paths.isEmpty()) {
								possiblePathsForStartSegments.addAll(paths);
							}
							break;
						}
					}
				
				}
				
				if (possiblePathsForStartSegments != null && !possiblePathsForStartSegments.isEmpty()) {
					// merge each path representing unsafe parts with safe path => building finished paths
					detectedPaths.addAll(buildFinishedPaths(possiblePathsForStartSegments, certainPath));
				}
				
			} while ((detectedPaths == null || detectedPaths.isEmpty()) && 
					 (pointIndex + properties.getNrOfPointsToSkip() + 1) < track.getTrackPoints().size());
		
			tx.success();
			
			////
			if (log.isDebugEnabled()) {
				log.debug("Finding paths took " + (totalTimeFindingPaths/1000000) + "ms");
				log.debug("Filtering paths took " + (totalTimeFilteringPaths/1000000) + "ms");
//				matchesFilter.logTimes();
			}
			////
			
			return getResult(detectedPaths, startSegmentId);
		
		} catch (CancellationException | TransactionTerminatedException e) {
			tx.failure();
			handleCancellationException();
			throw new CancellationException(e.getMessage());

		} catch (Exception e) {
			log.error("Error while map matching", e);
			tx.failure();
			throw new RuntimeException(e.getMessage());
		} finally {
			tx.close();
		}
	}

	private Collection<? extends IMatchedBranch> buildFinishedPaths(List<IMatchedBranch> possiblePathsForStartSegments,
			List<IMatchedWaySegment> certainPath) {
		if (!certainPath.isEmpty()) {
			List<IMatchedWaySegment> certainPathReverse = new ArrayList<>(certainPath);
			Collections.reverse(certainPathReverse);
			for (IMatchedBranch path : possiblePathsForStartSegments) {
				for (IMatchedWaySegment segment : certainPathReverse) {
					path.getMatchedWaySegments().add(0, segment);
				}
				path.recalculate();
			}
		}
		return possiblePathsForStartSegments;
	}

	private void extractCertainPath(List<IMatchedWaySegment> certainPath, List<IMatchedBranch> paths) {
		if (paths != null && !paths.isEmpty()) {
			IMatchedWaySegment lastCertainSegment = matchesFilter.identifyCertainSegment(paths);
			if (lastCertainSegment != null) {
				List<IMatchedWaySegment> newCertainSegments = new ArrayList<>();
				boolean fillNewCertainSegments = true;
	
				for (IMatchedBranch path : paths) {
					
					int i = 0;
					List<Integer> segmentIndicesToRemove = new ArrayList<>();
					Iterator<IMatchedWaySegment> itSeg = path.getMatchedWaySegments().iterator();
					while (itSeg.hasNext() && i >= 0) {
						IMatchedWaySegment seg = itSeg.next();
						seg.setCertain(true);
						if (!seg.equals(lastCertainSegment)) {
							segmentIndicesToRemove.add(i++);
							if (fillNewCertainSegments) {
								newCertainSegments.add(seg);
							}
						} else {
							i = -1;
						}
					}
					fillNewCertainSegments = false;
					path.removeMatchedWaySegments(segmentIndicesToRemove);
					path.setCertainPathEndSegment(lastCertainSegment);

					
					// replace first segment with lastCertainSegment to guarantee that all paths start from the same base (segment)
					if (!path.getMatchedWaySegments().isEmpty()) {
						path.getMatchedWaySegments().remove(0);
					}
					
					IMatchedWaySegment lastCertainSegmentClone = null;
					try {
						lastCertainSegmentClone = (IMatchedWaySegment) lastCertainSegment.clone();
						path.getMatchedWaySegments().add(0, lastCertainSegmentClone);
					} catch (CloneNotSupportedException e) {
						log.error("Could not clone segment");
						return;
					}

					
					
					// update indices
					if (path.getMatchedWaySegments().size() > 1) {
						IMatchedWaySegment firstUncertainSegment = path.getMatchedWaySegments().get(1);
						if (firstUncertainSegment.getEndPointIndex() < lastCertainSegmentClone.getEndPointIndex()) {
							if (lastCertainSegmentClone.getStartPointIndex() < firstUncertainSegment.getStartPointIndex()) {
								//updateIndices(firstUncertainSegment, lastCertainSegmentClone, properties.getMaxMatchingRadiusMeter());
								getSegmentMatcher().recalculateSegmentsIndexes(track, Math.min(lastCertainSegmentClone.getStartPointIndex(), firstUncertainSegment.getStartPointIndex()), 
										Math.max(lastCertainSegmentClone.getEndPointIndex(), firstUncertainSegment.getEndPointIndex()), path.getMatchedWaySegments());
							} else {
								lastCertainSegmentClone.setEndPointIndex(firstUncertainSegment.getStartPointIndex());
								lastCertainSegmentClone.calculateDistances(track);
							}
						} else {
							if (!firstUncertainSegment.isAfterSkippedPart() &&
								 lastCertainSegment.getEndPointIndex() >= firstUncertainSegment.getStartPointIndex()) {
								firstUncertainSegment.setStartPointIndex(lastCertainSegment.getEndPointIndex());
								firstUncertainSegment.calculateDistances(track);
							}
						}						
					}
					
				}
				
				certainPath.addAll(newCertainSegments);
				
				if (log.isDebugEnabled()) {
					List<Long> segmentIds = new ArrayList<>(certainPath.size());
					for (IMatchedWaySegment segment : certainPath) {
						segmentIds.add(segment.getId());
					}
					log.debug("Certain Path = " + StringUtils.join(segmentIds, ", "));
					
					
					///////////////////////////////////////////////////
					List<String> segmentInfoList = new ArrayList<>(certainPath.size());
					for (IMatchedWaySegment segment : certainPath) {
						segmentInfoList.add(segment.getId() + "(" + segment.getStartPointIndex() + "-" + segment.getEndPointIndex() + ")");
					}
					log.debug("Certain Path = " + StringUtils.join(segmentInfoList, ", "));
					///////////////////////////////////////////////////

				}
				
			}
		}
	}

	/**
	 * @param paths
	 */
	private void debugPrintPaths(List<IMatchedBranch> paths, List<IMatchedBranch> possibleFinishedPaths) {
		if (log.isDebugEnabled()) {
			log.debug("active paths: >>>>>>>>>>>>>>>>>>>>");
			printPaths(paths);
			log.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

			log.debug("finished paths: >>>>>>>>>>>>>>>>>>");
			printPaths(possibleFinishedPaths);
			log.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		}
	}

	/**
	 * @param paths
	 */
	private void debugPrintPathsWithIndexes(List<IMatchedBranch> paths, List<IMatchedBranch> possibleFinishedPaths) {
		if (log.isDebugEnabled()) {
			log.debug("active paths with indexes: >>>>>>>>>>>>>>>>>>>>");
			int i=1;
			for (IMatchedBranch path : paths) {
				List<String> segmentIds = new ArrayList<>(path.getMatchedWaySegments().size());
				for (IMatchedWaySegment segment : path.getMatchedWaySegments()) {
					segmentIds.add(segment.getId() + " (" + segment.getStartPointIndex() + "-" + segment.getEndPointIndex() + ")");
				}
				log.debug("Segments = " + StringUtils.join(segmentIds, ", "));
			}
			log.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		}
	}

	private void printPaths(List<IMatchedBranch> paths) {
		int i=1;
		for (IMatchedBranch path : paths) {
			List<Long> segmentIds = new ArrayList<>(path.getMatchedWaySegments().size());
			for (IMatchedWaySegment segment : path.getMatchedWaySegments()) {
				segmentIds.add(segment.getId());
			}
			log.debug("Path " + i + ": MatchedFactor = " + path.getMatchedFactor() +
					", step = " + path.getStep() +
					", empty segments = " + path.getNrOfEmptySegments() +
					", matched track points = " + path.getMatchedPoints() +
					", total track points = " + (path.getNrOfTotalTrackPoints()));
			log.debug("Path " + i + ": - Segments = " + StringUtils.join(segmentIds, ", "));
			
			///////////////////////////////////////////////////
			for (IMatchedWaySegment seg : path.getMatchedWaySegments()) {
				if (seg.getStartPointIndex() > seg.getEndPointIndex()) {
					log.debug("////////// Index error at segment " + seg.getId() + ": " + seg.getStartPointIndex() + " > " + seg.getEndPointIndex());
				}
			}	

			List<String> segmentInfoList = new ArrayList<>(path.getMatchedWaySegments().size());
			for (IMatchedWaySegment segment : path.getMatchedWaySegments()) {
				segmentInfoList.add(segment.getId() + "(" + segment.getStartPointIndex() + "-" + segment.getEndPointIndex() + ")");
			}
			log.debug("Path " + i + ": - " + StringUtils.join(segmentInfoList, ", "));
			///////////////////////////////////////////////////
			
			i++;
						
		}
	}

	private void handleCancellationException() {
//		globalStatistics.incrementNonSuccess(this.track.getMetadata().getNumberOfPoints());
		logCsv();
	}

	/**
	 * @param paths
	 * @return
	 */
	private int getMaxEndPointIndex(List<IMatchedBranch> paths) {
		int maxEndPointIndex = 0;
		for (IMatchedBranch path : paths) {
			int currentEndPointIndex = path.getMatchedWaySegments().get(path.getMatchedWaySegments().size() - 1).getEndPointIndex();
			if (currentEndPointIndex > maxEndPointIndex) {
				maxEndPointIndex = currentEndPointIndex;
			}
		}
		return maxEndPointIndex;
	}

	@VisibleForTesting
	protected boolean prepareMatching(String graphName, ITrack origTrack) {
		// analyze track sampling rate
		trackSanitizer.analyseTrack(origTrack, properties);
		
		// check bounds of graph and track
		if (!trackSanitizer.validateTrack(origTrack, graphMetadata, graphName)) {
			return false;
		}
		
		weightingStrategy = weightingStrategyFactory.getStrategy(track, properties);
		
		return true;
	}

	/**
	 * When matching iteratively, prepare the paths from the last iteration. The last
	 * segment of each path is matched again, so that new point of the track can be assigned
	 * to the last segment.
	 */
	private List<IMatchedBranch> prepareBranchesFromPreviousIteration(List<IMatchedBranch> branches, ITrack track) {
		for (IMatchedBranch branch : branches) {
			branch.setFinished(false);
			
			// set weighting strategy with new track
			branch.setWeightingStrategy(getWeightingStrategy());
			
			// check if the last segment of branch matches new track points
			segmentMatcher.rematchLastSegment(branch, track);
		}
		
		return branches;
	}

	private int getPreviousEndPointIndex(List<IMatchedBranch> branches) {
		IMatchedBranch bestBranch = branches.get(0);
		int previousStartPoint = bestBranch.getMatchedWaySegments().get(bestBranch.getMatchedWaySegments().size() - 1)
									.getEndPointIndex();
		
		return previousStartPoint;
	}

	/**
	 * Checks if the map matching has been canceled. If so, a {@link CancellationException}
	 * is thrown.
	 * 
	 * @throws CancellationException
	 */
	void checkCancelStatus() throws CancellationException {
		if (cancellationObject.booleanValue()) {
			throw new CancellationException();
		}
	}

	List<IMatchedBranch> getResult(List<IMatchedBranch> detectedPaths, Long optionalStartSegmentId) {
		if (detectedPaths != null && !detectedPaths.isEmpty()) {
			
			// do some filtering
			detectedPaths = postSegmentFilter(detectedPaths);
			
			// filter empty paths
			List<IMatchedBranch> nonEmptyPaths = MapMatchingUtil.filterEmptyBranches(detectedPaths);
			
			if (nonEmptyPaths.isEmpty()) {
				return Collections.emptyList();
			}
			
			// sort paths
			Collections.sort(nonEmptyPaths, weightingStrategy.getComparator());
			
			if (properties.isOnlyBestResult()) {
				
				IMatchedBranch bestBranch = nonEmptyPaths.get(0);
				
				if (optionalStartSegmentId != null &&
					nonEmptyPaths.get(0).getMatchedWaySegments().get(0).getSegment().getId() != optionalStartSegmentId) {

					
					log.warn("path's start segment ID " + nonEmptyPaths.get(0).getMatchedWaySegments().get(0).getSegment().getId() + 
							 " is not equal to requested start segment ID" + optionalStartSegmentId);
					
					
//					bestBranch = null;
					for (IMatchedBranch branch : nonEmptyPaths) {
						if (branch.getMatchedWaySegments().get(0).getSegment().getId() == optionalStartSegmentId) {
							bestBranch = branch;
						}
					}
				}
				
				// return best match
				if (bestBranch == null) {
					return Collections.emptyList();
				} else {
					postProcess(bestBranch);
					return Collections.singletonList(bestBranch);
				}
			} else {
				// return all paths
				return nonEmptyPaths;
			}
		} else {
			return Collections.emptyList();
		}
	}

	private void postProcess(IMatchedBranch bestBranch) {
		// direction of first segment may not be correct
		if (bestBranch.getMatchedWaySegments().size() > 1) {
			IMatchedWaySegment startSegment = bestBranch.getMatchedWaySegments().get(0);
			IMatchedWaySegment nextSegment = bestBranch.getMatchedWaySegments().get(1);
			if (startSegment.getEndNodeId() == nextSegment.getStartNodeId() || 
				startSegment.getEndNodeId() == nextSegment.getEndNodeId()) {
				startSegment.setDirection(Direction.START_TO_END);
			} else {
				startSegment.setDirection(Direction.END_TO_START);
			}
		}
		
		// TODO: Could this result in errors?
		// in cases of determining certain paths indices could be invalid
		correctIndices(bestBranch);
	}

	private void correctIndices(IMatchedBranch branch) {
		if (branch.getMatchedWaySegments().size() > 2) {
			IMatchedWaySegment previousSegment = branch.getMatchedWaySegments().get(0);
			IMatchedWaySegment currentSegment = branch.getMatchedWaySegments().get(1);
			for (int i=2; i<branch.getMatchedWaySegments().size(); i++) {
				if (previousSegment.getEndPointIndex() > currentSegment.getStartPointIndex()) {
					updateIndices(currentSegment, previousSegment);
				}
				previousSegment = currentSegment;
				currentSegment = branch.getMatchedWaySegments().get(i);
			}
			
		}
	}

	private void updateIndices(IMatchedWaySegment currentSegment, IMatchedWaySegment previousSegment) {
		int newStartIndex = segmentMatcher.updateMatchesOfPreviousSegment(previousSegment.getEndPointIndex(), previousSegment, currentSegment, track);
		currentSegment.setStartPointIndex(newStartIndex);
		currentSegment.calculateDistances(track);
	}

	/**
	 * Removes empty segments at the end of the path and short sections.
	 */
	private List<IMatchedBranch> postSegmentFilter(List<IMatchedBranch> paths) {
		for (IMatchedBranch branch : paths) {
			removeEmptySegmentsAtStart(branch);
			removeEmptySegmentsAtEnd(branch);
			removeShortSections(branch);
			removeEmptySegmentsAtEnd(branch);
		}
		return paths;
	}

	/**
	 * Removes short sections from the path. 
	 * 
	 * Sections of a path are the segment groups between the start, skipped parts and the end. 
	 * A section is considered 'short' if less than 5 segments of the segments have 
	 * matching points.
	 */
	protected void removeShortSections(IMatchedBranch branch) {
		if (branch.getMatchedWaySegments().size() <= 0) return;
		
		List<Integer> segmentsToRemove = new ArrayList<Integer>();
		List<IMatchedWaySegment> segments = branch.getMatchedWaySegments();
		
		int matchingSegmentsOfCurrentSection = 1;
		List<Integer> segmentsOfCurrentSection = new LinkedList<Integer>();
		segmentsOfCurrentSection.add(0);
		
		for (int i = 1; i < segments.size(); i++) {
			IMatchedWaySegment currentSegment = segments.get(i);
			
			if (currentSegment.isAfterSkippedPart()) {
				// points were skipped between the current and the previous segment
				if (matchingSegmentsOfCurrentSection < properties.getMinSegmentsPerSection()) {
					// the last section did not contain enough matching segments, remove that section
					segmentsToRemove.addAll(segmentsOfCurrentSection);
				}
				
				// start a new section after the skipped part
				matchingSegmentsOfCurrentSection = 0;
				segmentsOfCurrentSection.clear();
			}
			
			segmentsOfCurrentSection.add(i);
			if (currentSegment.getMatchedPoints() > 0) {
				matchingSegmentsOfCurrentSection++;
			}
		}
		
		// also check if the very last section is too short
		if (matchingSegmentsOfCurrentSection < properties.getMinSegmentsPerSection()) {
			segmentsToRemove.addAll(segmentsOfCurrentSection);
		}
		
		branch.removeMatchedWaySegments(segmentsToRemove);
	}
	
	private void removeEmptySegmentsAtStart(IMatchedBranch branch) {
		boolean doFiltering = true;
		int i = 0;
		List<Integer> segmentsToRemove = new ArrayList<Integer>();
		IMatchedWaySegment seg;
		while (i < branch.getMatchedWaySegments().size() && doFiltering) {
			seg = branch.getMatchedWaySegments().get(i);
			if (seg.getStartPointIndex() == seg.getEndPointIndex()) {
				segmentsToRemove.add(i);
			} else {
				doFiltering = false;
			}
			i++;
		}
		
		branch.removeMatchedWaySegments(segmentsToRemove);
	}

	protected void removeEmptySegmentsAtEnd(IMatchedBranch branch) {
		boolean doFiltering = true;
		int i = branch.getMatchedWaySegments().size() - 1;
		List<Integer> segmentsToRemove = new ArrayList<Integer>();
		IMatchedWaySegment seg;
		while (i >= 0 && doFiltering) {
			seg = branch.getMatchedWaySegments().get(i);
			if (seg.getStartPointIndex() == seg.getEndPointIndex()) {
				segmentsToRemove.add(i);
			} else {
				doFiltering = false;
			}
			i--;
		}
		
		branch.removeMatchedWaySegments(segmentsToRemove);
	}
	
	public void cancel() throws InterruptedException {
		log.info("Cancel requested for track " + track.getId());
//		cancelRequested = true;
		cancellationObject.setTrue();
		throw new InterruptedException();
	}
	
	private void logCsv() {
		// log statistics to CSV
		if (csvLogger != null) {
			csvLogger.info(statistics.toCsv());
		}
	}

	// === GETTERS AND SETTERS ===
	
	@Override
	public ITrack getTrack() {
		return track;
	}
	
	@Override
	public String getGraphName() {
		return graphMetadata.getGraphName();
	}
	
	@Override
	public String getGraphVersion() {
		return graphMetadata.getVersion();
	}

	public INeo4jWayGraphReadDao getGraphDao() {
		return mapMatcher.getGraphDao();
	}

	@Override
	public MapMatcherStatistics getStatistics() {
		return statistics;
	}

	public IMapMatchingProperties getProperties() {
		return properties;
	}

	public IMapMatcher getMapMatcher() {
		return mapMatcher;
	}

	protected InitialMatcher getInitialMatcher() {
		return initialMatcher;
	}

	protected SegmentMatcher getSegmentMatcher() {
		return segmentMatcher;
	}

	protected PathExpanderMatcher getPathExpanderMatcher() {
		return pathExpanderMatcher;
	}

	protected RoutingMatcher getRoutingMatcher() {
		return routingMatcher;
	}

	protected AlternativePathMatcher getAlternativePathMatcher() {
		return alternativePathMatcher;
	}

	protected MatchesFilter getMatchesFilter() {
		return matchesFilter;
	}

	public IWeightingStrategy getWeightingStrategy() {
		return weightingStrategy;
	}

	public Neo4jUtil getNeo4jUtil() {
		return neo4jUtil;
	}

	public void setNeo4jUtil(Neo4jUtil neo4jUtil) {
		this.neo4jUtil = neo4jUtil;
	}

}