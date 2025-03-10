package rip.diamond.hideandseek.util;

import me.goodestenglish.api.util.Common;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import rip.diamond.hideandseek.HideAndSeek;
import rip.diamond.hideandseek.disguise.Disguise;
import rip.diamond.hideandseek.disguise.impl.MiscDisguise;
import rip.diamond.hideandseek.disguise.impl.MobDisguise;
import rip.diamond.hideandseek.enums.DisguiseTypes;
import rip.diamond.hideandseek.game.disguise.DisguiseData;
import rip.diamond.hideandseek.player.GamePlayer;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Util {

    public static final List<Integer> ANNOUNCE = Arrays.asList(600, 500, 400, 300, 240, 180, 120, 60, 30, 20, 10, 5, 4, 3, 2, 1);

    public static <T> T random(T... objects) {
        int i = new Random().nextInt(objects.length);
        return objects[i];
    }

    public static <T> T random(List<T> objects) {
        int i = new Random().nextInt(objects.size());
        return objects.get(i);
    }

    public static boolean deleteFile(File file) {
        if (file.exists()) {
            if(file.isDirectory()) {
                for(File subFile : Objects.requireNonNull(file.listFiles())) {
                    if(!deleteFile(subFile)) {
                        return false;
                    }
                }
            }

            return file.delete();
        }
        return false;
    }

    public static void copyFolder(File src, File dest) throws IOException {
        if(src.isDirectory()){
            if(!dest.exists()){
                dest.mkdir();
            }

            String files[] = src.list();

            for(String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                copyFolder(srcFile, destFile);
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            while((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    public static World loadWorld(String worldName) {
        World world = new WorldCreator(worldName).createWorld();

        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(0);
        world.setChunkForceLoaded(0, 0, true);
        world.loadChunk(0, 0);

        return world;
    }

    public static Disguise disguise(Player player) {
        GamePlayer gamePlayer = HideAndSeek.INSTANCE.getGame().getGamePlayer(player);
        DisguiseData disguiseData = gamePlayer.getDisguises();

        if (disguiseData.getType() == DisguiseTypes.BLOCK) {
            MiscDisguise disguise = new MiscDisguise(player, Material.valueOf(disguiseData.getData()));
            disguise.startDisguise();
            return disguise;
        } else if (disguiseData.getType() == DisguiseTypes.MOB) {
            MobDisguise disguise = new MobDisguise(player, disguiseData.getData());
            disguise.startDisguise();
            return disguise;
        }

        return null;
    }

}
