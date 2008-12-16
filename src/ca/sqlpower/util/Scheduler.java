package ca.sqlpower.util;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * The Scheduler class is a cron-like facility for Java.
 */
public class Scheduler {
	private static final Logger logger = Logger.getLogger(Scheduler.class);

	/**
	 * This List contains zero or more ScheduledTask objects sorted by
	 * nextOccurrence(baseDate) with the first scheduled occurrence
	 * at position 0 in the list.  If two or more scheduled tasks are
	 * to occur at the same time, their order relative to each other
	 * is random.
	 */
	protected static List schedule;
	
	/**
	 * This is the base date used for sorting the schedule by next
	 * occurrence.  It is usually the most recently executed event.
	 *
	 * <p>This value should only be changed by threads that hold a
	 * lock on the schedule object.
	 */
	protected static Date baseDate;

	/**
	 * This thread is created when the Scheduler class is loaded.  It
	 * is responsible for running the ScheduledTask events at the
	 * correct time and rescheduling them as necessary.  It also gets
	 * notified when a scheduled task is added to or removed from the
	 * schedule list.
	 */
	protected static Thread schedulerThread;

	/**
	 * No-op constructor.  This class is meant to be used statically.
	 */
	protected Scheduler() {}

	static {
		schedule = new LinkedList();
		baseDate = new Date();
		schedulerThread = new SchedulerThread();
		schedulerThread.start();
	}

	/**
	 * Incorporates the given Runnable into the list of scheduled
	 * tasks, then wakes up the scheduler thread in case it needs to
	 * reconsider its sleeping time.
	 */
	public static void scheduleTask(Recurrence recurrence, Runnable task) {
		synchronized (schedule) {
			Date firstOccurrence = recurrence.nextOccurrence(baseDate);
			if (firstOccurrence == null) {
				logger.debug("Not scheduling new task because it has no more occurrences");
				return;
			}

			if (firstOccurrence.getTime() < System.currentTimeMillis()) {
				logger.debug("Newly scheduled task occurs after base date but before now."
							 +"  Updating base date.");
				baseDate = new Date();
			}
			schedule.add(0, new ScheduledTask(recurrence, task));
			Collections.sort(schedule);
			schedulerThread.interrupt();
		}
	}

	/**
	 * Removes the given Runnable task from the schedule.  Uses
	 * straight pointer comparison to find the task to remove, so
	 * you'll have to pass in a reference that you got from {@link
	 * #getScheduledTasks()}.
	 *
	 * @return The removed task, or null if the given task wasn't
	 * found in the list.
	 */
	public static ScheduledTask unscheduleTask(ScheduledTask task) {
		synchronized (schedule) {
			Iterator it = schedule.iterator();
			while (it.hasNext()) {
				ScheduledTask st = (ScheduledTask) it.next();
				if (st.task == task) {
					it.remove();
					return task;
				}
			}
			return null;
		}
	}

	public static List getScheduledTasks() {
		synchronized (schedule) {
			return Collections.unmodifiableList(schedule);
		}
	}

	public static Date getBaseDate() {
		return baseDate;
	}

	protected static class SchedulerThread extends Thread {

		public SchedulerThread() {
			super("SQLPower Cron");
			setDaemon(true);
		}

