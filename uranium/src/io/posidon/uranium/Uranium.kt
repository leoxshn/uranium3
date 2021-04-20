package io.posidon.uranium

import io.posidon.uranium.graphics.Window
import io.posidon.uranium.nodes.Scene
import kotlin.concurrent.thread

object Uranium {

    var DEBUG = false

    fun init() {
        ////START/////////////////////////////////////
        Window.init()
        initMillis = System.currentTimeMillis()

        ////UPDATES///////////////////////////////////
        thread {
            var lastTime = System.nanoTime()
            var delta = 0.0
            while (running) {
                val now = System.nanoTime()
                delta += (now - lastTime) / 1000000000.0
                lastTime = now
                if (delta >= updateInterval) {
                    Scene.update(delta)
                    delta = 0.0
                }
            }
        }
    }

    fun destroy() {
        Scene.destroy()
    }

    private var initMillis = 0L
    fun millis(): Long = System.currentTimeMillis() - initMillis

    /**
     * The default [updateInterval] (in seconds)
     */
    const val DEFAULT_UPDATE_INTERVAL = 0.001

    /**
     * The time in seconds between each tree update
     */
    var updateInterval = DEFAULT_UPDATE_INTERVAL

    internal var running = true
        private set

    /**
     * Stops the engine
     */
    fun end() {
        running = false
    }
}