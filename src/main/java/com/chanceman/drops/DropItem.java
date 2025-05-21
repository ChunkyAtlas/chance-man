package com.chanceman.drops;

import lombok.Getter;
import lombok.Setter;

public class DropItem
{
    @Setter @Getter private int itemId;
    @Setter @Getter private String name;
    @Setter @Getter private String quantity;
    @Setter @Getter private double rarity;
    @Setter @Getter private int exchangePrice;
    @Setter @Getter private int highAlchemyPrice;
    @Setter @Getter private String imageUrl;

    public DropItem(int itemId, String name, String quantity, double rarity, int exchangePrice, int highAlchemyPrice, String imageUrl)
    {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.rarity = rarity;
        this.exchangePrice = exchangePrice;
        this.highAlchemyPrice = highAlchemyPrice;
        this.imageUrl = imageUrl;
    }
}