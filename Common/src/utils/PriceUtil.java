package utils;

import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.utilities.Logger;

/**
 * Utility class for price operations
 */
public class PriceUtil {

    /**
     * Gets the current price of an item from LivePrices API
     * @param itemName Name of the item
     * @param fallbackPrice Fallback price to use if API fails
     * @return Current price or fallback price
     */
    public static float getItemPrice(String itemName, float fallbackPrice) {
        try {
            float price = LivePrices.get(itemName);
            if (price > 0) {
                return price;
            } else {
                Logger.log("Could not get " + itemName + " price from LivePrices, using fallback");
                return fallbackPrice;
            }
        } catch (Exception e) {
            Logger.log("Error getting " + itemName + " price: " + e.getMessage());
            return fallbackPrice;
        }
    }

    /**
     * Gets the current price of an item with default fallback of 0
     * @param itemName Name of the item
     * @return Current price or 0 if not found
     */
    public static float getItemPrice(String itemName) {
        return getItemPrice(itemName, 0.0f);
    }
}

