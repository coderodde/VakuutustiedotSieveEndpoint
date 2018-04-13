package fi.vakuutustiedot.frontenddevelopersieve;

import org.junit.Test;

public class DeviceEndpointTest {
    
    @Test
    public void testSomeMethod() {
        Device dev = new Device();
        dev.setId(12);
        dev.setDescription("description is here!");
        dev.setName("funk -o- device");
        dev.setStatus(true);
//        System.out.println(new DeviceEndpoint().getToggleMessageJson(dev));
    }
    
    @Test
    public void testJson() {
        Device device = new Device();
        device.setId(19);
        device.setName("Coffee machine");
        device.setDescription(" Makes morning coffee  ");
        device.setStatus(true);
        System.out.println(new DeviceEndpoint().getCreateDeviceMessageJson(device));
        System.out.println(new DeviceEndpoint().getToggleDeviceSuccessMessageJson(device));
        System.out.println(new DeviceEndpoint().getToggleDeviceFailureMessageJson(device));
        System.out.println(new DeviceEndpoint().getRemoveDeviceSuccessMessageJson(device));
        System.out.println(new DeviceEndpoint().getRemoveDeviceFailureMessageJson(device));
    }
}
