package com.mathewsachin.fategrandautomata.scripts.entrypoints

import com.mathewsachin.fategrandautomata.scripts.IFgoAutomataApi
import com.mathewsachin.fategrandautomata.scripts.Images
import com.mathewsachin.libautomata.*
import com.mathewsachin.libautomata.dagger.ScriptScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * CE bomb maker script with caveats.
 * This script isn't much tested. Use at your own risk. This code isn't calibrated for FGO JP.
 *
 * - Can only be started from CE enhancement screen with no CE selected.
 * - In the CE picking screens, the item sizes must be set to lowest.
 * - Base CE pickup screen should be filtered to correct rarity and sorted in Ascending order by Level.
 * - Enhancement material pickup screen should be filtered to correct rarity and sorted in Descending order by Level.
 */
@ScriptScope
class AutoCEBomb @Inject constructor(
    exitManager: ExitManager,
    api: IFgoAutomataApi
) : EntryPoint(exitManager), IFgoAutomataApi by api {
    sealed class ExitReason {
        object NoSuitableTargetCEFound : ExitReason()
    }

    //Declaration of Image Files for CE Search
    private fun imagesForSelectedRarity() = when (prefs.ceBombTargetRarity) {
        1 -> listOf(
            Images.CEStarvationLv1,
            Images.CEAwakeningLv1,
            Images.CEBarrierLv1,
            Images.CELinkageLv1,
            Images.CECombatLv1
        )
        2 -> listOf(
            Images.CEGloomLv1,
            Images.CESynchronizationLv1,
            Images.CEDeceptionLv1,
            Images.CEProsperityLv1,
            Images.CEMercyLv1
        )
        else -> emptyList()
    }

    class ExitException(val reason: ExitReason) : Exception()

    //Declaration of function for Base CE Region Selection
    private fun findBaseCE(): Match {
        //function to find the region to be clicked of Base CE.
        for (img in imagesForSelectedRarity()) {
            //for each image under selected rarity do the following
            val matches = locations.levelOneCERegion
                .findAll(images[img])
                .toList()
                .sorted()
            //find all that matches the image in previous comment into a list and sort them.

            // At least 2 copies are needed to merge
            if (matches.size > 1) {
                return matches[0]
                //return the first match if there are more than one of it as the Base CE Region.
            }
        }

        throw ExitException(ExitReason.NoSuitableTargetCEFound)
        //If Return wasn't called then throw an Exception.
    }

    override fun script(): Nothing {
        locations.ceEnhanceClick.click()

        while (true) {
            2.seconds.wait()

            val baseCERegion = findBaseCE().region
            val img = baseCERegion.getPattern()

            img.use {
                baseCERegion.click()
                2.seconds.wait()

                Location(900, 500).click()
                2.seconds.wait()

                // Picking the matching CE later allows more CE to be picked
                pickCEs()
                pickMatchingCE(img)

                repeat(2) {
                    Location(2300, 1300).click()
                    1.seconds.wait()
                }

                Location(1600, 1200).click()
                1.seconds.wait()

                Location(2000, 1000).click(70)
                locations.ceEnhanceClick.click()
            }
        }
    }

    private fun pickMatchingCE(img: Pattern) {
        // Scroll to top
        Location(2040, 400).click()
        2.seconds.wait()

        val matchingCE = locations.levelOneCERegion.find(img)
            ?: throw ExitException(ExitReason.NoSuitableTargetCEFound)

        matchingCE.region.click()
        1.seconds.wait()
    }

    private fun pickCEs() {
        // Scroll to bottom
        Location(2040, 1400).click()
        1.seconds.wait()

        for (y in 0..3) {
            for (x in 0..6) {
                Location(1900 - 270 * x, 1300 - 290 * y).click()
            }
        }
    }
}