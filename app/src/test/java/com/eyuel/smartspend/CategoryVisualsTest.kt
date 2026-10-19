package com.eyuel.smartspend

import com.eyuel.smartspend.ui.CategoryVisuals
import org.junit.Assert.*
import org.junit.Test

class CategoryVisualsTest {

    @Test
    fun testSelectableColorsAndIconsNotEmpty() {
        assertTrue(CategoryVisuals.selectableColors.isNotEmpty())
        assertTrue(CategoryVisuals.selectableIcons.isNotEmpty())
    }

    @Test
    fun testRegisterCustomCategoryDynamically() {
        val customName = "Gaming & Esports"
        val iconKey = "SportsEsports"
        val colorHex = 0xFFC084FC

        CategoryVisuals.registerCustomCategory(customName, iconKey, colorHex)
        val style = CategoryVisuals.getStyle(customName)

        assertNotNull(style)
        assertEquals(androidx.compose.ui.graphics.Color(colorHex), style.color)
        assertEquals(CategoryVisuals.getIconByKey(iconKey), style.icon)
    }

    @Test
    fun testUnregisterCustomCategory() {
        val tempCat = "Temporary Subscription"
        CategoryVisuals.registerCustomCategory(tempCat, "Book", 0xFF38BDF8)
        CategoryVisuals.unregisterCustomCategory(tempCat)

        // After unregistering, it should fallback to default / Other style
        val style = CategoryVisuals.getStyle(tempCat)
        assertNotNull(style)
    }

    @Test
    fun testCaseInsensitiveCustomCategoryLookup() {
        val cat = "Pet Health"
        CategoryVisuals.registerCustomCategory(cat, "Pets", 0xFFFB923C)

        val styleUpper = CategoryVisuals.getStyle("PET HEALTH")
        val styleLower = CategoryVisuals.getStyle("pet health")

        assertEquals(styleUpper.icon, styleLower.icon)
    }
}
