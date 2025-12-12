// SPDX-FileCopyrightText: 2025 Weibo, Inc.
//
// SPDX-License-Identifier: Apache-2.0

package com.sina.weibo.agent.util

import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Invoke a method using Java reflection.
 * This avoids Kotlin reflection's metadata parsing which is incompatible with shadowJar relocate.
 *
 * For suspend functions, the method is called with a Continuation as the last parameter.
 */
suspend fun doInvokeMethod(
    method: Method,
    args: List<Any?>,
    actor: Any
): Any? {
    val parameterTypes = method.parameterTypes
    val isSuspend = parameterTypes.isNotEmpty() &&
            Continuation::class.java.isAssignableFrom(parameterTypes.last())

    // Calculate actual parameter count (excluding Continuation for suspend functions)
    val actualParamCount = if (isSuspend) parameterTypes.size - 1 else parameterTypes.size

    // Check for special case: method expects a List as the only parameter
    val isListParameter = actualParamCount == 1 &&
            List::class.java.isAssignableFrom(parameterTypes[0])

    val processedArgs: Array<Any?> = if (isListParameter) {
        // Method signature is like: fun methodName(args: List<Any?>)
        arrayOf(args)
    } else {
        // Build processed arguments with type conversion
        val result = ArrayList<Any?>(actualParamCount)

        for (i in 0 until actualParamCount) {
            val arg = if (i < args.size) args[i] else null
            val paramClass = parameterTypes[i]

            // Handle type mismatch caused by serialization (e.g., int serialized as double)
            val convertedArg = when {
                arg is Double && (paramClass == Int::class.java || paramClass == Integer::class.java) -> {
                    arg.toInt()
                }

                arg is Double && (paramClass == Long::class.java || paramClass == java.lang.Long::class.java) -> {
                    arg.toLong()
                }

                arg is Double && (paramClass == Float::class.java || paramClass == java.lang.Float::class.java) -> {
                    arg.toFloat()
                }

                arg is Double && (paramClass == Short::class.java || paramClass == java.lang.Short::class.java) -> {
                    arg.toInt().toShort()
                }

                arg is Double && (paramClass == Byte::class.java || paramClass == java.lang.Byte::class.java) -> {
                    arg.toInt().toByte()
                }

                arg is Double && (paramClass == Boolean::class.java || paramClass == java.lang.Boolean::class.java) -> {
                    arg != 0.0
                }

                // Handle VSCode ExtensionIdentifier object that should be converted to String
                arg is Map<*, *> && paramClass == String::class.java -> {
                    arg["value"]?.toString() ?: arg.toString()
                }

                else -> arg
            }

            result.add(convertedArg)
        }

        result.toTypedArray()
    }

    return if (isSuspend) {
        // For suspend functions, use suspendCoroutineUninterceptedOrReturn to get the continuation
        suspendCoroutineUninterceptedOrReturn { continuation ->
            val argsWithContinuation = processedArgs + continuation
            val result = method.invoke(actor, *argsWithContinuation)
            result
        }
    } else {
        // For regular functions, just invoke directly
        method.invoke(actor, *processedArgs)
    }
}
