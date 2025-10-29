// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.util

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.jvmErasure

suspend fun doInvokeMethod(
    method: KFunction<*>,
    args: List<Any?>,
    actor: Any
): Any? {
    // Handle parameters
    val parameterTypes = method.parameters
    val processedArgs = ArrayList<Any?>(parameterTypes.size)
    val realArgs = listOf(actor, *args.toTypedArray())
    // Handle parameter type mismatch and nullable parameter issues
    for (i in parameterTypes.indices) {
        if (i < realArgs.size) {
            // Argument is provided, handle type conversion
            val arg = realArgs[i]
            val paramType = parameterTypes[i]
            val paramClass = paramType.type.jvmErasure

            // Handle type mismatch caused by serialization (e.g., int serialized as double)
            val convertedArg = when {
                arg is Double && paramClass == Int::class -> {
                  
                    arg.toInt()
                }

                arg is Double && paramClass == Long::class -> {
                  
                    arg.toLong()
                }

                arg is Double && paramClass == Float::class -> {
                  
                    arg.toFloat()
                }

                arg is Double && paramClass == Short::class -> {
                  
                    arg.toInt().toShort()
                }

                arg is Double && paramClass == Byte::class -> {
                   
                    arg.toInt().toByte()
                }

                arg is Double && paramClass == Boolean::class -> {
                    arg != 0.0
                }

                // Handle VSCode ExtensionIdentifier object that should be converted to String
                arg is Map<*, *> && paramClass == String::class -> {
                    val value = arg["value"]?.toString() ?: arg.toString()
                    value
                }

                else -> arg
            }

            processedArgs.add(convertedArg)
        } else {
            // Argument missing, check if it is a primitive type and set appropriate default value
            val paramType = parameterTypes[i]

            // Special handling for String type: set to empty string instead of null
            if (paramType == String::class.java) {
                processedArgs.add("")
            } else {
                processedArgs.add(null)
            }
        }
    }

    // Invoke method
    return method.callSuspend(*processedArgs.toTypedArray())
}