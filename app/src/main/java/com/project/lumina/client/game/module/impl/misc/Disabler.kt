package com.project.lumina.client 

import android.os.SystemClock 

enum class Category {
    MISC
} 

abstract class Module(val name: String, val description: String, val category: Category) {
    open fun onSendPacket(packet: Packet) {}
    open fun getModeText(): String = ""
} 

class EnumSetting(
    val name: String,
    val desc: String,
    val options: List<String>,
    var value: Int,
    val default: Int
) 

object Math {
    fun lerp(start: Float, end: Float, amount: Float): Float = start + (end - start) * amount
} 

open class Packet {
    open fun getName(): String = ""
} 

class PlayerAuthInputPacket : Packet() {
    var TicksAlive: Int = 0
    val position = Vector3()
    val mMove = Vector3()
    var mInputData: Int = 0
    var mPlayMode: Int = 0
    var mInputMode: Int = 0
} 

class MovePlayerPacket : Packet() {
    var mTick: Int = 0
    val mPos = Vector3()
    var mOnGround: Boolean = false
} 

class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) 

object InputData {
    const val StartJumping = 1 shl 0
    const val Jumping = 1 shl 1
} 

object ClientPlayMode {
    const val Screen = 1
} 

object InputModeAuth {
    const val Touch = 1
} 

object HitResultType {
    const val AIR = 0
} 

class HitResult {
    var type: Int = HitResultType.AIR
} 

object Game {
    fun getLocalPlayer(): LocalPlayer = LocalPlayer()
} 

class LocalPlayer {
    val level: Level = Level()
} 

class Level {
    fun getHitResult(): HitResult? = HitResult()
} 

class Disabler : Module("Disabler", "Disable the anticheat", Category.MISC) {
    private var Mode = 0
    private val serverSetting = EnumSetting("Server", "change mode", listOf("Lifeboat-OLD", "Lifeboat-NEW"), Mode, 0) 

    override fun getModeText(): String {
        return "Lifeboat"
    } 

    // Time management
    companion object {
        private var ms: Long = SystemClock.uptimeMillis()
        private var lastMS: Long = SystemClock.uptimeMillis()
        private var timeMS: Long = -1
        private fun getCurrentMs() = SystemClock.uptimeMillis()
        private fun getElapsedTime() = getCurrentMs() - ms
        private fun resetTime() {
            lastMS = getCurrentMs()
            timeMS = getCurrentMs()
        }
        private fun hasTimedElapsed(time: Long, reset: Boolean): Boolean {
            if (getCurrentMs() - lastMS > time) {
                if (reset) resetTime()
                return true
            }
            return false
        }
    } 

    override fun onSendPacket(packet: Packet) {
        if (Mode == 0 && (packet.getName() == "PlayerAuthInputPacket" || packet.getName() == "MovePlayerPacket")) {
            if (packet is PlayerAuthInputPacket) {
                val perc = (packet.TicksAlive % 3) / 3.0f
                val targetY = if (perc < 0.5f) 0.02f else -0.02f
                packet.position.y = Math.lerp(packet.position.y, packet.position.y + targetY, perc)
                packet.mMove.y = -(1.0f / 3.0f)
                if (packet.TicksAlive % 3 == 0) {
                    packet.mInputData = packet.mInputData or InputData.StartJumping
                }
                packet.mInputData = packet.mInputData or InputData.Jumping
            }
            if (packet is MovePlayerPacket) {
                val perc = (packet.mTick % 3) / 3.0f
                val targetY = if (perc < 0.5f) 0.02f else -0.02f
                packet.mPos.y = Math.lerp(packet.mPos.y, packet.mPos.y + targetY, perc)
                packet.mOnGround = true
            }
        }
        if (Mode == 1 && (packet.getName() == "PlayerAuthInputPacket" || packet.getName() == "MovePlayerPacket")) {
            if (packet is PlayerAuthInputPacket) {
                packet.mPlayMode = ClientPlayMode.Screen
                packet.mInputMode = InputModeAuth.Touch
                packet.TicksAlive = 0
                Game.getLocalPlayer().level.getHitResult()?.let { it.type = HitResultType.AIR }
            }
        }
    }
}