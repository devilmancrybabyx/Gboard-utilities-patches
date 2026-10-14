package dev.jason.gboardpatches.patches.gboard.features.recentimageslimit

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import dev.jason.gboardpatches.patches.gboard.shared.VerifiedTransformationPlan
import dev.jason.gboardpatches.patches.gboard.shared.VerifiedTransformationState
import dev.jason.gboardpatches.patches.gboard.shared.applyVerified
import dev.jason.gboardpatches.patches.gboard.shared.isOpcode
import dev.jason.gboardpatches.patches.gboard.shared.mutableClass
import dev.jason.gboardpatches.patches.shared.Constants.COMPATIBILITY_GBOARD

private const val TARGET_STORE_NAME_STRING = "recent_content_suggestion_shared"
private const val TARGET_STORE_HASH_LITERAL = 0x5f0d37d6 // 1594699734
private const val EQUALS_METHOD_NAME = "equals"
private const val STRING_CLASS = "Ljava/lang/String;"

internal val gboardRecentImagesLimitBytecodePatch = bytecodePatch(
    description = "解除 Recent stickers & GIFs 10 張上限，使所有 recent store 一律讀取 recent_content_suggestion_share_max_count flag。",
) {
    compatibleWith(COMPATIBILITY_GBOARD)

    execute {
        patchRecentImagesLimit()
    }
}

context(context: BytecodePatchContext)
private fun patchRecentImagesLimit() = with(context) {
    var foundClassType: String? = null

    classDefForEach { classDef ->
        for (method in classDef.methods) {
            if (method.name == "<init>" &&
                method.parameterTypes == listOf("Landroid/content/Context;", STRING_CLASS)
            ) {
                val instructions = method.implementation?.instructions
                if (instructions != null) {
                    for (instruction in instructions) {
                        if ((instruction as? ReferenceInstruction)?.reference?.toString() == TARGET_STORE_NAME_STRING) {
                            check(foundClassType == null) {
                                "Found multiple candidate RecentImages classes: $foundClassType and ${classDef.type}"
                            }
                            foundClassType = classDef.type
                            break
                        }
                    }
                }
            }
        }
    }

    if (foundClassType == null) {
        val fallbackClass = "Lgkk;"
        try {
            val candidate = mutableClass(fallbackClass)
            if (candidate.methods.any { it.name == "<init>" && it.parameterTypes == listOf("Landroid/content/Context;", STRING_CLASS) }) {
                foundClassType = fallbackClass
            }
        } catch (_: Throwable) {
        }
    }

    val targetClassType = checkNotNull(foundClassType) {
        "Expected exactly one RecentImages constructor referencing '$TARGET_STORE_NAME_STRING', but none was found"
    }

    val mutableTargetClass = mutableClass(targetClassType)
    val targetMethod = mutableTargetClass.methods.first {
        it.name == "<init>" && it.parameterTypes == listOf("Landroid/content/Context;", STRING_CLASS)
    }
    targetMethod.applyRecentImagesLimitTransformation()
}

internal fun MutableMethod.applyRecentImagesLimitTransformation() {
    applyVerified(
        VerifiedTransformationPlan(
            targetName = "$definingClass-><init>(Context, String)",
            classify = MutableMethod::classifyRecentImagesLimit,
            mutate = { method ->
                method.mutateRecentImagesLimit()
                method
            },
        ),
    )
}

private fun MutableMethod.classifyRecentImagesLimit(): VerifiedTransformationState {
    val implementation = implementation
        ?: error("No instructions in $definingClass->$name")
    val instructions = implementation.instructions
    val p2 = implementation.registerCount - 1

    val hasStockHashLiteral = instructions.any { instruction ->
        instruction.isOpcode("CONST") &&
            (instruction as? NarrowLiteralInstruction)?.narrowLiteral == TARGET_STORE_HASH_LITERAL
    }
    val hasStockStringRef = instructions.any { instruction ->
        (instruction.isOpcode("CONST_STRING") || instruction.isOpcode("CONST_STRING_JUMBO")) &&
            (instruction as? ReferenceInstruction)?.reference?.toString() == TARGET_STORE_NAME_STRING
    }

    if (hasStockHashLiteral && hasStockStringRef) {
        return VerifiedTransformationState.STOCK
    }

    // Check if patched: literal and string ref are replaced by move and move-object/from16
    val hashCheckPair = findHashCheckPair(instructions)
    val equalsCallIndex = findEqualsCallIndex(instructions)
    if (hashCheckPair != null && equalsCallIndex != null && equalsCallIndex > 0) {
        val (hashIndex, ifEqIndex) = hashCheckPair
        val hashInst = instructions[hashIndex]
        val ifEqInst = instructions[ifEqIndex] as? TwoRegisterInstruction
        val preEqualsInst = instructions[equalsCallIndex - 1]

        val isHashPatched = hashInst.isOpcode("MOVE") &&
            hashInst is TwoRegisterInstruction &&
            ifEqInst != null &&
            (hashInst.registerA == ifEqInst.registerA || hashInst.registerA == ifEqInst.registerB)

        val isEqualsPatched = preEqualsInst.isOpcode("MOVE_OBJECT_FROM16") &&
            preEqualsInst is TwoRegisterInstruction &&
            preEqualsInst.registerB == p2

        if (isHashPatched && isEqualsPatched && !hasStockHashLiteral && !hasStockStringRef) {
            return VerifiedTransformationState.PATCHED
        }
    }

    return VerifiedTransformationState.MALFORMED
}

