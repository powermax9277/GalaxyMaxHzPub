
package com.tribalfs.gmh.helpers

/*import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.*

@SuppressLint("WrongConstant", "PrivateApi")
object UtilDisplayMod  {

    fun updateVote(context: Context) {
        val displayModDirector = Class.forName("com.android.server.display.DisplayModeDirector")
        displayModDirector.getConstructor(Context::class.java, Handler::class.java).newInstance(context.applicationContext, Handler(
            Looper.getMainLooper()))
        val voteClass: Class<*> = displayModDirector.declaredClasses.first { it.name == "com.android.server.display.DisplayModeDirector\$Vote" }

        val vote =   voteClass.getMethod("forRefreshRates",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).invoke(null, 0f, 120f)
        val updateVoteLocked = displayModDirector.getMethod("updateVoteLocked", Int::class.javaPrimitiveType, voteClass::class.java)
        updateVoteLocked.isAccessible = true
        updateVoteLocked.invoke(null, 7, vote)
    }

    private fun findClass(className: String): Class<*>? {
        val classLoader = ClassLoader.getSystemClassLoader()
        return try {
            ClassUtils.getClass(classLoader, className, false)
        } catch (e: ClassNotFoundException) {
           e.printStackTrace()
            return null
        }
    }

    object ClassUtils {

        const val PACKAGE_SEPARATOR_CHAR = '.'
        const val PACKAGE_SEPARATOR = PACKAGE_SEPARATOR_CHAR.toString()
        const val INNER_CLASS_SEPARATOR_CHAR = '$'
        const val INNER_CLASS_SEPARATOR = INNER_CLASS_SEPARATOR_CHAR.toString()
        private val primitiveWrapperMap: MutableMap<Class<*>, Class<*>> = HashMap()
        private val wrapperPrimitiveMap: MutableMap<Class<*>, Class<*>> = HashMap()
        private val abbreviationMap: MutableMap<String?, String> = HashMap()
        private val reverseAbbreviationMap: MutableMap<String?, String> = HashMap()
        private fun addAbbreviation(primitive: String, abbreviation: String) {
            abbreviationMap[primitive] = abbreviation
            reverseAbbreviationMap[abbreviation] = primitive
        }

        fun getShortClassName(cls: Class<*>?): String {
            return if (cls == null) {
                ""
            } else getShortClassName(cls.name)
        }

        fun getShortClassName(className: String?): String {
            var className = className
            if (className == null) {
                return ""
            }
            if (className.length == 0) {
                return ""
            }
            val arrayPrefix = StringBuilder()

            // Handle array encoding
            if (className.startsWith("[")) {
                while (className!![0] == '[') {
                    className = className.substring(1)
                    arrayPrefix.append("[]")
                }
                // Strip Object type encoding
                if (className[0] == 'L' && className[className.length - 1] == ';') {
                    className = className.substring(1, className.length - 1)
                }
            }
            if (reverseAbbreviationMap.containsKey(className)) {
                className = reverseAbbreviationMap[className]
            }
            val lastDotIdx = className!!.lastIndexOf(PACKAGE_SEPARATOR_CHAR)
            val innerIdx = className.indexOf(
                INNER_CLASS_SEPARATOR_CHAR, if (lastDotIdx == -1) 0 else lastDotIdx + 1
            )
            var out = className.substring(lastDotIdx + 1)
            if (innerIdx != -1) {
                out = out.replace(INNER_CLASS_SEPARATOR_CHAR, PACKAGE_SEPARATOR_CHAR)
            }
            return out + arrayPrefix
        }

        fun getSimpleName(cls: Class<*>?): String {
            return if (cls == null) {
                ""
            } else cls.simpleName
        }


        fun getSimpleName(`object`: Any?, valueIfNull: String): String {
            return if (`object` == null) {
                valueIfNull
            } else getSimpleName(`object`.javaClass)
        }

        fun getPackageName(`object`: Any?, valueIfNull: String): String {
            return if (`object` == null) {
                valueIfNull
            } else getPackageName(`object`.javaClass)
        }

        fun getPackageName(cls: Class<*>?): String {
            return if (cls == null) {
                ""
            } else getPackageName(cls.name)
        }

        fun getPackageName(className: String?): String {
            var className = className
            if (className == null || className.length == 0) {
                return ""
            }

            // Strip array encoding
            while (className!![0] == '[') {
                className = className.substring(1)
            }
            // Strip Object type encoding
            if (className[0] == 'L' && className[className.length - 1] == ';') {
                className = className.substring(1)
            }
            val i = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR)
            return if (i == -1) {
                ""
            } else className.substring(0, i)
        }

        fun getAllSuperclasses(cls: Class<*>?): List<Class<*>>? {
            if (cls == null) {
                return null
            }
            val classes: MutableList<Class<*>> = ArrayList()
            var superclass = cls.superclass
            while (superclass != null) {
                classes.add(superclass)
                superclass = superclass.superclass
            }
            return classes
        }

        fun getAllInterfaces(cls: Class<*>?): List<Class<*>>? {
            if (cls == null) {
                return null
            }
            val interfacesFound = LinkedHashSet<Class<*>>()
            getAllInterfaces(cls, interfacesFound)
            return ArrayList(interfacesFound)
        }


        private fun getAllInterfaces(cls: Class<*>, interfacesFound: HashSet<Class<*>>) {
            var cls: Class<*>? = cls
            while (cls != null) {
                val interfaces = cls.interfaces
                for (i in interfaces) {
                    if (interfacesFound.add(i)) {
                        getAllInterfaces(i, interfacesFound)
                    }
                }
                cls = cls.superclass
            }
        }

        fun convertClassNamesToClasses(classNames: List<String?>?): List<Class<*>?>? {
            if (classNames == null) {
                return null
            }
            val classes: MutableList<Class<*>?> = ArrayList(classNames.size)
            for (className in classNames) {
                try {
                    classes.add(Class.forName(className))
                } catch (ex: Exception) {
                    classes.add(null)
                }
            }
            return classes
        }


        fun convertClassesToClassNames(classes: List<Class<*>?>?): List<String?>? {
            if (classes == null) {
                return null
            }
            val classNames: MutableList<String?> = ArrayList(classes.size)
            for (cls in classes) {
                if (cls == null) {
                    classNames.add(null)
                } else {
                    classNames.add(cls.name)
                }
            }
            return classNames
        }




        @Throws(ClassNotFoundException::class)
        fun getClass(
            classLoader: ClassLoader?, className: String, initialize: Boolean
        ): Class<*> {
            return try {
                val clazz: Class<*> = if (abbreviationMap.containsKey(className)) {
                    val clsName = "[" + abbreviationMap[className]
                    Class.forName(clsName, initialize, classLoader).componentType
                } else {
                    Class.forName(toCanonicalName(className), initialize, classLoader)
                }
                clazz
            } catch (ex: ClassNotFoundException) {
                // allow path separators (.) as inner class name separators
                val lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR_CHAR)
                if (lastDotIndex != -1) {
                    try {
                        return getClass(
                            classLoader, className.substring(0, lastDotIndex) +
                                    INNER_CLASS_SEPARATOR_CHAR + className.substring(lastDotIndex + 1),
                            initialize
                        )
                    } catch (ex2: ClassNotFoundException) { // NOPMD
                        // ignore exception
                    }
                }
                throw ex
            }
        }

        private fun toCanonicalName(className: String): String {
            var className: String? = className
            className = deleteWhitespace(className)
            if (className == null) {
                throw NullPointerException("className must not be null.")
            } else if (className.endsWith("[]")) {
                val classNameBuffer = StringBuilder()
                while (className!!.endsWith("[]")) {
                    className = className.substring(0, className.length - 2)
                    classNameBuffer.append("[")
                }
                val abbreviation = abbreviationMap[className]
                if (abbreviation != null) {
                    classNameBuffer.append(abbreviation)
                } else {
                    classNameBuffer.append("L").append(className).append(";")
                }
                className = classNameBuffer.toString()
            }
            return className
        }

        fun deleteWhitespace(str: String?): String? {
            if (str!!.isEmpty()) {
                return str
            }
            val sz = str.length
            val chs = CharArray(sz)
            var count = 0
            for (i in 0 until sz) {
                if (!Character.isWhitespace(str[i])) {
                    chs[count++] = str[i]
                }
            }
            return if (count == sz) {
                str
            } else String(chs, 0, count)
        }

        init {
            primitiveWrapperMap[java.lang.Boolean.TYPE] = Boolean::class.java
            primitiveWrapperMap[java.lang.Byte.TYPE] = Byte::class.java
            primitiveWrapperMap[Character.TYPE] = Char::class.java
            primitiveWrapperMap[java.lang.Short.TYPE] = Short::class.java
            primitiveWrapperMap[Integer.TYPE] = Int::class.java
            primitiveWrapperMap[java.lang.Long.TYPE] = Long::class.java
            primitiveWrapperMap[java.lang.Double.TYPE] = Double::class.java
            primitiveWrapperMap[java.lang.Float.TYPE] = Float::class.java
            primitiveWrapperMap[Void.TYPE] = Void.TYPE
        }


        init {
            addAbbreviation("int", "I")
            addAbbreviation("boolean", "Z")
            addAbbreviation("float", "F")
            addAbbreviation("long", "J")
            addAbbreviation("short", "S")
            addAbbreviation("byte", "B")
            addAbbreviation("double", "D")
            addAbbreviation("char", "C")
        }
    }
}

*/
