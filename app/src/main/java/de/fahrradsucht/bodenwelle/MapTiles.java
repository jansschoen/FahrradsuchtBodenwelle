package de.fahrradsucht.bodenwelle;

import android.content.Context;
import java.io.File;
import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.views.MapView;

/** Shared tile policy/cache for the ride map and the post-ride comparison. */
final class MapTiles {
    private MapTiles(){}
    static MapView create(Context activity) {
        File base = new File(activity.getNoBackupFilesDir(), "osm");
        File cache = new File(base, "tiles");
        if (!cache.isDirectory() && !cache.mkdirs()) throw new IllegalStateException("Map cache unavailable");
        IConfigurationProvider config = Configuration.getInstance();
        config.setOsmdroidBasePath(base);
        config.setOsmdroidTileCache(cache);
        config.setUserAgentValue("Bodenwelle/" + BuildConfig.VERSION_NAME + " (de.fahrradsucht.bodenwelle; https://github.com/jansschoen/FahrradsuchtBodenwelle)");
        config.setTileDownloadThreads((short) 2);
        config.setTileFileSystemCacheMaxBytes(60 * 1024 * 1024L);
        config.setTileFileSystemCacheTrimBytes(48 * 1024 * 1024L);
        MapView map = new MapView(activity);
        map.setTileSource(new XYTileSource("OSM", 0, 19, 256, ".png",
                new String[]{"https://tile.openstreetmap.org/"}, "© OpenStreetMap contributors",
                new TileSourcePolicy(2, TileSourcePolicy.FLAG_NO_BULK | TileSourcePolicy.FLAG_NO_PREVENTIVE
                        | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL)));
        return map;
    }
}
