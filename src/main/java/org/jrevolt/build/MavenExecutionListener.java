package org.jrevolt.build;

import static java.util.Collections.synchronizedList;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
public class MavenExecutionListener extends AbstractExecutionListener {

	static Map<MavenSession, MavenExecutionListener> listenerBySession = new HashMap<MavenSession, MavenExecutionListener>();

	static public synchronized MavenExecutionListener forSession(MavenSession session) {
		MavenExecutionListener listener = listenerBySession.get(session);
		if (listener == null) {
			listener = new MavenExecutionListener(session);
			listener.install();
			listenerBySession.put(session, listener);
		}
		return listener;
	}

	MavenSession session;
	ExecutionListener original;
	List<ExecutionListener> listeners;

	private MavenExecutionListener(MavenSession session) {
		this.session = session;
		this.listeners = synchronizedList(new LinkedList<ExecutionListener>());
	}

	///

	private synchronized void install() {
		original = session.getRequest().getExecutionListener();
		addListener(original);
		session.getRequest().setExecutionListener(this);
	}

	private synchronized void uninstall() {
		removeListener(original);
		session.getRequest().setExecutionListener(original);
	}

	public void addListener(ExecutionListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ExecutionListener listener) {
		listeners.remove(listener);
	}

	///


	@Override
	public void forkedProjectFailed(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.forkedProjectFailed(event);
		}
	}

	@Override
	public void forkedProjectSucceeded(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.forkedProjectSucceeded(event);
		}
	}

	@Override
	public void forkedProjectStarted(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.forkedProjectStarted(event);
		}
	}

	@Override
	public void mojoFailed(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.mojoFailed(event);
		}
	}

	@Override
	public void mojoSucceeded(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.mojoSucceeded(event);
		}
	}

	@Override
	public void mojoStarted(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.mojoStarted(event);
		}
	}

	@Override
	public void mojoSkipped(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.mojoSkipped(event);
		}
	}

	@Override
	public void forkFailed(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.forkFailed(event);
		}
	}

	@Override
	public void forkSucceeded(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.forkSucceeded(event);
		}
	}

	@Override
	public void forkStarted(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.forkStarted(event);
		}
	}

	@Override
	public void projectFailed(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.projectFailed(event);
		}
	}

	@Override
	public void projectSucceeded(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.projectSucceeded(event);
		}
	}

	@Override
	public void projectStarted(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.projectStarted(event);
		}
	}

	@Override
	public void projectSkipped(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.projectSkipped(event);
		}
	}

	@Override
	public void sessionEnded(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.sessionEnded(event);
		}
		listenerBySession.remove(event.getSession());
		uninstall();
	}

	@Override
	public void sessionStarted(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.sessionStarted(event);
		}
	}

	@Override
	public void projectDiscoveryStarted(ExecutionEvent event) {
		for (ExecutionListener listener : listeners) {
			listener.projectDiscoveryStarted(event);
		}
	}
}
