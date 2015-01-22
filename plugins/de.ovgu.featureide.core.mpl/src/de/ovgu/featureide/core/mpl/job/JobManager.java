/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.core.mpl.job;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;

import de.ovgu.featureide.core.mpl.MPLPlugin;
import de.ovgu.featureide.core.mpl.job.util.IChainJob;
import de.ovgu.featureide.fm.core.job.IJob.JobStatus;

/**
 * Class for starting jobs.
 * It is possible to add jobs to a certain sequence, so they are executed consecutively.
 * Jobs in different sequences are executed in parallel.
 * 
 * It is also possible to wait for a sequence to finish.
 * 
 * @author Sebastian Krieter
 */
public abstract class JobManager {
	private static class SequenceData {
		private final LinkedList<AChainJob<?>> jobs = new LinkedList<AChainJob<?>>();
		private final LinkedList<SequenceFinishedListener> listener = new LinkedList<SequenceFinishedListener>();
		private boolean running = false;
	}
	
	private static final Map<Object, SequenceData> sequenceDataMap = new WeakHashMap<Object, SequenceData>();
	
	/**
	 * Adds a job to the job sequence of the given project.
	 * If no other job is currently scheduled the new job is started instantly.
	 * 
	 * @param project the project.
	 * @param newJob the new job to add.
	 * @return the id object (in this case the project's name)
	 * 
	 * @see #addJob(Object, IChainJob)
	 * @see #addJob(Object, IChainJob, boolean)
	 */
	public static void addJob(IProject project, IChainJob newJob) {
		addJob(project.getName(), newJob, true);
	}
	
	/**
	 * Executes {@link #addJob(Object, IChainJob, boolean)} with {@code true} as third parameter (so the sequence is started automatically).
	 * 
	 * @param idObject the id object for the sequence.
	 * @param newJob the new job to add.
	 * @return the id object
	 */
	public static void addJob(Object idObject, IChainJob newJob) {
		addJob(idObject, newJob, true);
	}
	
	/**
	 * Adds a job to job sequence with the specified id object.
	 * 
	 * @param idObject the id object for the sequence.
	 * @param newJob the new job to add.
	 * @param autoStart if {@code true} the sequence is started automatically.
	 * @return the id object
	 * 
	 * @see #addJob(IProject, IChainJob)
	 * @see #addJob(Object, IChainJob)
	 */
	public static synchronized void addJob(Object idObject, IChainJob newJob, boolean autoStart) {
		final SequenceData sequenceData = getData(idObject);
		final AChainJob<?> newAJob = (AChainJob<?>) newJob;
		sequenceData.jobs.add(newAJob);
		newAJob.setSequenceObject(idObject);
		if (autoStart && !sequenceData.running) {
			sequenceData.running = true;
			sequenceData.jobs.peekFirst().schedule();
		}
	}
	
	/**
	 * Starts a job sequence which is not already running.
	 * 
	 * @param idObject the id object for the sequence.
	 * @param waitForTheEnd if {@code true} this method waits for the sequence to end, before returning.
	 * 
	 * @return {@code false} if the waiting was interrupted, {@code true} otherwise.
	 * 
	 * @see #waitForTheEnd(Object)
	 */
	public static void startSequence(Object idObject) {
		final SequenceData sequenceData = getData(idObject);
		synchronized (sequenceData) {
			final AChainJob<?> firstJob = sequenceData.jobs.peekFirst();
			if (firstJob != null && !sequenceData.running) {
				sequenceData.running = true;
				firstJob.schedule();
			}
		}
	}
	
	/**
	 * This method adds a {@link SequenceFinishedListener} to the specified sequence.
	 * All listeners are removed once the sequence is over.
	 * 
	 * @param idObject the id object for the sequence
	 * @param listener the listener
	 */
	public static void addSequenceFinishedListener(Object idObject, SequenceFinishedListener listener) {
		final SequenceData sequenceData = getData(idObject);
		synchronized (sequenceData) {
			sequenceData.listener.add(listener);
		}
	}
	
	static void insertJobs(AChainJob<?> lastJob, Collection<IChainJob> jobs) {
		final SequenceData sequenceData = getData(lastJob.getSequenceObject());
		synchronized (sequenceData) {
			final ListIterator<AChainJob<?>> it = sequenceData.jobs.listIterator();
			while (it.hasNext()) {
				if (it.next().equals(lastJob)) {
					for (IChainJob job : jobs) {
						final AChainJob<?> newAJob = (AChainJob<?>) job;
						newAJob.setSequenceObject(lastJob.getSequenceObject());
						it.add(newAJob);
					}
					break;
				}
			}
		}
	}
	
	static void startNextJob(Object idObject) {
		final SequenceData sequenceData = getData(idObject);
		synchronized (sequenceData) {
			final IChainJob lastJob = sequenceData.jobs.poll();
			if (lastJob != null) {
				JobStatus lastStatus = lastJob.getStatus();
				AChainJob<?> nextJob = null;

				for (final Iterator<AChainJob<?>> it = sequenceData.jobs.iterator(); it.hasNext();) {
					nextJob = it.next();
					if (nextJob.getStatus() == JobStatus.FAILED) {
						lastStatus = JobStatus.FAILED;
						it.remove();
					} else if (lastStatus == JobStatus.FAILED && !nextJob.ignoresPreviousJobFail()) {
						it.remove();
					} else {
						break;
					}
				}
				if (sequenceData.jobs.isEmpty()) {
					for (final Iterator<SequenceFinishedListener> it = sequenceData.listener.iterator(); it.hasNext();) {
					    try {
					    	it.next().sequenceFinished(idObject, lastStatus == JobStatus.OK);
					    }
					    catch (RuntimeException e) {
					        MPLPlugin.getDefault().logError(e);
					    }
					}
					sequenceDataMap.remove(idObject);
				} else {
					nextJob.schedule();
				}
			}
		}
	}
	
	private static synchronized SequenceData getData(Object idObject) {
		SequenceData sequenceData = sequenceDataMap.get(idObject);
		if (sequenceData == null) {
			sequenceData = new SequenceData();
			sequenceDataMap.put(idObject, sequenceData);
		}
		return sequenceData;
	}
}
