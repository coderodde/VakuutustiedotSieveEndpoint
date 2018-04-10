package fi.vakuutustiedot.frontenddevelopersieve;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * 
 * @author Rodion "rodde" Efremov
 * @version 1.6 (Apr 9, 2018)
 */
@ServerEndpoint("/devices")
public final class DeviceEndpoint {
    
    private static final class JsonDefinitions {
        private static final String ACTION = "action";
        private static final String DEVICE_ID = "deviceId";
        private static final String DEVICE_NAME = "deviceName";
        private static final String DEVICE_DESCRIPTION = "deviceDescription";
        private static final String DEFICE_STATUS = "deviceStatus";
        
        private static final class Actions {
            private static final String CREATE = "create";
            private static final String TOGGLE = "toggle";
            private static final String DELETE = "delete";
        }
    }
    
    // Used for creating IDs for the devices.
    private static int deviceCounter = 0;
    private static final Set<Session> sessions = new HashSet<>();
    private static final Map<Integer, Device> mapDeviceIdToDevice = 
            new HashMap<>();
    
    @OnOpen
    public void open(Session session) {
        sessions.add(session);
    }
    
    @OnClose
    public void close(Session session) {
        sessions.remove(session);
    }
    
    @OnError
    public void onError(Throwable error) {
        
    }
    
    @OnMessage
    public void handleMessage(String message, Session session) {
        try (JsonReader reader = 
                Json.createReader(new StringReader(message))) {
            JsonObject jsonMessage = reader.readObject();
            String actionName = jsonMessage.getString(JsonDefinitions.ACTION);
            
            switch (actionName) {
                case JsonDefinitions.Actions.CREATE:
                    handleCreateMessage(jsonMessage);
                    break;
                    
                case JsonDefinitions.Actions.TOGGLE:
                    handleToggleMessage(jsonMessage);
                    break;
                    
                case JsonDefinitions.Actions.DELETE:
                    handleDeleteMessage(jsonMessage);
                    break;
                    
                default:
                    throw new IllegalStateException(
                            "Unknown action: \"" + actionName + "\".");
            }
        }
    }
    
    /**
     * Creates and stores a new device and notifies all the clients of the newly
     * created device.
     * 
     * @param jsonObject the JSON object describing the new device.
     */
    private void handleCreateMessage(JsonObject jsonObject) {
        int deviceId = deviceCounter++;
        String deviceName = jsonObject.getString(JsonDefinitions.DEVICE_NAME);
        String deviceDescription = 
                jsonObject.getString(JsonDefinitions.DEVICE_DESCRIPTION);
        
        Device device = new Device();
        
        device.setId(deviceId);
        device.setName(deviceName);
        device.setDescription(deviceDescription);
        device.setStatus(false); // Each new device is initially turned off.
        
        mapDeviceIdToDevice.put(device.getId(), device);
        String jsonMessage = getCreateDeviceMessageJson(device);
        broadcastMessageToAllConnectedSessions(jsonMessage);
    }
    
    /**
     * Toggles the status of a device.
     * 
     * @param jsonObject the JSON object describing the device toggle action.
     */
    private void handleToggleMessage(JsonObject jsonObject) {
        int deviceId = jsonObject.getInt(JsonDefinitions.DEVICE_ID);
        Device device = mapDeviceIdToDevice.get(deviceId);
        
        if (device == null) {
            String errorMessageJson = composeMissingDeviceErrorJSON(deviceId);
            broadcastMessageToAllConnectedSessions(errorMessageJson);
        } else {
            device.setStatus(!device.getStatus());
            String jsonMessage = getToggleDeviceMessageJson(device);
            broadcastMessageToAllConnectedSessions(jsonMessage);
        }
    }
    
    /**
     * Deletes a device.
     * 
     * @param jsonObject the JSON object describing the device delete action.
     */
    private void handleDeleteMessage(JsonObject jsonObject) {
        int deviceId = jsonObject.getInt(JsonDefinitions.DEVICE_ID);
        Device device = mapDeviceIdToDevice.get(deviceId);
        
        if (device == null) {
            String errorMessageJson = composeMissingDeviceErrorJSON(deviceId);
            broadcastMessageToAllConnectedSessions(errorMessageJson);
        } else {
            mapDeviceIdToDevice.remove(deviceId);
            String jsonMessage = getDeleteDeviceMessageJson(device);
            broadcastMessageToAllConnectedSessions(jsonMessage);
        }
    }
    
    /**
     * Composes the error message JSON that describes that a device with given
     * ID does not exist.
     * 
     * @param missingDeviceId the ID of a missing device.
     * @return JSON text describing the error.
     */
    private String composeMissingDeviceErrorJSON(int missingDeviceId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"success\":\"false\",");
        stringBuilder.append("\"errorMessage\":\"");
        stringBuilder.append("No device with ID ");
        stringBuilder.append(missingDeviceId);
        stringBuilder.append(".\"}");
        return stringBuilder.toString();
    }
    
    /**
     * Sends the input text message to all connected sessions.
     * 
     * @param message the message to send.
     */
    private void broadcastMessageToAllConnectedSessions(String message) {
        for (Session session : sessions)  {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException ex) {
                
            }
        }
    }
    
    /**
     * Composes the JSON message representing the event of adding a new device.
     * 
     * @param device the new device.
     * @return the JSON message representing the action.
     */
    private String getCreateDeviceMessageJson(Device device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"action\":\"");
        stringBuilder.append(JsonDefinitions.Actions.CREATE);
        stringBuilder.append("\",");
        stringBuilder.append("\"deviceId\":");
        stringBuilder.append(device.getId());
        stringBuilder.append(",\"deviceName\":\"");
        stringBuilder.append(device.getName());
        stringBuilder.append("\",deviceDescription\":\"");
        stringBuilder.append(device.getDescription());
        stringBuilder.append("\",\"deviceStatus\":");
        stringBuilder.append(device.getStatus());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
    
    /**
     * Composes the JSON message representing device status change action.
     * 
     * @param device the target device.
     * @return the JSON message representing the action.
     */
    private String getToggleDeviceMessageJson(Device device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"action\":\"");
        stringBuilder.append(JsonDefinitions.Actions.TOGGLE);
        stringBuilder.append("\",");
        stringBuilder.append("\"");
        stringBuilder.append(JsonDefinitions.DEVICE_ID);
        stringBuilder.append("\":");
        stringBuilder.append(device.getId());
        stringBuilder.append(",");
        stringBuilder.append("\"");
        stringBuilder.append(JsonDefinitions.DEFICE_STATUS);
        stringBuilder.append("\":");
        stringBuilder.append(device.getStatus());
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
    
    /**
     * Composes the JSON message representing device delete action.
     * 
     * @param device the target device.
     * @return the JSON message representing the action.
     */
    private String getDeleteDeviceMessageJson(Device device) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"action\":\"");
        stringBuilder.append(JsonDefinitions.Actions.DELETE);
        stringBuilder.append("\",");
        stringBuilder.append("\"");
        stringBuilder.append(JsonDefinitions.DEVICE_ID);
        stringBuilder.append("\":");
        stringBuilder.append(device.getId());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
