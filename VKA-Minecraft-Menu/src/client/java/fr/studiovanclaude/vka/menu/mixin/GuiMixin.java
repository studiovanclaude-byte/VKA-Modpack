package fr.studiovanclaude.vka.menu.mixin;

import fr.studiovanclaude.vka.menu.VkaMenuScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * MC 26.3 : le changement d'écran se fait dans Gui#setScreen(Screen),
 * pas dans Minecraft#setScreen (supprimé en 26.x).
 * On conserve les écrans d'erreur de déconnexion, afin d'afficher la cause,
 * et on remplace l'écran vers lequel ils renvoient ensuite.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), argsOnly = true)
    private Screen vka$replaceOldTitle(Screen target) {
        if (target == null || target instanceof VkaMenuScreen) return target;
        if (target instanceof TitleScreen) return new VkaMenuScreen();
        if (target instanceof JoinMultiplayerScreen && VkaMenuScreen.hasJoinedFromVka()) {
            return new VkaMenuScreen();
        }
        // Compatibilité avec un ancien écran VKA client inconnu, si son titre est celui du menu montré.
        if ("VKA Serveur".equals(target.getTitle().getString())) {
            return new VkaMenuScreen();
        }
        return target;
    }
}
