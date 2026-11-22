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
 * VKA Menu v5 : des VRAIS contrôles rendus à l'exécution.
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
    private static final Identifier STONE =
        Identifier.fromNamespaceAndPath("vkamenu", "textures/gui/vka_stone_face.png");

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

    // Construction procédurale d'un vrai bouton graphique, JAMAIS un crop de l'illustration.
    private void drawButton(GuiGraphicsExtractor g,int index) {
        final int[] b=BUTTONS[index];
        final int x=ox()+px(b[0]),baseY=oy()+px(b[1]),w=px(b[2]),h=px(b[3]);
        if(w<16||h<14)return;
        float hover=hoverProgress[index],down=pressProgress[index];
        int rise=Math.round(-Math.max(2,px(7))*hover+Math.max(3,px(10))*down);
        int depth=Math.max(4,px(12));
        int y=baseY+rise;
        int corner=Math.max(3,px(11));
        int bevel=Math.max(2,px(5));
        int bottom=y+h-depth;
        int side=Math.max(2,px(7));

        // Ombre portée liée à la hauteur réelle de la pierre.
        g.fill(x+corner+side,baseY+h+Math.max(1,px(2)),
            x+w-corner+side,baseY+h+Math.max(4,px(12)),0x96000100);
        g.fill(x+corner,baseY+h,x+w-corner,baseY+h+Math.max(2,px(6)),0x92000805);

        // Une tranche réellement extrudée : sombre à gauche, arête claire à droite.
        g.fill(x+corner,y+depth,x+w-corner,y+h,0xFF1D2826);
        g.fill(x+corner+bevel,y+h-depth,x+w-corner-bevel,y+h-2,0xFF151C1C);
        g.fill(x+w-side,y+depth,x+w-corner,y+h-2,0xFF414D48);
        g.fill(x+corner+bevel,y+h-Math.max(3,px(5)),x+w-corner-bevel,y+h-2,0xFF0B1311);

        // Contour pierre taillée avec angles biseautés, face réellement flottante.
        int faceBottom=y+h-depth;
        int faceTop=y+bevel;
        int edge=lerpColor(0xFF6E7973,0xFFA7ACA1,hover);
        g.fill(x+corner,y,x+w-corner,faceTop,0xFFC1C6BD); // arête supérieure
        g.fill(x+bevel,y+corner,x+w-bevel,faceBottom-corner,edge);
        g.fill(x,y+corner,x+w,faceBottom-corner,0xFF313B37);
        g.fill(x+corner,y+bevel,x+w-corner,faceBottom-bevel,0xFF36433E);

        // Variations par bandes dans la roche, sans utiliser de texture découpée.
        int inset=corner+Math.max(1,px(3));
        int cy0=y+Math.max(bevel+1,px(15));
        int cy1=faceBottom-Math.max(bevel+1,px(12));
        if(cy1>cy0) {
            g.fillGradient(x+inset,cy0,x+w-inset,cy1,
                lerpColor(0xFF626B65,0xFF777D76,hover),
                lerpColor(0xFF373E3A,0xFF474F48,hover));
            // La pierre de la FACE est une texture de matériau créée pour ce
            // contrôle, pas une capture découpée de l'ancien menu.
            // IMPORTANT : blit prend une LARGEUR, pas une abscisse droite.
            // Le précédent x+w-2*inset faisait déborder la pierre très
            // loin à droite et recouvrait le décor.
            int stoneWidth = Math.max(1, w - inset*2);
            int stoneHeight = Math.max(1, cy1 - cy0);
            g.blit(RenderPipelines.GUI_TEXTURED,STONE,
                x+inset,cy0,0.0f,0.0f,stoneWidth,stoneHeight,
                1024,192,1024,192);
            g.fill(x+inset,cy0,x+w-inset,cy0+Math.max(1,px(2)),0x554D5A4E);
            int grain=Math.max(1,px(1));
            g.fill(x+inset+px(50),cy0+px(13),
                x+w-inset-px(45),cy0+px(13)+grain,0x26383A32);
        }
        // Éclats métalliques du cadre et reflets haute lumière.
        g.fill(x+corner,y,x+w-corner,y+Math.max(1,px(2)),0xFFE1E6DB);
        g.fill(x+bevel,y+corner,x+bevel+Math.max(1,px(2)),faceBottom-corner,0xFFB8C1B2);
        g.fill(x+w-bevel-Math.max(1,px(3)),y+corner,x+w-bevel,faceBottom-corner,0xFF141D1A);
        g.fill(x+corner,faceBottom-bevel,x+w-corner,faceBottom,0xFF151F1A);

        // Axe décoratif et pictogramme indépendants (pas inclus dans une image).
        int iconX=x+Math.round(w*0.23f);
        int iconY=y+Math.round((faceBottom-y)*0.53f);
        int icon=Math.max(5,Math.min(h/5,px(26)));
        g.fill(iconX+icon+px(22),y+Math.max(5,px(19)),
            iconX+icon+px(23),faceBottom-Math.max(5,px(12)),0xFF0F1815);
        g.fill(iconX+icon+px(24),y+Math.max(5,px(19)),
            iconX+icon+px(25),faceBottom-Math.max(5,px(12)),0xFFA1A99C);
        drawIcon(g,index,iconX,iconY,icon,hover);

        // Le vrai texte est rasterisé par Minecraft, toujours net et lié au bouton.
        if(this.minecraft!=null) {
            // Lisibilité : la police était trop petite par rapport à la
            // hauteur réelle des pierres. Texte centré et taille augmentée.
            float textScale=Math.max(1.35f,Math.min(4.0f,h/18.0f));
            float centerX=x+w*0.63f;
            float centerY=y+(faceBottom-y)*0.51f;
            g.pose().pushMatrix();
            g.pose().translate(centerX,centerY);
            g.pose().scale(textScale,textScale);
            int halfText=this.font.width(CAPTIONS[index])/2;
            g.text(this.font,CAPTIONS[index],-halfText,-this.font.lineHeight/2,
                index==0?0xFFE6FFDF:0xFFFFFFFF,true);
            g.pose().popMatrix();
        }
    }

    private void drawIcon(GuiGraphicsExtractor g,int index,int cx,int cy,int size,float hover) {
        int c=index==0?0xFFB5F134:0xFFE4E7DE;
        int outline=0xFF101A15;
        int unit=Math.max(1,size/5);
        if(index==0) {
            // Triangle relief, pointe décalée à droite.
            for(int row=-size;row<=size;row+=Math.max(1,unit)) {
                int width=Math.max(1,size-Math.abs(row));
                g.fill(cx-size/3,cy+row,cx+width,cy+row+Math.max(1,unit),outline);
                g.fill(cx-size/3+unit,cy+row,cx+width-unit,cy+row+Math.max(1,unit),c);
            }
        } else if(index==1) {
            // Engrenage taillé : 8 dents + centre évidé.
            int r=size/2;
            g.fill(cx-r,cy-r,cx+r,cy+r,c);
            g.fill(cx-size,cy-unit,cx+size,cy+unit,c);
            g.fill(cx-unit,cy-size,cx+unit,cy+size,c);
            g.fill(cx-r,cy-r,cx+r,cy+r,c);
            g.fill(cx-r/2,cy-r/2,cx+r/2,cy+r/2,outline);
        } else {
            // Icône porte / sortie avec flèche autonome.
            int r=Math.max(3,size/2);
            g.fill(cx-r,cy-r,cx-r+unit,cy+r,c);
            g.fill(cx-r,cy-r,cx+r/2,cy-r+unit,c);
            g.fill(cx-r,cy+r-unit,cx+r/2,cy+r,c);
            g.fill(cx-unit,cy-unit,cx+r,cy+unit,c);
            g.fill(cx+r-unit,cy-2*unit,cx+r+unit,cy+2*unit,c);
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
