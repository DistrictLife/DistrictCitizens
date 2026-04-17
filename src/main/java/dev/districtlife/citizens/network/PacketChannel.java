package dev.districtlife.citizens.network;

import dev.districtlife.citizens.DLCitizensPlugin;
import dev.districtlife.citizens.network.handlers.CheckNameUniqueHandler;
import dev.districtlife.citizens.network.handlers.RequestAppearanceHandler;
import dev.districtlife.citizens.network.handlers.SubmitCharacterHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * Canal réseau "districtlife:main" côté serveur (Arclight / DLCitizens).
 *
 * Sur Arclight, le mod dlclient (chargé au démarrage Forge) enregistre déjà ce
 * canal avant que le plugin DLCitizens ne démarre (onEnable). Forge interdit la
 * double inscription et lance IllegalStateException.
 *
 * Solution : on tente d'inscrire notre propre SimpleChannel ; si Forge refuse,
 * on récupère celui de dlclient via reflection et on y ajoute nos handlers.
 *
 * Noms de classes Minecraft : mappings MCP (Arclight 1.16.5)
 *   ResourceLocation → net.minecraft.util.ResourceLocation
 *   FriendlyByteBuf  → net.minecraft.network.PacketBuffer
 *   ServerPlayer     → net.minecraft.entity.player.ServerPlayerEntity
 */
public class PacketChannel {

    public static final String CHANNEL_ID = "districtlife:main";
    public static final String PROTOCOL_VERSION = "1.0.0";

    private static SimpleChannel channel;
    private static DLCitizensPlugin plugin;

    // ─── IDs S2C ────────────────────────────────────────────────────────────
    public static final int ID_OPEN_CHARACTER_CREATION  = 0;
    public static final int ID_NAME_CHECK_RESPONSE      = 1;
    public static final int ID_CHARACTER_CREATED        = 2;
    public static final int ID_CHARACTER_CREATION_FAILED = 3;
    public static final int ID_APPEARANCE_SYNC          = 4;
    public static final int ID_OPEN_ID_CARD             = 5;
    public static final int ID_APPEARANCE_CONFIG        = 6;

    // ─── IDs C2S ────────────────────────────────────────────────────────────
    public static final int ID_CHECK_NAME_UNIQUE  = 10;
    public static final int ID_SUBMIT_CHARACTER   = 11;
    public static final int ID_REQUEST_APPEARANCE = 12;

    // ─── Wrappers S2C (classes distinctes pour éviter Duplicate message type) ─
    public static final class S0 { final Consumer<PacketBuffer> w; public S0(Consumer<PacketBuffer> w){this.w=w;} }
    public static final class S1 { final Consumer<PacketBuffer> w; public S1(Consumer<PacketBuffer> w){this.w=w;} }
    public static final class S2 { final Consumer<PacketBuffer> w; public S2(Consumer<PacketBuffer> w){this.w=w;} }
    public static final class S3 { final Consumer<PacketBuffer> w; public S3(Consumer<PacketBuffer> w){this.w=w;} }
    public static final class S4 { final Consumer<PacketBuffer> w; public S4(Consumer<PacketBuffer> w){this.w=w;} }
    public static final class S5 { final Consumer<PacketBuffer> w; public S5(Consumer<PacketBuffer> w){this.w=w;} }
    public static final class S6 { final Consumer<PacketBuffer> w; public S6(Consumer<PacketBuffer> w){this.w=w;} }

    // ─── Wrappers C2S (classes distinctes pour éviter Duplicate message type) ─
    public static final class C10 { public final PacketBuffer buf; public C10(PacketBuffer b){buf=b;} }
    public static final class C11 { public final PacketBuffer buf; public C11(PacketBuffer b){buf=b;} }
    public static final class C12 { public final PacketBuffer buf; public C12(PacketBuffer b){buf=b;} }

    // ────────────────────────────────────────────────────────────────────────

    public PacketChannel(DLCitizensPlugin plugin) {
        PacketChannel.plugin = plugin;
        init();
    }

