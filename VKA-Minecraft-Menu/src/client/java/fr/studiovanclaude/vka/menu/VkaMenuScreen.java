package fr.studiovanclaude.vka.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Première maquette VKA validée, rendue comme fond intégral.
 * Les zones de boutons sont réelles : clic gauche, maintien, relâchement et hover.
 *
 * Les coordonnées sont exprimées dans l'image source 1672×941 et converties
 * automatiquement vers les coordonnées d'interface de Minecraft (GUI Scale).
 */
public final class VkaMenuScreen extends Screen {
    private static final int IMAGE_W = 1672;
    private static final int IMAGE_H = 941;
    private static final String ADDRESS = "91.197.6.162:24508";
    private static final Identifier ART =
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_menu.png");

    private static final int[][] BUTTONS = {
        {450, 465, 748, 87},  // Rejoindre VKA
        {450, 574, 748, 83},  // Options et touches
        {450, 682, 748, 88}   // Quitter le jeu
    };
    private static boolean joinedFromVka;
    private int pressed = -1;

    public VkaMenuScreen() { super(Component.literal("VKA Serveur")); }
    public static boolean hasJoinedFromVka() { return joinedFromVka; }

    private float scale() {
        return Math.min((float)this.width / IMAGE_W, (float)this.height / IMAGE_H);
    }
    private int offsetX() { return (this.width - Math.round(IMAGE_W*scale())) / 2; }
    private int offsetY() { return (this.height - Math.round(IMAGE_H*scale())) / 2; }
    private int px(float coordinate) { return Math.round(coordinate*scale()); }

    private int buttonAt(double x, double y) {
        final float scale = scale();
        if (scale <= 0) return -1;
        final double imageX = (x - offsetX()) / scale;
        final double imageY = (y - offsetY()) / scale;
        for (int i = 0; i < BUTTONS.length; i++) {
            final int[] b = BUTTONS[i];
            if (imageX >= b[0] && imageX < b[0]+b[2]
                && imageY >= b[1] && imageY < b[1]+b[3]) return i;
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Ne pas couvrir notre illustration avec un dégradé opaque Minecraft.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xFF030B09);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ART, offsetX(), offsetY(),
            0.0f, 0.0f, px(IMAGE_W), px(IMAGE_H), IMAGE_W, IMAGE_H, IMAGE_W, IMAGE_H);

        final int hovered = buttonAt(mouseX, mouseY);
        if (hovered >= 0) {
            final int[] b = BUTTONS[hovered];
            final int x = offsetX()+px(b[0]);
            final int y = offsetY()+px(b[1]);
            final int w = px(b[2]);
            final int h = px(b[3]);
            final int inset = Math.max(2, px(5));
            final boolean down = pressed == hovered;
            final int left = x+inset, top = y+inset, right = x+w-inset, bottom = y+h-inset;

            // Relief 3D dynamique : illuminé au survol, plus sombre et enfoncé au clic.
            graphics.fill(left, top, right, bottom, down ? 0x7855CC12 : 0x2255FF12);
            graphics.fill(left, top, right, top+Math.max(2,px(down?2:5)),
                down ? 0x3B071807 : 0x88DDFF88);
            graphics.fill(left, bottom-Math.max(2,px(down?5:3)), right, bottom,
                down ? 0xCC071807 : 0xAA163816);
            graphics.fill(left, top, left+Math.max(1,px(2)), bottom,
                down ? 0xAA2F6E19 : 0xFFB3FF33);
            graphics.fill(right-Math.max(1,px(2)), top, right, bottom,
                down ? 0xAA2F6E19 : 0xFFB3FF33);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Minecraft 26.3 utilise SDL3 : le bouton GAUCHE a l'index 1 (et non 0/GLFW).
        if (event.buttonInfo().button()!=InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        pressed=buttonAt(event.x(),event.y());
        return pressed>=0 || super.mouseClicked(event,doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.buttonInfo().button()==InputConstants.MOUSE_BUTTON_LEFT) {
            final int destination=buttonAt(event.x(),event.y());
            final int active=pressed;
            pressed=-1;
            if (active>=0 && active==destination) {
                select(destination);
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    private void select(int action) {
        if (this.minecraft==null) return;
        if (action==0) {
            joinedFromVka=true;
            ServerData server = new ServerData("VKA Serveur",ADDRESS,ServerData.Type.OTHER);
            ConnectScreen.startConnecting(this,this.minecraft,
                ServerAddress.parseString(ADDRESS),server,false,null);
        } else if (action==1) {
            this.minecraft.gui.setScreen(new OptionsScreen(this,this.minecraft.options));
        } else if (action==2) {
            this.minecraft.stop();
        }
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}
