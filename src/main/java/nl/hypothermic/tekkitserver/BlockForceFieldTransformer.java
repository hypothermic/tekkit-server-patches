package nl.hypothermic.tekkitserver;

import nl.hypothermic.htf.api.MethodTransformer;
import nl.hypothermic.htf.utils.IfStatementBuilder;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * This transformer modifies "void a(World var1, int var2, int var3, int var4, Entity var5)"
 *
 * Original instructions:
 *
 * ```java
 * if (!var1.isStatic && isZapper(var1.getData(var2, var3, var4)) && var5 instanceof EntityLiving) {
 *     var5.damageEntity(DamageSource.GENERIC, 5);
 * }
 * ```
 *
 * New instructions:
 *
 * ```java
 * if (!var1.isStatic && isZapper(var1.getData(var2, var3, var4)) && var5 instanceof EntityLiving) {
 *     EntityPlayer fake = new EntityPlayer(((CraftServer)Bukkit.getServer()).getServer(), var1, "[MFFS]", new ItemInWorldManager(var1));
 *     EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)CraftEventFactory.callEntityDamageEvent(fake, var5, DamageCause.CUSTOM, 5);
 *     if (!event.isCancelled()) {
 *         var5.damageEntity(DamageSource.GENERIC, event.getDamage());
 *         return;
 *     } else {
 *         return;
 *     }
 * }
 * ```
 */
@MethodTransformer(
		targetClass = "mffs/BlockForceField",
		targetMethodName = "a",
		targetMethodDescription = "(Lnet/minecraft/server/World;IIILnet/minecraft/server/Entity;)V"
)
public class BlockForceFieldTransformer extends MethodNode {

	public BlockForceFieldTransformer(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
		super(ASM4, access, name, desc, signature, exceptions);
		this.mv = mv;
	}

	@Override
	public void visitCode() {
		visitWorldIsStatic(mv);
	}

	private void visitWorldIsStatic(MethodVisitor methodVisitor) {
		IfStatementBuilder worldIsStatic = IfStatementBuilder.newBuilder();

		// ---
		// if (world is not static) {
		//     visitIsZapper(...)
		// }

		worldIsStatic.onLoad(loadMethodVisitor -> {
			// par1: world
			loadMethodVisitor.visitVarInsn(ALOAD, 1);

			// is world static?
			loadMethodVisitor.visitFieldInsn(
					GETFIELD,
					"net/minecraft/server/World",
					"isStatic",
					"Z"
			);
		});
		worldIsStatic.setOpcode(IFNE);

		worldIsStatic.onTrue(this::visitIsZapper);
		worldIsStatic.onFalse(unused -> {});

		worldIsStatic.insert(methodVisitor);
	}

	private void visitIsZapper(MethodVisitor methodVisitor) {
		IfStatementBuilder isZapper = IfStatementBuilder.newBuilder();

		// ---
		// if (block at xyz is connected to a projector with Zapper upgrade) {
		//     visitInstanceofLiving()
		// }

		isZapper.onLoad(loadMethodVisitor -> {
			loadMethodVisitor.visitVarInsn(ALOAD, 1);

			// par1: x coord
			loadMethodVisitor.visitVarInsn(ILOAD, 2);

			// par2: y coord
			loadMethodVisitor.visitVarInsn(ILOAD, 3);

			// par3: z coord
			loadMethodVisitor.visitVarInsn(ILOAD, 4);

			// retrieve block meta value
			loadMethodVisitor.visitMethodInsn(
					INVOKEVIRTUAL,
					"net/minecraft/server/World",
					"getData",
					"(III)I"
			);

			// check if block at this xyz is a zapper
			loadMethodVisitor.visitMethodInsn(
					INVOKESTATIC,
					"mffs/BlockForceField",
					"isZapper",
					"(I)Z"
			);
		});
		isZapper.setOpcode(IFEQ);

		isZapper.onTrue(this::visitInstanceofLiving);
		isZapper.onFalse(unused -> {});

		isZapper.insert(methodVisitor);
	}

	private void visitInstanceofLiving(MethodVisitor methodVisitor) {
		IfStatementBuilder instanceofLiving = IfStatementBuilder.newBuilder();

		// ---
		// if (entity instanceof nms/EntityLiving) {
		//      apply damage
		// }

		instanceofLiving.onLoad(loadMethodVisitor -> {
			loadMethodVisitor.visitVarInsn(ALOAD, 5);
			loadMethodVisitor.visitTypeInsn(
					INSTANCEOF,
					"net/minecraft/server/EntityLiving"
			);
		});
		instanceofLiving.setOpcode(IFEQ);

		instanceofLiving.onTrue(this::visitApplyEntityDamage);
		instanceofLiving.onFalse(unused -> {});

		instanceofLiving.insert(methodVisitor);
	}

