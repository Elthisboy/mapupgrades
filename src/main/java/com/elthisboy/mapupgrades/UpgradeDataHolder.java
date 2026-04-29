package com.elthisboy.mapupgrades;

public interface UpgradeDataHolder {
    int mapupgrades$getUpgradeLevel(UpgradeType type);
    void mapupgrades$setUpgradeLevel(UpgradeType type, int level);
    void mapupgrades$addUpgradeLevel(UpgradeType type, int amount);
    void mapupgrades$copyUpgradesFrom(UpgradeDataHolder other);
}