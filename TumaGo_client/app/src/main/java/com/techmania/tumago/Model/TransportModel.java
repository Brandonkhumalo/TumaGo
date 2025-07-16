package com.techmania.tumago.Model;

public class TransportModel {
    String TransportImage;
    String TransportName;
    double price;

    public TransportModel() {
    }

    public TransportModel(String TransportImage, String TransportName, double price) {
        this.TransportImage = TransportImage;
        this.TransportName = TransportName;
        this.price = price;
    }

    public String getImageName() {
        return TransportImage;
    }

    public String getTransportName() {
        return TransportName;
    }

    public double getPrice() {
        return price;
    }
}