	private void visitApplyEntityDamage(MethodVisitor methodVisitor) {

		// --- EntityPlayer fake = new EntityPlayer(((CraftServer)Bukkit.getServer()).getServer(), var1, "[MFFS]", new ItemInWorldManager(var1));

		methodVisitor.visitTypeInsn(NEW, "net/minecraft/server/EntityPlayer");
		methodVisitor.visitInsn(DUP);

		// par1: minecraft server instance
		methodVisitor.visitMethodInsn(
				INVOKESTATIC,
				"org/bukkit/Bukkit",
				"getServer",
				"()Lorg/bukkit/Server;"
		);
		methodVisitor.visitTypeInsn(CHECKCAST, "org/bukkit/craftbukkit/CraftServer");
		methodVisitor.visitMethodInsn(
				INVOKEVIRTUAL,
				"org/bukkit/craftbukkit/CraftServer",
				"getServer",
				"()Lnet/minecraft/server/MinecraftServer;"
		);

		// par2: world
		methodVisitor.visitVarInsn(ALOAD, 1);

		// par3: fakeplayer name
		methodVisitor.visitLdcInsn("[MFFS]");

		// par4: iteminworldmanager
		methodVisitor.visitTypeInsn(NEW, "net/minecraft/server/ItemInWorldManager");
		methodVisitor.visitInsn(DUP);
		methodVisitor.visitVarInsn(ALOAD, 1);
		methodVisitor.visitMethodInsn(
				INVOKESPECIAL,
				"net/minecraft/server/ItemInWorldManager",
				"<init>",
				"(Lnet/minecraft/server/World;)V"
		);

		// create the entityplayer and store in var6
		methodVisitor.visitMethodInsn(
				INVOKESPECIAL,
				"net/minecraft/server/EntityPlayer",
				"<init>",
				"(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/World;Ljava/lang/String;Lnet/minecraft/server/ItemInWorldManager;)V"
		);
		methodVisitor.visitVarInsn(ASTORE, 6);

		// ---
		// EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)CraftEventFactory.callEntityDamageEvent(fake, var5, DamageCause.CUSTOM, 5);

		// par1: fake entityplayer
		methodVisitor.visitVarInsn(ALOAD, 6);
		// par2: target entity (param)
		methodVisitor.visitVarInsn(ALOAD, 5);
		// par3: damagecause CUSTOM
		methodVisitor.visitFieldInsn(
				GETSTATIC,
				"org/bukkit/event/entity/EntityDamageEvent$DamageCause",
				"CUSTOM",
				"Lorg/bukkit/event/entity/EntityDamageEvent$DamageCause;"
		);
		// par4: initial damage amount
		methodVisitor.visitInsn(ICONST_5);

		// create damage event
		methodVisitor.visitMethodInsn(
				INVOKESTATIC,
				"org/bukkit/craftbukkit/event/CraftEventFactory",
				"callEntityDamageEvent",
				"(Lnet/minecraft/server/Entity;Lnet/minecraft/server/Entity;Lorg/bukkit/event/entity/EntityDamageEvent$DamageCause;I)Lorg/bukkit/event/entity/EntityDamageEvent;"
		);

		// cast it to entitydamagebyentityevent
		methodVisitor.visitTypeInsn(CHECKCAST, "org/bukkit/event/entity/EntityDamageByEntityEvent");

		// store it in reg 7
		methodVisitor.visitVarInsn(ASTORE, 7);

		// ---
		// RET before exiting IF statement to save a frame reset:
		// if (!event.isCancelled()) {
		//     var5.damageEntity(DamageSource.GENERIC, event.getDamage());
		//     return;
		// } else {
		//     return;
		// }

		IfStatementBuilder ifIsCancelled = IfStatementBuilder.newBuilder();

		ifIsCancelled.onLoad(loadMethodVisitor -> {
			// ifne par1: bool whether event is cancelled
			loadMethodVisitor.visitVarInsn(ALOAD, 7);
			loadMethodVisitor.visitMethodInsn(
					INVOKEVIRTUAL,
					"org/bukkit/event/entity/EntityDamageByEntityEvent",
					"isCancelled",
					"()Z"
			);
		});
		ifIsCancelled.setOpcode(IFNE);

		ifIsCancelled.onTrue(trueMethodVisitor -> {
			// par1: damage source
			trueMethodVisitor.visitVarInsn(ALOAD, 5);
			trueMethodVisitor.visitFieldInsn(
					GETSTATIC,
					"net/minecraft/server/DamageSource",
					"GENERIC",
					"Lnet/minecraft/server/DamageSource;"
			);

			// par2: damage amount
			trueMethodVisitor.visitVarInsn(ALOAD, 7);
			trueMethodVisitor.visitMethodInsn(
					INVOKEVIRTUAL,
					"org/bukkit/event/entity/EntityDamageByEntityEvent",
					"getDamage",
					"()I"
			);

			// call damage entity
			trueMethodVisitor.visitMethodInsn(
					INVOKEVIRTUAL,
					"net/minecraft/server/Entity",
					"damageEntity",
					"(Lnet/minecraft/server/DamageSource;I)Z"
			);
			// ignore call result
			trueMethodVisitor.visitInsn(POP);

			// ret
			trueMethodVisitor.visitInsn(RETURN);
		});

		// instant ret
		ifIsCancelled.onFalse(falseMethodVisitor -> falseMethodVisitor.visitInsn(RETURN));
		ifIsCancelled.insert(methodVisitor);
	}

	@Override
	public void visitEnd() {
		accept(mv);
	}
}