		public void run() {
			Date wakeup = null;
			ScheduledTask nextTask = null;

			for (;;) {
				try {
					synchronized (schedule) {
						if (!schedule.isEmpty()) {
							nextTask = (ScheduledTask) schedule.get(0);
							wakeup = nextTask.recurrence.nextOccurrence(baseDate);
						}
					}
					
					// determine how long to sleep (0 means sleep until interrupted)
					long sleepMillis = 0;
					if (wakeup != null) {
						sleepMillis = wakeup.getTime() - System.currentTimeMillis();
					}
					
					logger.debug("Sleeping "+(wakeup==null?"indefinitely":wakeup.toString()));
					
					try {
						join(sleepMillis);  // not using Thread.sleep() because 0 means forever
					} catch (InterruptedException e) {
						logger.debug("Received an interrupt while sleeping");
					}
					
					// remember when we woke up
					wakeup = new Date();
					
					// now that we're awake, we will perform all tasks that were due
					// at the actual wakeup time
					// XXX: schedule remains locked during task execution!
					synchronized (schedule) {
						if (!schedule.isEmpty()) {
							nextTask = (ScheduledTask) schedule.get(0);
							Date nextTime = nextTask.recurrence.nextOccurrence();
							while (nextTime != null
								   && nextTime.getTime() <= System.currentTimeMillis()) {
								try {
									logger.debug("Starting to run scheduled task");
									nextTask.task.run();
								} catch (Exception e) {
									logger.error("Scheduled task threw an exception", e);
								}
								baseDate = nextTime;
								Collections.sort(schedule);
								nextTask = (ScheduledTask) schedule.get(0);
								nextTime = nextTask.recurrence.nextOccurrence();
							}
						}
					}
				} catch (Exception e) {
					logger.error("Unexpected exception in scheduler thread", e);
					return; // XXX: should remove this in production!
				} finally {
					logger.error("SchedulerThread.run() is exiting!");
				}
			}
		}
	}

	/**
	 * A container for the objects that represent a scheduled task.
	 * Comparisons of objects of this type are based on the
	 * recurrence's nextOccurrence() after the Scheduler's current
	 * base date.
	 */
	public static class ScheduledTask {
		public Recurrence recurrence;
		public Runnable task;

		public ScheduledTask(Recurrence recurrence, Runnable task) {
			this.recurrence = recurrence;
			this.task = task;
		}

		/**
		 * Compares this ScheduledTask to the other given task.  The
		 * comparison is based on the return value of the recurrence's
		 * nextOccurrence(Scheduler.baseDate).  If other is not an instance of
		 * ScheduledTask, you will get a ClassCastException.
		 *
		 * <p>A null nextOccurrence (meaning the task has no more
		 * scheduled occurrences) sorts to the end (infinitely far in
		 * the future).  This helps with pruning the schedule because
		 * after sorting all the expired tasks will be grouped at the
		 * end.
		 */
		public int compareTo(Object other) {
			ScheduledTask otherTask = (ScheduledTask) other;
			Date thisNextOccurrence = this.recurrence.nextOccurrence(baseDate);
			Date otherNextOccurrence = otherTask.recurrence.nextOccurrence(baseDate);
			if (thisNextOccurrence == null && otherNextOccurrence == null) {
				return 0;
			} else if (thisNextOccurrence == null) {
				return 1;
			} else if (otherNextOccurrence == null) {
				return -1;
			} else {
				return (int) (thisNextOccurrence.getTime() - otherNextOccurrence.getTime());
			}
		}

		/**
		 * Tells if two scheduled tasks will occur next at exactly the
		 * same time.  Warning: this method was implemented so equals
		 * could be consistent with compareTo, but it is not all that
		 * useful in reality.  Two different scheduled tasks will be
		 * considered equal if their next occurrences coincide.
		 */
		@Override
		public boolean equals(Object other) {
			ScheduledTask otherTask = (ScheduledTask) other;
			Date thisNextOccurrence = this.recurrence.nextOccurrence(baseDate);
			Date otherNextOccurrence = otherTask.recurrence.nextOccurrence(baseDate);
			if (thisNextOccurrence == null && otherNextOccurrence == null) {
				return true;
			} else if (thisNextOccurrence == null) {
				return false;
			} else if (otherNextOccurrence == null) {
				return false;
			} else {
				return thisNextOccurrence.getTime() == otherNextOccurrence.getTime();
			}
		}
		
		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + this.recurrence.nextOccurrence(baseDate).hashCode();
			return result;
		}
	}
}
