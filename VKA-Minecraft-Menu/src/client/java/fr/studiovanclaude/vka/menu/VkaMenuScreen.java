package fr.studiovanclaude.vka.menu;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * VKA Menu v9 : contrôles en pierre biseautée RGBA indépendants.
 *
 * 1. Le décor ne fournit AUCUN bouton, texte ou icône d'interaction.
 * 2. Chaque bouton est construit à partir de faces géométriques (pierre,
 *    biseau supérieur, tranche latérale et ombre), avec relief animé.
 * 3. Le libellé est dessiné par le moteur texte Minecraft et non récupéré
 *    dans un rectangle de capture d'écran.
 * 4. L'illustration panoramique est réellement générée sur les côtés, sans
 *    bandes répétées ni arrière-plan étiré ; le panneau est protégé au centre.
 */
public final class VkaMenuScreen extends Screen {
    private static final int DESIGN_W = 1672;
    private static final int DESIGN_H = 941;
    private static final int ART_SCALE = 2;
    private static final int PANORAMA_W = 4464;
    private static final int PANORAMA_H = 1882;
    private static final String ADDRESS = "91.197.6.162:24508";
    private static final Identifier SCENE =
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_menu_panorama.png");
    // Sprites de pierre taillée indépendants, générés en RGBA transparent.
    // Il ne s'agit PAS de rectangles extraits de l'ancienne illustration.
    private static final Identifier[] BUTTON_STATES = {
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_button_normal.png"),
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_button_hover.png"),
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_button_pressed.png")
    };
    private static final Identifier[] ICONS = {
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_icon_0.png"),
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_icon_1.png"),
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_icon_2.png")
    };

    private static final String[] CAPTIONS = {
        "Rejoindre VKA", "Options et touches", "Quitter le jeu"
    };
    // Positions dans l'illustration de référence, indépendantes des pixels de l'écran.
    private static final int[][] BUTTONS = {
        {440, 471, 770, 103},
        {440, 585, 770, 103},
        {440, 699, 770, 103}
    };

    private static boolean joinedFromVka;
    private final float[] hoverProgress = new float[3];
    private final float[] pressProgress = new float[3];
    private long lastFrameNanos;
    private int pressed = -1;
    private int lastHovered = -1;
    private boolean hoverInitialized;

    public VkaMenuScreen() { super(Component.literal("VKA Serveur")); }
    public static boolean hasJoinedFromVka() { return joinedFromVka; }

    // Mise à l'échelle pilotée par la hauteur : conserve toujours tout le logo
    // ET utilise davantage de paysage lorsque l'écran s'élargit.
    // À l'inverse du mode contain + tranches étirées, le décor est continu.
    private float scale() {
        return (float) this.height / DESIGN_H;
    }
    private int ox() { return (this.width - Math.round(DESIGN_W * scale())) / 2; }
    private int oy() { return (this.height - Math.round(DESIGN_H * scale())) / 2; }
    private int px(float value) { return Math.round(value * scale()); }

    private int buttonAt(double mouseX,double mouseY) {
        if (scale() <= 0) return -1;
        final double ix = (mouseX - ox()) / scale();
        final double iy = (mouseY - oy()) / scale();
        for (int i=0;i<BUTTONS.length;i++) {
            final int[] b = BUTTONS[i];
            if (ix >= b[0] && ix < b[0]+b[2] && iy >= b[1] && iy < b[1]+b[3]) return i;
        }
        return -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        // Pas de fond Minecraft qui couvrirait le panneau.
    }

    private void scene(GuiGraphicsExtractor g) {
        // Texture panorama 4464×1882 : image source centrale 3344×1882
        // conservée exactement, avec du paysage réellement généré à gauche
        // et à droite. On affiche un rectangle source centré dont la largeur
        // dépend du ratio de la fenêtre. AUCUN étirement d'une bande de 2 px.
        g.fill(0,0,this.width,this.height,0xFF06100C);
        final float factor=scale();
        if(factor<=0)return;
        final int requested=Math.max(1,Math.round((this.width/factor)*ART_SCALE));
        final int srcWidth=Math.min(PANORAMA_W,requested);
        final int sourceX=(PANORAMA_W-srcWidth)/2;
        g.blit(RenderPipelines.GUI_TEXTURED,SCENE,
            0,0,(float)sourceX,0.0f,this.width,this.height,
            srcWidth,PANORAMA_H,PANORAMA_W,PANORAMA_H);
    }

    private static int lerpColor(int low,int high,float p) {
        p=Math.max(0.0f,Math.min(1.0f,p));
        int r=(int)(((low>>16)&255)*(1-p)+((high>>16)&255)*p);
        int g=(int)(((low>>8)&255)*(1-p)+((high>>8)&255)*p);
        int b=(int)((low&255)*(1-p)+(high&255)*p);
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }

