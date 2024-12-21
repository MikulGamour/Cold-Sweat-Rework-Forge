package com.momosoftworks.coldsweat.common.item;

import com.momosoftworks.coldsweat.api.temperature.modifier.WaterskinTempModifier;
import com.momosoftworks.coldsweat.api.util.Placement;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.momosoftworks.coldsweat.core.init.ModSounds;
import com.momosoftworks.coldsweat.core.network.message.ParticleBatchMessage;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;

public class FilledWaterskinItem extends Item
{
    public static final double EFFECT_RATE = 0.4;

    public FilledWaterskinItem()
    {
        super(new Properties().stacksTo(1).craftRemainder(ModItems.WATERSKIN.get())
                              .component(ModItemComponents.WATER_TEMPERATURE, 0d));

        DispenserBlock.registerBehavior(this, DISPENSE_BEHAVIOR);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entity, slot, isSelected);
        if (entity.tickCount % 5 == 0 && entity instanceof Player player)
        {
            double itemTemp = stack.get(ModItemComponents.WATER_TEMPERATURE);
            if (itemTemp != 0 && slot <= 8 || player.getOffhandItem().equals(stack))
            {
                double temp = (EFFECT_RATE / 20) * ConfigSettings.TEMP_RATE.get();
                double newTemp = CSMath.shrink(itemTemp, temp * 5);

                stack.set(ModItemComponents.WATER_TEMPERATURE, newTemp);
                Temperature.addModifier(player, new WaterskinTempModifier(temp * CSMath.sign(itemTemp)).expires(5), Temperature.Trait.CORE, Placement.Duplicates.ALLOW);
            }
        }
    }

    public static boolean performPourAction(ItemStack stack, LivingEntity entity, InteractionHand hand)
    {
        if (!(entity instanceof Player player && stack.is(ModItems.FILLED_WATERSKIN) && stack.has(ModItemComponents.WATER_TEMPERATURE))) return false;

        Level level = player.level();
        double amount = stack.get(ModItemComponents.WATER_TEMPERATURE) * (ConfigSettings.WATERSKIN_STRENGTH.get() / 50d);
        Temperature.addModifier(player, new WaterskinTempModifier(amount).expires(0), Temperature.Trait.CORE, Placement.Duplicates.ALLOW);

        // Play empty sound
        if (!player.level().isClientSide)
        {   WorldHelper.playEntitySound(ModSounds.WATERSKIN_POUR.value(), player, player.getSoundSource(), 2f, (float) ((Math.random() / 5) + 0.9));
        }

        consumeWaterskin(stack, player, hand);
        player.swing(hand);

        // spawn falling water particles
        Random rand = new Random();
        for (int i = 0; i < 6; i++)
        {
            TaskScheduler.scheduleClient(() ->
            {
                ParticleBatchMessage particleBatch = new ParticleBatchMessage(2);
                for (int p = 0; p < 10; p++)
                {
                    AABB playerBB = player.getDimensions(player.getPose()).makeBoundingBox(player.position()).inflate(0.2);
                    particleBatch.addParticle(ParticleTypes.FALLING_WATER,
                                              Mth.lerp(rand.nextFloat(), playerBB.minX, playerBB.maxX),
                                              playerBB.maxY,
                                              Mth.lerp(rand.nextFloat(), playerBB.minZ, playerBB.maxZ),
                                              0.3, 0.3, 0.3);
                }
                particleBatch.sendEntity(player);
            }, i);
        }
        player.clearFire();
        player.getCooldowns().addCooldown(ModItems.FILLED_WATERSKIN.value(), 10);
        player.getCooldowns().addCooldown(ModItems.WATERSKIN.value(), 10);

        return true;
    }

    public static void consumeWaterskin(ItemStack stack, Player player, InteractionHand usedHand)
    {
        // Create empty waterskin item
        ItemStack emptyStack = getEmpty(stack);
        // TODO: Add this back if TWT is for this version
        //emptyStack.getOrCreateTag().remove("Purity");

        // Add the item to the player's inventory
        if (player.getInventory().contains(emptyStack))
        {   player.addItem(emptyStack);
            player.setItemInHand(usedHand, ItemStack.EMPTY);
        }
        else
        {   player.setItemInHand(usedHand, emptyStack);
        }
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (player.isCrouching())
        {   return ItemUtils.startUsingInstantly(level, player, hand);
        }
        else if (performPourAction(player.getItemInHand(hand), player, hand))
        {   return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON))
        {
            boolean hasWater = state.is(Blocks.WATER_CAULDRON);
            // Fill cauldron
            int waterLevel = hasWater ? state.getValue(LayeredCauldronBlock.LEVEL) : 0;
            if (waterLevel >= 3) return InteractionResult.PASS;
            state = hasWater
                    ? state.setValue(LayeredCauldronBlock.LEVEL, waterLevel + 1)
                    : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1);
            level.setBlock(pos, state, 3);
            // Pouring sound / visuals
            if (!level.isClientSide)
            {   level.playSound(null, pos, ModSounds.WATERSKIN_FILL.value(), SoundSource.BLOCKS, 2f, (float) Math.random() / 5 + 0.9f);
                WorldHelper.spawnParticleBatch(level, ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 0.5, 0.5, 0.5, 10, 0);
            }
            // Consume waterskin
            if (player != null)
            {   consumeWaterskin(context.getItemInHand(), player, context.getHand());
                player.getCooldowns().addCooldown(ModItems.WATERSKIN.value(), 10);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {   return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {   return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity)
    {
        double amount = stack.get(ModItemComponents.WATER_TEMPERATURE) * (ConfigSettings.WATERSKIN_STRENGTH.get() / 50d);
        Temperature.addModifier(entity, new WaterskinTempModifier(amount / 100).expires(100), Temperature.Trait.CORE, Placement.Duplicates.ALLOW);
        return entity instanceof Player player && player.isCreative()
               ? stack
               : this.getCraftingRemainingItem(stack);
    }

    public static ItemStack getEmpty(ItemStack stack)
    {
        if (stack.getItem() instanceof FilledWaterskinItem)
        {
            ItemStack emptyWaterskin = new ItemStack(ModItems.WATERSKIN.value());

            // Preserve NBT (except temperature)
            emptyWaterskin.applyComponents(stack.getComponents());
            emptyWaterskin.remove(ModItemComponents.WATER_TEMPERATURE);
            return emptyWaterskin;
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag advanced)
    {
        double temp = CSMath.round(stack.get(ModItemComponents.WATER_TEMPERATURE), 2);
        if (TooltipHandler.isShiftDown())
        {
            String crouchKey = Minecraft.getInstance().options.keyShift.getKey().getDisplayName().getString();
            tooltip.add(Component.translatable("tooltip.cold_sweat.waterskin.drink", Component.literal(crouchKey).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));

            // Info tooltip for hotbar functionality
            String perSecond = Component.translatable("tooltip.cold_sweat.per_second").getString();

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.cold_sweat.hotbar").withStyle(ChatFormatting.GRAY));
            Component tempEffectText = (temp > 0  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.round(EFFECT_RATE * ConfigSettings.TEMP_RATE.get(), 2)).withStyle(TooltipHandler.HOT) :
                                        temp == 0 ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+0").withStyle(ChatFormatting.WHITE)
                                                  : Component.translatable("tooltip.cold_sweat.temperature_effect", "-" + CSMath.round(EFFECT_RATE * ConfigSettings.TEMP_RATE.get(), 2)).withStyle(TooltipHandler.COLD))
                        .append(perSecond);
            tooltip.add(tempEffectText);

            Component tempText = temp > 0  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)).withStyle(TooltipHandler.HOT) :
                                 temp == 0 ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp)).withStyle(ChatFormatting.WHITE)
                                           : Component.translatable("tooltip.cold_sweat.temperature_effect", CSMath.formatDoubleOrInt(temp)).withStyle(TooltipHandler.COLD);

            // Info tooltip for drinking/pouring functionality
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.cold_sweat.consumed").withStyle(ChatFormatting.GRAY));
            tooltip.add(tempText);
        }
        else
        {   tooltip.add(TooltipHandler.EXPAND_TOOLTIP);
        }

        // Tooltip to display temperature
        boolean celsius = ConfigSettings.CELSIUS.get();
        Style color = temp == 0 ? Style.EMPTY : (temp < 0 ? TooltipHandler.COLD : TooltipHandler.HOT);
        String tempUnits = celsius ? "C" : "F";
        temp = temp / 2 + 95;
        if (celsius) temp = Temperature.convert(temp, Temperature.Units.F, Temperature.Units.C, true);
        temp += ConfigSettings.TEMP_OFFSET.get() / 2.0;

        tooltip.add(1, Component.translatable("item.cold_sweat.waterskin.filled").withStyle(ChatFormatting.GRAY)
                       .append(" (")
                       .append(Component.literal((int) temp + " \u00B0" + tempUnits).withStyle(color))
                       .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));

        super.appendHoverText(stack, context, tooltip, advanced);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {   return slotChanged;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack)
    {   return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack)
    {
        ItemStack empty = getEmpty(itemStack);
        // TODO: Add this back if TWT is for this version
        //empty.getOrCreateTag().remove("Purity");
        return empty;
    }

    public String getDescriptionId()
    {   return Component.translatable("item.cold_sweat.waterskin").getString();
    }

    private static final DispenseItemBehavior DISPENSE_BEHAVIOR = (source, stack) ->
    {
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        Level level = source.level();
        ChunkAccess chunk = WorldHelper.getChunk(level, pos);
        double itemTemp = stack.getOrDefault(ModItemComponents.WATER_TEMPERATURE, 0d);

        if (chunk == null) return stack;

        // Play sound
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.WATERSKIN_POUR,
                SoundSource.BLOCKS, 1, (float) ((Math.random() / 5) + 0.9));

        // Spawn particles
        Random rand = new Random();
        for (int i = 0; i < 6; i++)
        {
            TaskScheduler.scheduleServer(() ->
            {
                ParticleBatchMessage particles = new ParticleBatchMessage(2);
                for (int p = 0; p < rand.nextInt(5) + 5; p++)
                {
                    particles.addParticle(ParticleTypes.FALLING_WATER,
                            new ParticleBatchMessage.ParticlePlacement(pos.getX() + rand.nextDouble(),
                                                                       pos.getY() + rand.nextDouble(),
                                                                       pos.getZ() + rand.nextDouble(), 0, 0, 0));
                }
                particles.sendWorld(level);
            }, i);
        }

        // Spawn a hitbox that falls at the same rate as the particles and gives players below the waterskin effect
        new Object()
        {
            double acceleration = 0;
            int tick = 0;
            AABB aabb = new AABB(pos).inflate(0.5);
            // Track affected players to prevent duplicate effects
            Set<Player> affectedPlayers = new HashSet<>();

            void start()
            {   NeoForge.EVENT_BUS.register(this);
            }

            @SubscribeEvent
            public void onTick(LevelTickEvent.Pre event)
            {
                if (event.getLevel().isClientSide() == level.isClientSide())
                {
                    // Temperature of waterskin weakens over time
                    double waterTemp = CSMath.blend(itemTemp, itemTemp / 5, tick, 20, 100);

                    // Move the box down at the speed of gravity
                    aabb = aabb.move(0, -acceleration, 0);

                    // If there's ground, stop
                    BlockPos pos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
                    if (WorldHelper.isFullSide(chunk.getBlockState(pos).getShape(level, pos), Direction.UP))
                    {   NeoForge.EVENT_BUS.unregister(this);
                        return;
                    }

                    // Apply the waterskin modifier to all entities in the box
                    level.getEntitiesOfClass(Player.class, aabb).forEach(player ->
                    {
                        if (!affectedPlayers.contains(player))
                        {   // Apply the effect and store the player
                            Temperature.addModifier(player, new WaterskinTempModifier(waterTemp).expires(0), Temperature.Trait.CORE, Placement.Duplicates.ALLOW);
                            affectedPlayers.add(player);
                        }
                    });

                    // Increase the speed of the box
                    acceleration += 0.0052;
                    tick++;

                    // Expire after 5 seconds
                    if (tick > 100)
                    {   NeoForge.EVENT_BUS.unregister(this);
                    }
                }
            }
        }.start();

        return getEmpty(stack);
    };
}