private fun MutableMethod.mutateRecentImagesLimit() {
    val implementation = implementation
        ?: error("No instructions in $definingClass->$name")
    val instructions = implementation.instructions
    val p2 = implementation.registerCount - 1

    // 1. Locate and transform hash check
    val hashIndex = instructions.indexOfFirst { instruction ->
        instruction.isOpcode("CONST") &&
            (instruction as? NarrowLiteralInstruction)?.narrowLiteral == TARGET_STORE_HASH_LITERAL
    }
    check(hashIndex >= 0 && hashIndex < instructions.lastIndex) {
        "Could not find const with literal 0x${TARGET_STORE_HASH_LITERAL.toString(16)} in $definingClass->$name"
    }

    val hashConstInstruction = instructions[hashIndex] as? OneRegisterInstruction
        ?: error("Instruction at $hashIndex is not OneRegisterInstruction")
    val vB = hashConstInstruction.registerA

    val ifEqInstruction = instructions[hashIndex + 1]
    check(ifEqInstruction.isOpcode("IF_EQ") && ifEqInstruction is TwoRegisterInstruction) {
        "Expected if-eq instruction immediately following const at ${hashIndex + 1}, found ${ifEqInstruction.opcode.name}"
    }

    val vA = if (ifEqInstruction.registerA == vB) {
        ifEqInstruction.registerB
    } else if (ifEqInstruction.registerB == vB) {
        ifEqInstruction.registerA
    } else {
        error("Neither register of if-eq (${ifEqInstruction.registerA}, ${ifEqInstruction.registerB}) matches vB ($vB)")
    }

    // 2. Locate and transform equals check
    val stringIndex = instructions.indexOfFirst { instruction ->
        (instruction.isOpcode("CONST_STRING") || instruction.isOpcode("CONST_STRING_JUMBO")) &&
            (instruction as? ReferenceInstruction)?.reference?.toString() == TARGET_STORE_NAME_STRING
    }
    check(stringIndex >= 0 && stringIndex < instructions.lastIndex) {
        "Could not find const-string '$TARGET_STORE_NAME_STRING' in $definingClass->$name"
    }

    val stringInstruction = instructions[stringIndex] as? OneRegisterInstruction
        ?: error("Instruction at $stringIndex is not OneRegisterInstruction")
    val vC = stringInstruction.registerA

    // Verify equals follows stringIndex
    val nextInvoke = instructions.drop(stringIndex + 1).take(3).firstOrNull { instruction ->
        instruction.opcode.name.startsWith("INVOKE") &&
            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.name == EQUALS_METHOD_NAME
    }
    check(nextInvoke != null) {
        "Expected equals invocation shortly after const-string at $stringIndex in $definingClass->$name"
    }

    // Apply replacements
    replaceInstruction(hashIndex, "move v$vB, v$vA")
    replaceInstruction(stringIndex, "move-object/from16 v$vC, v$p2")
}

private fun findHashCheckPair(instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>): Pair<Int, Int>? {
    for (i in 0 until instructions.lastIndex) {
        if (instructions[i + 1].isOpcode("IF_EQ")) {
            val prev = instructions[i]
            if (prev.isOpcode("CONST") || prev.isOpcode("MOVE")) {
                return Pair(i, i + 1)
            }
        }
    }
    return null
}

private fun findEqualsCallIndex(instructions: List<com.android.tools.smali.dexlib2.iface.instruction.Instruction>): Int? {
    val index = instructions.indexOfFirst { instruction ->
        instruction.opcode.name.startsWith("INVOKE") &&
            ((instruction as? ReferenceInstruction)?.reference as? MethodReference)?.let {
                it.definingClass == STRING_CLASS && it.name == EQUALS_METHOD_NAME
            } == true
    }
    return if (index >= 0) index else null
}
