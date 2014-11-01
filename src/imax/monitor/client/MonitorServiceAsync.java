package imax.monitor.client;

import imax.monitor.shared.IMonitor;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface MonitorServiceAsync {
    void addMonitor(IMonitor entity, String code, AsyncCallback<String> callback);
}
