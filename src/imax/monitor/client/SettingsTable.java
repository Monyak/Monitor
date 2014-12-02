package imax.monitor.client;

import imax.monitor.shared.IMonitor;
import imax.monitor.shared.MovieMonitor;
import imax.monitor.shared.SeatMonitor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class SettingsTable extends Composite {

    private final MonitorServiceAsync monitorService = GWT.create(MonitorService.class);

    private static SettingsTableUiBinder uiBinder = GWT.create(SettingsTableUiBinder.class);

    interface SettingsTableUiBinder extends UiBinder<Widget, SettingsTable> {
    }
    
    @UiField
    RadioButton seatsType;
    
    @UiField
    RadioButton movieType;
    
    @UiField
    TextBox ids;
    
    @UiField
    TextBox rowsFrom;
    
    @UiField
    TextBox rowsTo;
    
    @UiField
    TextBox seatsFrom;
    
    @UiField
    TextBox seatsTo;
    
    @UiField
    TextBox email;
    
    @UiField
    TextBox code;
    
    @UiField
    Button send;
    
    @UiField
    TextBox date;
    
    @UiField
    HTMLPanel rowPanel;
    
    @UiField
    HTMLPanel cellPanel;
    
    @UiField
    HTMLPanel datePanel;
    
    public SettingsTable() {
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    @UiHandler({"seatsType","movieType"})
    public void onChange(ValueChangeEvent<Boolean> e) {
        rowPanel.setVisible(seatsType.getValue());
        cellPanel.setVisible(seatsType.getValue());
        datePanel.setVisible(movieType.getValue());
    }
    
    @UiHandler("send")
    public void onSendClick(ClickEvent e) {
        IMonitor entity = null;
        if (seatsType.getValue()) {
            int[] id;
            try {
                String[] idStr = ids.getText().split(",");
                id = new int[idStr.length];
                
                for (int i = 0; i < idStr.length; i++) {
                    id[i] = Integer.valueOf(idStr[i].trim());
                }
            } catch (Exception ex) {
                Window.alert("Cannot parse ids");
                return;
            }
            
            int[] rows = new int[2];
            int[] seats = new int[2];
            try {
                rows[0] = Integer.valueOf(rowsFrom.getText().trim());
                rows[1] = Integer.valueOf(rowsTo.getText().trim());
                
                seats[0] = Integer.valueOf(seatsFrom.getText().trim());
                seats[1] = Integer.valueOf(seatsTo.getText().trim());
            } catch (Exception ex) {
                Window.alert("Cannot parse rows or seats");
                return;
            }
            if (email.getText().trim().isEmpty() || !email.getText().contains("@")) {
                Window.alert("Invalid email");
                return;
            }
            entity = new SeatMonitor(id, rows, seats, email.getText().trim());
        } else {
            String id;
            try {
                id = ids.getText();
            } catch (Exception ex) {
                Window.alert("Cannot parse id");
                return;
            }
            if (email.getText().trim().isEmpty() || !email.getText().contains("@")) {
                Window.alert("Invalid email");
                return;
            }
            if (date.getText().trim().isEmpty()) {
                Window.alert("Invalid date");
                return;
            }
            entity = new MovieMonitor(id, date.getText().trim(), email.getText().trim());
        }
        monitorService.addMonitor(entity, code.getText(), new AsyncCallback<String>() {
            
            @Override
            public void onSuccess(String result) {
                Window.alert(result);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Error:\n" + caught.getMessage());
            }
        });
    }

}
