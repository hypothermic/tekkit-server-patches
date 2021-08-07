package nl.hypothermic.tekkitserver;

import nl.hypothermic.htf.api.MethodTransformer;
import nl.hypothermic.htf.utils.IfStatementBuilder;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

@MethodTransformer(targetClass = "mffs/BlockForceField",
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

		worldIsStatic.onLoad(loadMethodVisitor -> {
			loadMethodVisitor.visitVarInsn(ALOAD, 1);
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

		isZapper.onLoad(loadMethodVisitor -> {
			loadMethodVisitor.visitVarInsn(ALOAD, 1);
			loadMethodVisitor.visitVarInsn(ILOAD, 2);
			loadMethodVisitor.visitVarInsn(ILOAD, 3);
			loadMethodVisitor.visitVarInsn(ILOAD, 4);

			loadMethodVisitor.visitMethodInsn(
					INVOKEVIRTUAL,
					"net/minecraft/server/World",
					"getData",
					"(III)I"
			);

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
//		methodVisitor.visitFieldInsn(
//				GETSTATIC,
//				"java/lang/System",
//				"out",
//				"Ljava/io/PrintStream;"
//		);
//		methodVisitor.visitLdcInsn("HELLO WORLD!!!!!!!!!!1");
//		methodVisitor.visitMethodInsn(
//				INVOKEVIRTUAL,
//				"java/io/PrintStream",
//				"println",
//				"(Ljava/lang/String;)V"
//		);
//
//		methodVisitor.visitInsn(RETURN);

//		------------------------------------------------------------

//		methodVisitor.visitVarInsn(ALOAD, 5);
//		methodVisitor.visitFieldInsn(
//				GETSTATIC,
//				"net/minecraft/server/DamageSource",
//				"GENERIC",
//				"Lnet/minecraft/server/DamageSource;"
//		);
//		methodVisitor.visitInsn(ICONST_5);
//		methodVisitor.visitMethodInsn(
//				INVOKEVIRTUAL,
//				"net/minecraft/server/Entity",
//				"damageEntity",
//				"(Lnet/minecraft/server/DamageSource;I)Z"
//		);
//		methodVisitor.visitInsn(POP);
//
//		methodVisitor.visitInsn(RETURN);


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

		// ---  EntityDamageByEntityEvent event = (EntityDamageByEntityEvent)CraftEventFactory.callEntityDamageEvent(fake, var5, DamageCause.CUSTOM, 5);

		methodVisitor.visitVarInsn(ALOAD, 6);
		methodVisitor.visitVarInsn(ALOAD, 5);
		methodVisitor.visitFieldInsn(
				GETSTATIC,
				"org/bukkit/event/entity/EntityDamageEvent$DamageCause",
				"CUSTOM",
				"Lorg/bukkit/event/entity/EntityDamageEvent$DamageCause;"
		);
		methodVisitor.visitInsn(ICONST_5);
		methodVisitor.visitMethodInsn(
				INVOKESTATIC,
				"org/bukkit/craftbukkit/event/CraftEventFactory",
				"callEntityDamageEvent",
				"(Lnet/minecraft/server/Entity;Lnet/minecraft/server/Entity;Lorg/bukkit/event/entity/EntityDamageEvent$DamageCause;I)Lorg/bukkit/event/entity/EntityDamageEvent;"
		);
		methodVisitor.visitTypeInsn(CHECKCAST, "org/bukkit/event/entity/EntityDamageByEntityEvent");
		methodVisitor.visitVarInsn(ASTORE, 7);

		// ---
		// if (!event.isCancelled()) {
		//     var5.damageEntity(DamageSource.GENERIC, event.getDamage());
		// } else {
		//     return;
		// }

		IfStatementBuilder ifIsCancelled = IfStatementBuilder.newBuilder();

		ifIsCancelled.onLoad(loadMethodVisitor -> {
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
//			trueMethodVisitor.visitVarInsn(ALOAD, 5);
//			trueMethodVisitor.visitFieldInsn(
//					GETSTATIC,
//					"net/minecraft/server/DamageSource",
//					"GENERIC",
//					"Lnet/minecraft/server/DamageSource;"
//			);
//			trueMethodVisitor.visitVarInsn(ALOAD, 7);
//			trueMethodVisitor.visitMethodInsn(
//					INVOKEVIRTUAL,
//					"org/bukkit/event/entity/EntityDamageByEntityEvent",
//					"getDamage",
//					"()I"
//			);

			trueMethodVisitor.visitInsn(ICONST_1);
			trueMethodVisitor.visitMethodInsn(
					INVOKEVIRTUAL,
					"net/minecraft/server/Entity",
					"damageEntity",
					"(Lnet/minecraft/server/DamageSource;I)Z"
			);
			trueMethodVisitor.visitInsn(POP);

//			trueMethodVisitor.visitFieldInsn(
//					GETSTATIC,
//					"java/lang/System",
//					"out",
//					"Ljava/io/PrintStream;"
//			);
//			trueMethodVisitor.visitInsn(SWAP);
//			trueMethodVisitor.visitMethodInsn(
//					INVOKEVIRTUAL,
//					"java/io/PrintStream",
//					"println",
//					"(Ljava/lang/String;)V"
//			);

			trueMethodVisitor.visitInsn(RETURN);
		});

		ifIsCancelled.onFalse(falseMethodVisitor -> falseMethodVisitor.visitInsn(RETURN));
		ifIsCancelled.insert(methodVisitor);
	}

	@Override
	public void visitEnd() {
		accept(mv);
	}
}
