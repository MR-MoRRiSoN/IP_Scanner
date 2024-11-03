package com.morrison.ip_scanner.modal;

public record NetworkDevice(String ipAddress, String macAddress) {
    @Override
    public String toString() {
        return "NetworkDevice{" +
                "ipAddress='" + ipAddress + '\'' +
                ", macAddress='" + macAddress + '\'' +
                '}';
    }
}

