package org.gms.idle;

public final class IdleRewardItem {
    private final int itemId;
    private final int quantity;

    public IdleRewardItem(int itemId, int quantity) {
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public int getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
