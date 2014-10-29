package imax.monitor.client;

import imax.monitor.shared.Monitor;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface MonitorServiceAsync {
    void addMonitor(Monitor entity, String code, AsyncCallback<String> callback);
}
