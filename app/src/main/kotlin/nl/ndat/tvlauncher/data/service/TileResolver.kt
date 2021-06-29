package nl.ndat.tvlauncher.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import androidx.core.content.getSystemService
import nl.ndat.tvlauncher.data.entity.Tile
import nl.ndat.tvlauncher.util.createSwitchIntent
import nl.ndat.tvlauncher.util.loadPreferredLabel

class TileResolver {
	companion object {
		private val launcherCategories = arrayOf(
			Intent.CATEGORY_LEANBACK_LAUNCHER,
			Intent.CATEGORY_LAUNCHER
		)

		const val APP_ID_PREFIX = "app:"
		const val INPUT_ID_PREFIX = "input:"
	}

	fun getApplications(context: Context): List<Tile> {
		val packageManager = context.packageManager

		return launcherCategories
			.map { category ->
				val intent = Intent(Intent.ACTION_MAIN, null).addCategory(category)
				packageManager.queryIntentActivities(intent, 0)
			}
			.flatten()
			.distinctBy { it.activityInfo.name }
			.map { resolveInfo -> createTile(packageManager, resolveInfo) }
	}

	fun getInputs(context: Context): List<Tile> {
		val tvInputManager = context.getSystemService<TvInputManager>()
		val tvInputs = tvInputManager?.tvInputList.orEmpty()

		return tvInputs.map { createTile(context, it) }
	}

	// TODO move to extensions file
	private fun ResolveInfo.createLaunchIntent(packageManager: PackageManager) =
		packageManager.getLeanbackLaunchIntentForPackage(activityInfo.packageName)
			?: packageManager.getLaunchIntentForPackage(activityInfo.packageName)

	private fun createTile(packageManager: PackageManager, resolveInfo: ResolveInfo) = Tile(
		id = APP_ID_PREFIX + resolveInfo.activityInfo.name,
		type = Tile.TileType.APPLICATION,
		hasLeanbackCategory = resolveInfo.filter?.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER) == true,
		name = resolveInfo.activityInfo.loadLabel(packageManager).toString(),
		uri = resolveInfo.createLaunchIntent(packageManager)?.toUri(0),
	)

	private fun createTile(context: Context, tvInputInfo: TvInputInfo) = Tile(
		id = INPUT_ID_PREFIX + tvInputInfo.id,
		type = Tile.TileType.INPUT,
		hasLeanbackCategory = false,
		name = tvInputInfo.loadPreferredLabel(context),
		uri = tvInputInfo.createSwitchIntent().toUri(0),
	)
}
