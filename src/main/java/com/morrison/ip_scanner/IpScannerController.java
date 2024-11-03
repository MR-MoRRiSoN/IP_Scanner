package com.morrison.ip_scanner;

import com.morrison.ip_scanner.modal.NetworkDevice;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IpScannerController {
    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button firstScanButton;
    @FXML
    private Button secondScanButton;

    @FXML
    private Button moveSelectedButton;
    @FXML
    private Button cleanSavedIps;

    @FXML
    private TableView<NetworkDevice> table;
    @FXML
    private TableColumn<NetworkDevice, String> ipColumn;
    @FXML
    private TableColumn<NetworkDevice, String> macColumn;
    @FXML
    private TableView<NetworkDevice> table_2;
    @FXML
    private TableColumn<NetworkDevice, String> ipColumn_2;
    @FXML
    private TableColumn<NetworkDevice, String> macColumn_2;

    @FXML
    private TextField start_ip_1;

    @FXML
    private TextField start_ip_2;

    @FXML
    private TextField start_ip_3;

    @FXML
    private TextField start_ip_4;

    @FXML
    private TextField end_ip_1;

    @FXML
    private TextField end_ip_2;

    @FXML
    private TextField end_ip_3;

    @FXML
    private TextField end_ip_4;

    private ObservableList<NetworkDevice> deviceList;
    private ObservableList<NetworkDevice> deviceList_2;


    private final List<NetworkDevice> firstScanNetworkDevices = new ArrayList<>();
    private final List<NetworkDevice> otherScanNetworkDevices = new ArrayList<>();
    private final int THREAD_POOL_SIZE = 50;
    private final int PING_TIMEOUT = 1000;
    private final Pattern MAC_PATTERN = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");

    private Boolean isManual = true;

    @FXML
    public void initialize() {
        deviceList = FXCollections.observableArrayList();
        deviceList_2 = FXCollections.observableArrayList();

        setupTextField(start_ip_1, start_ip_2);
        setupTextField(start_ip_2, start_ip_3);
        setupTextField(start_ip_3, start_ip_4);
        setupTextField(start_ip_4, end_ip_1);
        setupTextField(end_ip_1, end_ip_2);
        setupTextField(end_ip_2, end_ip_3);
        setupTextField(end_ip_3, end_ip_4);
        setupTextField(end_ip_4, end_ip_4); // This seems redundant, as end_ip_4 points to itself

        table.setItems(deviceList);
        table_2.setItems(deviceList_2);

        table_2.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // Check for double-click
                NetworkDevice selectedDevice = table_2.getSelectionModel().getSelectedItem();
                if (selectedDevice != null) {
                    // Remove the selected item from the list
                    deviceList_2.remove(selectedDevice);
                }
            }
        });

        // Remove auto-focus change behavior when updating end_ip fields
        setupEndIPListeners();

        // Table setup
        ipColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ipAddress()));
        macColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().macAddress()));

        ipColumn_2.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().ipAddress()));
        macColumn_2.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().macAddress()));

        firstScanButton.setOnAction(_ -> startScanning(true));
        secondScanButton.setOnAction(_ -> startScanning(false));
        cleanSavedIps.setOnAction(_ -> {
            deviceList_2.clear();
        });



        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        moveSelectedButton.setOnAction(_ -> moveSelectedDevices());
    }


    private void setupEndIPListeners() {
        start_ip_1.textProperty().addListener((observable, oldValue, newValue) -> {
            isManual = false;
            end_ip_1.setText(newValue);
            isManual = true;
        });

        start_ip_2.textProperty().addListener((observable, oldValue, newValue) -> {
            isManual = false;
            end_ip_2.setText(newValue);
            isManual = true;
        });

        start_ip_3.textProperty().addListener((observable, oldValue, newValue) -> {
            isManual = false;
            end_ip_3.setText(newValue);
            isManual = true;
        });
    }

    private void showError(String headerText, String contentText) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(headerText);

        dialog.getDialogPane().setContentText(contentText);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);


        dialog.showAndWait();
    }


    private void moveSelectedDevices() {
        ObservableList<NetworkDevice> selectedDevices = table.getSelectionModel().getSelectedItems();
        if (!selectedDevices.isEmpty()) {

            deviceList_2.addAll(selectedDevices);
        } else {
            System.out.println("No device selected.");
        }
    }

    private void setupTextField(TextField currentField, TextField nextField) {
        currentField.textProperty().addListener((_, _, newValue) -> {
            // Allow only digits
            if (!newValue.matches("\\d*")) {
                currentField.setText(newValue.replaceAll("\\D", ""));
            }

            // Limit to 3 digits
            if (newValue.length() > 3) {
                currentField.setText(newValue.substring(0, 3));
            }

            // Move focus only if the current field has exactly 3 characters
            if (currentField.getText().length() == 3 && isManual) {
                nextField.requestFocus();
            }
        });
    }

    private void startScanning(boolean isFirstScan) {

        String[] splitFirstIp = new String[]{start_ip_1.getText(), start_ip_2.getText(), start_ip_3.getText(), start_ip_4.getText()};
        String[] splitLastIp = new String[]{end_ip_1.getText(), end_ip_2.getText(), end_ip_3.getText(), end_ip_4.getText()};

        if (!isValidIpRange(splitFirstIp, splitLastIp)) {
            showError("ERROR", "WRONG IP RANGE");
            return;
        }

        progressBar.setProgress(0);
        deviceList.clear();
        if (isFirstScan) {
            firstScanNetworkDevices.clear();
            otherScanNetworkDevices.clear();
        } else {
            firstScanNetworkDevices.addAll(otherScanNetworkDevices);
            otherScanNetworkDevices.clear();
        }


        new Thread(() -> {
            String ipPrefix = String.join(".", splitFirstIp[0], splitFirstIp[1], splitFirstIp[2]) + ".";
            int startIp = Integer.parseInt(splitFirstIp[3]);
            int endIp = Integer.parseInt(splitLastIp[3]);
            int totalSteps = endIp - startIp + 1; // Total steps from start to end, inclusive
            CountDownLatch latch = new CountDownLatch(totalSteps); // Initialize latch with total steps
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            for (int i = startIp; i <= endIp; i++) {
                int currentStep = i - startIp;


                double progress = (double) currentStep / totalSteps * 100;
                final String ip = ipPrefix + i;

                executor.submit(() -> {
                    try {
                        String result = pingHost(ip);
                        if (result != null) {
                            String macAddress = getMacAddress(ip);

                            if (macAddress == null) {
                                macAddress = getCurrentDeviceMacAddress(ip);
                            }

                            String finalMacAddress = macAddress;
                            Platform.runLater(() -> {
                                var networkDevice = new NetworkDevice(ip, finalMacAddress);
                                if (isFirstScan) {
                                    firstScanNetworkDevices.add(networkDevice);
                                    deviceList.add(networkDevice);

                                } else {
                                    if (!firstScanNetworkDevices.contains(networkDevice)) {
                                        deviceList.add(networkDevice);
                                        otherScanNetworkDevices.add(networkDevice);
                                    }
                                }

                                progressBar.setProgress(progress / 100);
                            });
                        }
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }


            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


            Platform.runLater(() -> {

                System.out.println("Scanning complete!");

                progressBar.setProgress(1.0);
            });

            executor.shutdown();
        }).start();
    }


    private static String getCurrentDeviceMacAddress(String ipAddress) throws Exception {
        String macAddress = null; // Variable to hold the MAC address

        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Get the MAC address
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    StringBuilder macAddressBuilder = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        macAddressBuilder.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }

                    // Get IP addresses
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                    while (inetAddresses.hasMoreElements()) {
                        InetAddress inetAddress = inetAddresses.nextElement();
                        // Check if the address is not a loopback address and is an IPv4 address
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                            // Compare the current IP address with the given IP address
                            if (inetAddress.getHostAddress().equals(ipAddress)) {
                                // If it matches, return the MAC address
                                macAddress = macAddressBuilder.toString();
                                return macAddress; // Return the MAC address of the matching IP
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        // If no match found, return null or an appropriate value
        return null;
    }


    public String getMacAddress(String ip) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] commands;

        if (os.contains("win")) {
            commands = new String[]{"getmac /fo csv /v | findstr " + ip, "arp -a " + ip, "nbtstat -a " + ip};
        } else {
            commands = new String[]{"ip neigh show " + ip, "arp -n " + ip, "nmblookup -A " + ip};
        }

        for (String command : commands) {
            String mac = executeMacLookupCommand(command, ip);
            if (isValidMac(mac)) {
                return mac.toUpperCase();
            }
        }


        return getMacBySocket(ip);
    }

    private String getMacBySocket(String ip) {
        try {

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, 80), PING_TIMEOUT);
            socket.close();


            Thread.sleep(100);


            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getByName(ip));
            if (network != null) {
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    String macStr = sb.toString();
                    if (isValidMac(macStr)) {
                        return macStr;
                    }
                }
            }
        } catch (Exception _) {

        }
        return null;
    }

    private boolean isValidMac(String mac) {
        if (mac == null) return false;


        if (mac.replaceAll("[:-]", "").matches("^0+$")) return false;


        if (!MAC_PATTERN.matcher(mac).matches()) return false;


        String firstByte = mac.substring(0, 2).toUpperCase();

        if (firstByte.equals("FF")) return false;


        int firstByteInt = Integer.parseInt(firstByte, 16);
        if ((firstByteInt & 0x01) != 0) return false;

        String[] invalidPrefixes = {"00:00:00", "FF:FF:FF", "01:00:5E", "33:33:00", "01:80:C2", "00:00:5E", "01:00:0C",};

        for (String prefix : invalidPrefixes) {
            if (mac.toUpperCase().startsWith(prefix)) return false;
        }

        return true;
    }

    private String executeMacLookupCommand(String command, String targetIp) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(targetIp)) {
                        Matcher matcher = MAC_PATTERN.matcher(line);
                        if (matcher.find()) {
                            String mac = matcher.group().toUpperCase();
                            if (!mac.equals("00:00:00:00:00:00") && isValidMac(mac)) {
                                return mac;
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("Error executing command: " + command);
        }
        return null;
    }

    private String pingHost(String ip) {
        try {
            Process process;
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                process = Runtime.getRuntime().exec("ping -n 1 -w " + PING_TIMEOUT + " " + ip);
            } else {
                process = Runtime.getRuntime().exec("ping -c 1 -W " + (PING_TIMEOUT / 1000) + " " + ip);
            }

            int exitCode = process.waitFor();
            return exitCode == 0 ? ip : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isValidIpRange(String[] firstIp, String[] lastIp) {
        if (firstIp.length != 4 || lastIp.length != 4) return false;


        for (int i = 0; i < 3; i++) {
            if (!firstIp[i].equals(lastIp[i])) return false;
        }

        try {
            int start = Integer.parseInt(firstIp[3]);
            int end = Integer.parseInt(lastIp[3]);
            return start <= end && start >= 0 && end <= 255;
        } catch (NumberFormatException e) {
            return false;
        }


    }

}
