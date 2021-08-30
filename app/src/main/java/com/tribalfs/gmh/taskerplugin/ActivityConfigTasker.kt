package com.tribalfs.gmh.taskerplugin

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginRunner


abstract class ActivityConfigTasker<TInput : Any, TOutput : Any, TActionRunner : TaskerPluginRunner<TInput, TOutput>, THelper : TaskerPluginConfigHelper<TInput, TOutput, TActionRunner>> : Activity(), TaskerPluginConfig<TInput> {
    abstract fun getNewHelper(config: TaskerPluginConfig<TInput>): THelper
    abstract val layoutResId: Int

    private val taskerHelper by lazy { getNewHelper(this) }

    open val isConfigurable = true
    override val context: Context get() = applicationContext
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isConfigurable) {
            taskerHelper.finishForTasker()
            return
        }
        setContentView(layoutResId)
        taskerHelper.onCreate()
    }
}

abstract class ActivityConfigTaskerNoInput<TOutput : Any, TActionRunner : TaskerPluginRunner<Unit, TOutput>, THelper : TaskerPluginConfigHelper<Unit, TOutput, TActionRunner>> : ActivityConfigTasker<Unit, TOutput, TActionRunner, THelper>() {
    override fun assignFromInput(input: TaskerInput<Unit>) {}
    override val inputForTasker = TaskerInput(Unit)
    override val layoutResId = 0
    override val isConfigurable = false
}

abstract class ActivityConfigTaskerNoOutputOrInput<TActionRunner : TaskerPluginRunner<Unit, Unit>, THelper : TaskerPluginConfigHelper<Unit, Unit, TActionRunner>> : ActivityConfigTaskerNoInput<Unit, TActionRunner, THelper>()
