package com.example.zhttaskflow.base.extension

/**
 * 集合类（List/Map/Set）的通用扩展，比如判空、安全取值、快速转换等
 * */

/**
 * 集合为空或 null 时执行 [action]。
 * 这是一个扩展函数，可以用于 Collection<T>? 类型的对象。
 *
 * @param T 集合中元素的类型
 * @param action 当集合为空或 null 时要执行的函数
 * @return 返回原始集合（可能是 null）
 */
inline fun <T> Collection<T>?.ifNullOrEmpty(action: () -> Unit): Collection<T>? {
    if (this == null || isEmpty()) action()
    return this
}

/**
 * 对非空集合执行 [block]。
 * 这是一个扩展函数，可以为可空的集合（Collection<T>?）添加功能。
 *
 * @param T 集合中元素的类型
 * @param R mapNotNull 操作后返回的列表元素类型
 * @param block 对集合中每个元素执行的转换函数，可以返回 null
 * @return 如果集合为 null 或空，返回空列表；否则返回经过 block 处理并过滤掉 null 值后的列表
 */
inline fun <T, R> Collection<T>?.mapNotNullOrEmpty(block: (T) -> R?): List<R> {
    // 检查集合是否为 null 或空，如果是则直接返回空列表
    if (this == null || isEmpty()) return emptyList()
    // 使用 mapNotNull 函数对集合元素进行处理，过滤掉 block 返回 null 的情况
    return mapNotNull(block)
}
