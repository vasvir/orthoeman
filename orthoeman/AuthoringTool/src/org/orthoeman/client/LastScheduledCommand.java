package org.orthoeman.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class LastScheduledCommand implements ScheduledCommand {
	private final ScheduledCommand sc;
	private int scheduled = 0;
	private final Log log = LogFactory.getLog(getClass());

	public LastScheduledCommand(ScheduledCommand sc) {
		log.debug("Constructor lsc " + " sc: " + sc);
		this.sc = sc;
	}

	public void schedule() {
		log.debug("Scheduling lsc " + scheduled + " sc: " + sc);
		Scheduler.get().scheduleDeferred(this);
		scheduled++;
	}

	@Override
	public void execute() {
		scheduled--;
		if (scheduled > 0) {
			log.debug("Avoiding lsc " + scheduled + " sc: " + sc);
			return;
		}
		log.debug("Executing lsc sc: " + sc);
		sc.execute();
	}
}
