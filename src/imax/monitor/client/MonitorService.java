package imax.monitor.client;

import imax.monitor.shared.IMonitor;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("monitor")
public interface MonitorService extends RemoteService {
    String addMonitor(IMonitor entity, String code);
}
