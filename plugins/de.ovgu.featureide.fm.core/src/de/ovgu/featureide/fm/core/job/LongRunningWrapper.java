/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.core.job;

import de.ovgu.featureide.fm.core.FMCorePlugin;

/**
 * Job that wraps the functionality of a {@link LongRunningMethod}.
 * 
 * @author Sebastian Krieter
 */
public abstract class LongRunningWrapper {

	public static <T> LongRunningJob<T> createJob(String name, LongRunningMethod<T> method) {
		return new LongRunningJob<T>(name, method);
	}

	public static <T> LongRunningJob<T> startJob(String name, LongRunningMethod<T> method) {
		final LongRunningJob<T> job = new LongRunningJob<T>(name, method);
		job.schedule();
		return job;
	}

	public static Thread createThread(final LongRunningMethod<?> method) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					method.execute(new WorkMonitor());
				} catch (Exception e) {
					FMCorePlugin.getDefault().logError(e);
				}
			}
		});
	}

	public static Thread startThread(final LongRunningMethod<?> method) {
		final Thread thread = createThread(method);
		thread.start();
		return thread;
	}

	public static <T> T runMethod(LongRunningMethod<T> method) {
		try {
			return method.execute(new WorkMonitor());
		} catch (Exception e) {
			FMCorePlugin.getDefault().logError(e);
			return null;
		}
	}

	public static <T> T runMethod(LongRunningMethod<T> method, WorkMonitor monitor) {
		try {
			monitor = monitor != null ? monitor : new WorkMonitor();
			monitor.begin("");
			final T result = method.execute(monitor);
			monitor.done();
			return result;
		} catch (Exception e) {
			FMCorePlugin.getDefault().logError(e);
			return null;
		}
	}

}
