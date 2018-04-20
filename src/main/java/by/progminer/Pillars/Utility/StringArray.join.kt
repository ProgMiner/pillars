package by.progminer.Pillars.Utility

fun Array<String>.join(glue: String = ""): String {
    val builder = StringBuilder()

    forEachIndexed { i, str ->
        if (i == 0) {
            builder.append(str)
        } else {
            builder.append(" $str")
        }
    }

    return builder.toString()
}