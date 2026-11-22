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
 * Menu VKA 3D : image plein écran, éléments séparés et vraie profondeur.
 *
 * Décor et boutons sont préparés à 2x la résolution graphique d'origine
 * pour éviter de demander à Minecraft d'agrandir une miniature.
 * Le fond remplit la totalité de la fenêtre (cover) sans bandes noires,
 * et la détection de clic utilise exactement la même transformation.
 */
public final class VkaMenuScreen extends Screen {
    private static final int DESIGN_W = 1672;
    private static final int DESIGN_H = 941;
    private static final int RESOLUTION = 2;
    private static final String ADDRESS = "91.197.6.162:24508";

    private static final Identifier BACKGROUND =
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_menu_hd.png");
    private static final Identifier[] BUTTON_ART = {
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_button_0.png"),
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_button_1.png"),
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_button_2.png")
    };
    private static final int[][] BUTTONS = {
        {446, 459, 758, 104},  // Rejoindre VKA
        {446, 566, 758, 103},  // Options et touches
        {446, 676, 758, 102}   // Quitter le jeu
    };

    private static boolean joinedFromVka;
    private int pressed = -1;

    public VkaMenuScreen() { super(Component.literal("VKA Serveur")); }
    public static boolean hasJoinedFromVka() { return joinedFromVka; }

    // "Cover" : remplit bien tout l'écran, contrairement à l'ancien "contain".
    private float scale() {
        return Math.max((float)this.width / DESIGN_W, (float)this.height / DESIGN_H);
    }
    private int offsetX() { return (this.width - Math.round(DESIGN_W*scale())) / 2; }
    private int offsetY() { return (this.height - Math.round(DESIGN_H*scale())) / 2; }
    private int px(float coordinate) { return Math.round(coordinate*scale()); }

    private int buttonAt(double x, double y) {
        final float factor = scale();
        if (factor <= 0) return -1;
        final double ix = (x-offsetX())/factor;
        final double iy = (y-offsetY())/factor;
        for (int i=0;i<BUTTONS.length;i++) {
            final int[] b=BUTTONS[i];
            if (ix>=b[0] && ix<b[0]+b[2] && iy>=b[1] && iy<b[1]+b[3]) return i;
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Supprime le dégradé opaque natif, qui dégrade la qualité du visuel VKA.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0,0,this.width,this.height,0xFF07110E);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            offsetX(),offsetY(),0.0f,0.0f,
            px(DESIGN_W),px(DESIGN_H),
            DESIGN_W*RESOLUTION,DESIGN_H*RESOLUTION,
            DESIGN_W*RESOLUTION,DESIGN_H*RESOLUTION);

        final int hovered=buttonAt(mouseX,mouseY);
        for (int i=0;i<BUTTONS.length;i++) {
            draw3DButton(graphics,i,i==hovered,i==pressed);
        }
    }

    /**
     * Les boutons sont de véritables éléments 3D séparés du fond :
     * - normal : pierre relevée, ombre portée de six pixels
     * - survol : se soulève de quatre pixels, ombre plus longue
     * - clic : s'enfonce de quatre pixels, ombre rétractée
     * Aucun effet de fluorescence ni rectangle vert ajouté.
     */
    private void draw3DButton(GuiGraphicsExtractor graphics,int index,boolean hovered,boolean down) {
        final int[] b=BUTTONS[index];
        final int x=offsetX()+px(b[0]);
        final int y=offsetY()+px(b[1]);
        final int w=px(b[2]);
        final int h=px(b[3]);

        final int elevation=down?px(3):(hovered?px(11):px(6));
        final int lift=down?px(4):(hovered?-px(4):0);
        final int shadowInset=Math.max(2,px(8));
        final int shadowTop=y+lift+px(6);
        final int shadowBottom=y+h+elevation+px(3);
        graphics.fill(x+shadowInset,shadowTop,x+w-shadowInset,shadowBottom,0xB8000705);
        graphics.fill(x+shadowInset,y+h+lift,x+w-shadowInset,y+h+elevation,0xD70B0F0E);
        // Fine arête d'ombre latérale, pour renforcer la profondeur du bouton en pierre.
        graphics.fill(x+shadowInset,y+h+lift,x+shadowInset+Math.max(1,px(4)),y+h+elevation,0xC4202924);

        // Les textes et icônes font partie de la texture du bouton : aucun
        // double dessin qui rendrait la typographie floue.
        graphics.blit(RenderPipelines.GUI_TEXTURED,BUTTON_ART[index],
            x,y+lift,0.0f,0.0f,w,h,
            b[2]*RESOLUTION,b[3]*RESOLUTION,
            b[2]*RESOLUTION,b[3]*RESOLUTION);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button()!=InputConstants.MOUSE_BUTTON_LEFT)
            return super.mouseClicked(event,doubleClick);
        pressed=buttonAt(event.x(),event.y());
        return pressed>=0 || super.mouseClicked(event,doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.buttonInfo().button()==InputConstants.MOUSE_BUTTON_LEFT) {
            final int selected=buttonAt(event.x(),event.y());
            final int started=pressed;
            pressed=-1;
            if(started>=0 && started==selected) { select(selected);return true; }
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