    private void updateMotion(int hovered) {
        long now=System.nanoTime();
        float dt=lastFrameNanos==0?0.016f:Math.min(0.06f,(now-lastFrameNanos)/1_000_000_000f);
        lastFrameNanos=now;
        for(int i=0;i<3;i++) {
            float hTarget=hovered==i?1.0f:0.0f;
            float pTarget=pressed==i?1.0f:0.0f;
            hoverProgress[i]+= (hTarget-hoverProgress[i])*Math.min(1.0f,dt*14.0f);
            pressProgress[i]+= (pTarget-pressProgress[i])*Math.min(1.0f,dt*25.0f);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta) {
        scene(g);
        final int hovered=buttonAt(mouseX,mouseY);
        if (hoverInitialized && hovered!=lastHovered && hovered>=0 && this.minecraft!=null)
            this.minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1.18F));
        lastHovered=hovered;
        hoverInitialized=true;
        updateMotion(hovered);
        for(int i=0;i<3;i++) drawButton(g,i);
    }

    // VKA v9 : véritable bouton graphique autonome, avec texture alpha
    // façonnée en biseau, relief, ombre et trois états. Aucun rectangle du
    // décor n'est recopié ; le texte et les icônes restent séparés.
    private void drawButton(GuiGraphicsExtractor g,int index) {
        final int[] b=BUTTONS[index];
        final int x=ox()+px(b[0]);
        final int baseY=oy()+px(b[1]);
        final int w=px(b[2]),h=px(b[3]);
        if(w<24 || h<22)return;

        float hover=hoverProgress[index],down=pressProgress[index];
        int lift=Math.round(-Math.max(1,px(3))*hover+Math.max(2,px(4))*down);
        int y=baseY+lift;
        Identifier art=down>0.45f?BUTTON_STATES[2]:
            (hover>0.35f?BUTTON_STATES[1]:BUTTON_STATES[0]);

        // Le PNG RGBA contient une VRAIE silhouette aux angles coupés.
        // zéros alpha aux coins : fini les bavures rectangulaires.
        // La largeur et la hauteur envoyées à blit restent toujours w,h.
        g.blit(RenderPipelines.GUI_TEXTURED,art,
            x,y,0.0f,0.0f,w,h,1540,206,1540,206);

        // Pictogramme autonome haute résolution : triangle / engrenage /
        // sortie. Sa position et celle du texte suivent le bouton 3D.
        final int iconSize=Math.max(12,Math.round(h*0.55f));
        final int iconX=x+Math.round(w*0.21f)-iconSize/2;
        final int iconY=y+(h-iconSize)/2-Math.max(1,px(3));
        g.blit(RenderPipelines.GUI_TEXTURED,ICONS[index],
            iconX,iconY,0.0f,0.0f,iconSize,iconSize,128,128,128,128);

        // Séparateur gravé dans le métal avec effet de profondeur.
        int dividerX=x+Math.round(w*0.31f);
        int dividerTop=y+Math.max(8,px(24));
        int dividerBottom=y+h-Math.max(10,px(30));
        if(dividerBottom>dividerTop){
            g.fill(dividerX,dividerTop,dividerX+Math.max(1,px(3)),
                dividerBottom,0xD00F1914);
            g.fill(dividerX+Math.max(2,px(3)),dividerTop,
                dividerX+Math.max(3,px(5)),dividerBottom,0xADCAD3C8);
        }

        // Le libellé n'est PAS peint dans la texture : rendu natif de la
        // police Minecraft, déplaçable, net et actionnable avec le bouton.
        if(this.minecraft!=null) {
            float textScale=Math.max(1.5f,Math.min(3.65f,h/22.0f));
            float centerX=x+w*0.61f;
            float centerY=y+h*0.455f;
            g.pose().pushMatrix();
            g.pose().translate(centerX,centerY);
            g.pose().scale(textScale,textScale);
            int halfText=this.font.width(CAPTIONS[index])/2;
            g.text(this.font,CAPTIONS[index],
                -halfText,-this.font.lineHeight/2,
                index==0?0xFFF3FFE8:0xFFF0F2EC,true);
            g.pose().popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
        if(event.buttonInfo().button()!=InputConstants.MOUSE_BUTTON_LEFT)
            return super.mouseClicked(event,doubleClick);
        pressed=buttonAt(event.x(),event.y());
        return pressed>=0||super.mouseClicked(event,doubleClick);
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if(event.buttonInfo().button()==InputConstants.MOUSE_BUTTON_LEFT) {
            int selected=buttonAt(event.x(),event.y()),started=pressed;
            pressed=-1;
            if(started>=0 && started==selected){select(selected);return true;}
        }
        return super.mouseReleased(event);
    }
    private void select(int action) {
        if(this.minecraft==null)return;
        if(action==0) {
            joinedFromVka=true;
            ServerData data=new ServerData("VKA Serveur",ADDRESS,ServerData.Type.OTHER);
            ConnectScreen.startConnecting(this,this.minecraft,ServerAddress.parseString(ADDRESS),
                data,false,null);
        } else if(action==1) {
            this.minecraft.gui.setScreen(new OptionsScreen(this,this.minecraft.options));
        } else if(action==2) {
            this.minecraft.stop();
        }
    }
    @Override public boolean shouldCloseOnEsc(){return false;}
}