    private void init() {
        // Sur Arclight, dlclient (mod Forge) a déjà inscrit ce canal.
        // On tente d'abord notre propre inscription ; en cas de refus on récupère le canal existant.
        try {
            channel = NetworkRegistry.newSimpleChannel(
                new net.minecraft.util.ResourceLocation(CHANNEL_ID),
                () -> PROTOCOL_VERSION,
                v -> PROTOCOL_VERSION.equals(v) || "ABSENT".equals(v),
                v -> PROTOCOL_VERSION.equals(v) || "ABSENT".equals(v)
            );
            plugin.getLogger().info("Canal districtlife:main inscrit par DLCitizens.");
        } catch (Exception alreadyRegistered) {
            // Canal déjà inscrit par dlclient — on le récupère via reflection
            plugin.getLogger().info("Canal districtlife:main déjà inscrit par dlclient, récupération...");
            channel = findDlclientChannel();
            if (channel == null) {
                plugin.getLogger().severe(
                    "Impossible de récupérer le canal districtlife:main ! "
                    + "Les packets ne seront pas envoyés.");
                return;
            }
            plugin.getLogger().info("Canal districtlife:main récupéré depuis dlclient.");
        }

        // ── S2C — API messageBuilder (Forge 36.2.x — registerMessage n'existe pas) ─
        channel.<S0>messageBuilder(S0.class, ID_OPEN_CHARACTER_CREATION)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S0(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        channel.<S1>messageBuilder(S1.class, ID_NAME_CHECK_RESPONSE)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S1(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        channel.<S2>messageBuilder(S2.class, ID_CHARACTER_CREATED)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S2(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        channel.<S3>messageBuilder(S3.class, ID_CHARACTER_CREATION_FAILED)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S3(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        channel.<S4>messageBuilder(S4.class, ID_APPEARANCE_SYNC)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S4(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        channel.<S5>messageBuilder(S5.class, ID_OPEN_ID_CARD)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S5(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        channel.<S6>messageBuilder(S6.class, ID_APPEARANCE_CONFIG)
            .encoder((msg, out) -> msg.w.accept(out))
            .decoder(buf -> new S6(b -> {}))
            .consumer((msg, ctx) -> ctx.get().setPacketHandled(true))
            .add();

        // ── C2S — handlers de réception ──────────────────────────────────────
        // On n'appelle AUCUNE méthode Forge (enqueueWork/getSender) directement depuis
        // DLCitizens car les signatures SRG au runtime diffèrent du stub MCP.
        // → setPacketHandled et getSender via réflexion, puis Bukkit scheduler.
        // IMPORTANT : lire le ByteBuf IMMÉDIATEMENT dans le consumer (thread Netty).
        // Forge libère le buffer dès le retour du consumer → ne jamais différer la lecture.
        channel.<C10>messageBuilder(C10.class, ID_CHECK_NAME_UNIQUE)
            .encoder((msg, out) -> {})
            .decoder(buf -> new C10(buf))
            .consumer((msg, ctx) -> {
                String fn = BufUtil.readString(msg.buf, 32);
                String ln = BufUtil.readString(msg.buf, 32);
                dispatchC2S(ctx, player ->
                    new CheckNameUniqueHandler(plugin).handle(fn, ln, player));
            })
            .add();

        channel.<C11>messageBuilder(C11.class, ID_SUBMIT_CHARACTER)
            .encoder((msg, out) -> {})
            .decoder(buf -> new C11(buf))
            .consumer((msg, ctx) -> {
                String fn   = BufUtil.readString(msg.buf, 32);
                String ln   = BufUtil.readString(msg.buf, 32);
                String bd   = BufUtil.readString(msg.buf, 32);
                int st = msg.buf.readInt();
                int ec = msg.buf.readInt();
                int hs = msg.buf.readInt();
                int hc = msg.buf.readInt();
                dispatchC2S(ctx, player ->
                    new SubmitCharacterHandler(plugin).handle(fn, ln, bd, st, ec, hs, hc, player));
            })
            .add();

        channel.<C12>messageBuilder(C12.class, ID_REQUEST_APPEARANCE)
            .encoder((msg, out) -> {})
            .decoder(buf -> new C12(buf))
            .consumer((msg, ctx) -> {
                java.util.UUID targetUuid = BufUtil.readUUID(msg.buf);
                dispatchC2S(ctx, player ->
                    new RequestAppearanceHandler(plugin).handle(targetUuid, player));
            })
            .add();

        plugin.getLogger().info("Handlers du canal districtlife:main enregistrés.");
    }

    /**
     * Dispatche un paquet C2S reçu sur le thread Bukkit principal.
     * Utilise la réflexion pour appeler setPacketHandled et getSender afin
     * d'éviter tout problème de signature SRG/MCP au runtime Arclight.
     */
    private static void dispatchC2S(Supplier<NetworkEvent.Context> ctx, Consumer<Player> handler) {
        Object rawCtx = ctx.get();

        // setPacketHandled(true) via réflexion — évite toute désynchronisation de signature
        try {
            rawCtx.getClass().getMethod("setPacketHandled", boolean.class).invoke(rawCtx, true);
        } catch (Exception e) {
            if (plugin != null) plugin.getLogger().warning("setPacketHandled réflexion: " + e);
        }

        // getSender() via réflexion — retourne le ServerPlayerEntity (NMS)
        Object rawSender = null;
        try {
            rawSender = rawCtx.getClass().getMethod("getSender").invoke(rawCtx);
        } catch (Exception e) {
            if (plugin != null) plugin.getLogger().warning("getSender réflexion: " + e);
            return;
        }
        if (rawSender == null) {
            if (plugin != null) plugin.getLogger().warning("dispatchC2S: getSender() a retourné null");
            return;
        }

        // Résoudre le Player Bukkit depuis le NMS ServerPlayerEntity.
        // Dans Arclight, le NMS n'implémente PAS directement Player → passer par getBukkitEntity().
        Player player = null;

        if (rawSender instanceof Player) {
            // Au cas où Arclight implémente directement l'interface
            player = (Player) rawSender;
        } else {
            // Approche CraftBukkit/Arclight standard : entity.getBukkitEntity()
            try {
                Object bukkit = rawSender.getClass().getMethod("getBukkitEntity").invoke(rawSender);
                if (bukkit instanceof Player) player = (Player) bukkit;
            } catch (Exception ignored) {}
        }

        if (player == null) {
            if (plugin != null) plugin.getLogger().warning(
                "dispatchC2S: impossible de résoudre Player Bukkit depuis "
                + rawSender.getClass().getName()
                + " — cherche par UUID...");
            // Dernier recours : chercher via getStringUUID() → Bukkit.getPlayer(uuid)
            try {
                Object uuidStr = rawSender.getClass().getMethod("getStringUUID").invoke(rawSender);
                if (uuidStr instanceof String) {
                    player = plugin.getServer().getPlayer(java.util.UUID.fromString((String) uuidStr));
                }
            } catch (Exception ignored) {}
        }

        if (player == null) {
            if (plugin != null) plugin.getLogger().severe("dispatchC2S: Player Bukkit introuvable, paquet ignoré.");
            return;
        }

        // Exécuter sur le thread principal Bukkit (thread-safe pour les API Bukkit)
        final Player finalPlayer = player;
        if (plugin != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (finalPlayer.isOnline()) handler.accept(finalPlayer);
            });
        }
    }

    /**
     * Récupère le SimpleChannel enregistré par dlclient via reflection.
     */
    private static SimpleChannel findDlclientChannel() {
        try {
            // Essai via le thread context classloader (accès aux classes Forge mod)
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> clientPcClass = Class.forName("dev.districtlife.client.network.PacketChannel", true, cl);
            Method getChannel = clientPcClass.getMethod("getChannel");
            Object ch = getChannel.invoke(null);
            if (ch instanceof SimpleChannel) {
                return (SimpleChannel) ch;
            }
            plugin.getLogger().severe("getChannel() n'a pas retourné un SimpleChannel : "
                + (ch != null ? ch.getClass().getName() : "null"));
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Reflection sur dlclient.PacketChannel échouée : " + e);
            return null;
        }
    }

    public static SimpleChannel getChannel() {
        return channel;
    }

    /**
     * Envoie un packet S2C au joueur via réflexion totale (évite tout cast Minecraft/Forge).
     */
    public static void sendToPlayer(Player player, int packetId, Consumer<PacketBuffer> writer) {
        if (channel == null) {
            if (plugin != null) plugin.getLogger().warning(
                "sendToPlayer ignoré (canal null) pour " + player.getName() + " packet=" + packetId);
            return;
        }
        if (plugin != null) plugin.getLogger().info(
            "sendToPlayer: envoi packet=" + packetId + " à " + player.getName());
        try {
            Object msg = buildWrapper(packetId, writer);

            // Récupère le ServerPlayerEntity NMS sans cast (classloader Arclight)
            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);

            // PacketDistributor.PLAYER depuis le classloader du NMS player (game classloader)
            ClassLoader gameCl = nmsPlayer.getClass().getClassLoader();
            Class<?> pdClass = Class.forName(
                "net.minecraftforge.fml.network.PacketDistributor", true, gameCl);

            Object playerDistributor = null;
            for (java.lang.reflect.Field f : pdClass.getFields()) {
                if ("PLAYER".equals(f.getName())) {
                    playerDistributor = f.get(null);
                    break;
                }
            }
            if (playerDistributor == null) throw new Exception("PacketDistributor.PLAYER introuvable");

            // PacketDistributor.with(Supplier<?>) → PacketTarget
            java.lang.reflect.Method withMethod = null;
            for (java.lang.reflect.Method m : playerDistributor.getClass().getMethods()) {
                if ("with".equals(m.getName()) && m.getParameterCount() == 1) {
                    withMethod = m;
                    break;
                }
            }
            if (withMethod == null) throw new Exception("PacketDistributor.with() introuvable");
            final Object finalNms = nmsPlayer;
            Object target = withMethod.invoke(playerDistributor,
                (java.util.function.Supplier<?>) () -> finalNms);

            // SimpleChannel.send(PacketTarget, MSG) via réflexion
            java.lang.reflect.Method sendMethod = null;
            for (java.lang.reflect.Method m : channel.getClass().getMethods()) {
                if ("send".equals(m.getName()) && m.getParameterCount() == 2) {
                    sendMethod = m;
                    break;
                }
            }
            if (sendMethod == null) throw new Exception("SimpleChannel.send() introuvable");
            sendMethod.invoke(channel, target, msg);

            if (plugin != null) plugin.getLogger().info(
                "sendToPlayer: packet=" + packetId + " envoyé à " + player.getName());
        } catch (Exception e) {
            if (plugin != null) plugin.getLogger().severe(
                "sendToPlayer échoué pour " + player.getName()
                + " (packet " + packetId + ") : " + e);
        }
    }

    private static Object buildWrapper(int packetId, Consumer<PacketBuffer> writer) {
        switch (packetId) {
            case ID_OPEN_CHARACTER_CREATION:   return new S0(writer);
            case ID_NAME_CHECK_RESPONSE:       return new S1(writer);
            case ID_CHARACTER_CREATED:         return new S2(writer);
            case ID_CHARACTER_CREATION_FAILED: return new S3(writer);
            case ID_APPEARANCE_SYNC:           return new S4(writer);
            case ID_OPEN_ID_CARD:              return new S5(writer);
            case ID_APPEARANCE_CONFIG:         return new S6(writer);
            default:
                throw new IllegalArgumentException("Unknown S2C packet ID: " + packetId);
        }
    }
}
